package xyz.emlyn.patchextension.qbf.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import android.util.Pair;

import xyz.emlyn.patchextension.shared.logging.Logging;
import xyz.emlyn.patchextension.shared.logging.Lvl;
import xyz.emlyn.patchextension.shared.Result;

class NetErr {
    public static final int NO_STATUS = -1;

    public final int status;
    public final String msg;

    NetErr(int status, String msg) {
        this.status = status;
        this.msg = (msg == null) ? msgForStatus(status) : msg;
    }

    private String msgForStatus(int status) {
        switch (status) {
            case 400:
            case 404:
                return "An error in the patch occured + no scan could be produced";
            case 401:
                return "No API key set";
            case 402:
                return "Insufficient funds";
            case 403:
                return "Forbidden (You may have selected an unavailable model)";
            case 413:
                return "Content too large";
            case 422:
                return "Invalid model selected";
            case 503:
                return "Selected model temporarily unavailable";
            case 500:
            default:
                return "An unknown error occured";
        }
    }

    @Override
    public String toString() {
        return this.msg;
    }
}

class Network {
    private static final int TIMEOUT = 10000; // millis

    public static Result<String, NetErr> makeReq(String url_s, String method, List<Pair<String, String>> headers,
            String body) {
        HttpURLConnection conn = null;
        try {
            @SuppressWarnings("deprecation")
            URL url = new URL(url_s);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            for (Pair<String, String> header : headers) {
                conn.setRequestProperty(header.first, header.second);
            }
            if (body != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                    os.write(payload, 0, payload.length);
                }
            }

            conn.setConnectTimeout(Network.TIMEOUT);
            conn.setReadTimeout(Network.TIMEOUT);

            int responseCode = conn.getResponseCode();
            // img api sends 202 :)
            if (responseCode >= 200 && responseCode < 300) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return Result.Ok(response.toString());
            } else {
                return Result.Err(new NetErr(responseCode, null));
            }
        } catch (Exception e) {
            Logging.log(Lvl.Error, "make_req error: " + e.getMessage());
            return Result.Err(new NetErr(NetErr.NO_STATUS, String.valueOf(e)));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

public class QbfNet {
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_BASE = 100;
    private static final int RETRY_MAX = 10000;

    public static Result<String, String> validateNormalizeURL(String urlRaw) {
        try {
            if (!(urlRaw.startsWith("http://") || urlRaw.startsWith("https://"))) {
                urlRaw = "https://" + urlRaw;
            }
            URI parsed = URI.create(urlRaw);
            URL url = parsed.toURL();
            String proto = url.getProtocol();
            String urlHost = url.getHost();

            if (!("http".equals(proto) || "https".equals(proto)) || urlHost == null || urlHost.isBlank()) {
                throw new IllegalArgumentException();
            }

            String normed = parsed.normalize().toString();
            normed = normed.replaceAll("[/]+$", "");
            return Result.Ok(normed);
        } catch (Exception e) {
            return Result.Err("Invalid url: \"" + urlRaw + "\"");
        }

    }

    public static Result<QbfJson.TextScan, String> checkText(String apiKey, String endpoint, String model,
            String text) {
        return QbfNet.scan(apiKey, endpoint + "/task", new QbfJson.TextRequest(text, model, false),
                QbfJson.TextScan::fromJson);
    }

    public static Result<QbfJson.ImageScan, String> checkImage(String apiKey, String endpoint, String model,
            String imageB64) {
        return QbfNet.scan(apiKey, endpoint + "/v1/tasks", new QbfJson.ImageRequest(imageB64, model),
                QbfJson.ImageScan::fromJson);
    }

    private static <T> Result<T, String> scan(String apiKey, String tasksUrl, QbfJson.ToJson request,
            QbfJson.FromJson<T> parser) {
        if (!tasksUrl.startsWith("http")) {
            tasksUrl = "https://" + tasksUrl;
        }

        ArrayList<Pair<String, String>> headers = new ArrayList<>();
        headers.add(new Pair<>("x-api-key", apiKey));
        headers.add(new Pair<>("Content-Type", "application/json"));

        Result<String, String> reqPayload = QbfJson.serialize(request);
        if (!reqPayload.ok)
            return Result.Err(reqPayload.err());

        Result<String, NetErr> taskRespRes = Network.makeReq(tasksUrl, "POST", headers, reqPayload.unwrap());
        if (!taskRespRes.ok)
            return Result.Err(taskRespRes.err().msg);
        Result<QbfJson.IdResponse, String> idResp = QbfJson.deserialize(taskRespRes.unwrap(),
                QbfJson.IdResponse::fromJson);
        if (!idResp.ok)
            return Result.Err("API returned unexpected response - " + idResp.err());
        QbfJson.IdResponse id = idResp.unwrap();

        int retries = 0;
        while ((retries++) < QbfNet.MAX_RETRIES) {
            Result<String, NetErr> resultRaw = Network.makeReq(tasksUrl + "/" + id.task_id, "GET", headers, null);
            if (resultRaw.ok) {
                String queryResult = resultRaw.unwrap();
                Result<QbfJson.StageResponse, String> stageRes = QbfJson.deserialize(queryResult,
                        QbfJson.StageResponse::fromJson);
                if (stageRes.ok) {
                    String stage = stageRes.unwrap().stage;
                    if ("STAGE_FAILED".equals(stage)) {
                        return Result.Err("Query failed: " + queryResult);
                    } else if ("STAGE_SUCCESS".equals(stage)) {
                        return QbfJson.deserialize(queryResult, parser);
                    }
                } else {
                    return Result.Err(stageRes.err());
                }
            } else {
                return Result.Err(resultRaw.err().msg);
            }

            try {
                Thread.sleep(Math.round(Math.random() * Math.min(RETRY_MAX, RETRY_BASE * Math.pow(2, retries))));
            } catch (InterruptedException _ignored) {
                Thread.currentThread().interrupt();
                String last = resultRaw.ok ? resultRaw.unwrap() : resultRaw.err().msg;
                return Result.Err("Task cancelled while waiting to retry. Last response: " + last);
            }
        }

        return Result.Err("No scan found after timeout");
    }
}
