package xyz.emlyn.patchextension.qbf.api;

import xyz.emlyn.patchextension.shared.Result;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QbfJson {
    public interface ToJson {
        JSONObject toJson() throws JSONException;
    }

    public interface FromJson<T> {
        T fromJson(JSONObject o) throws JSONException;
    }

    public static Result<String, String> serialize(ToJson model) {
        try {
            return Result.Ok(model.toJson().toString());
        } catch (JSONException e) {
            return Result.Err("Failed to serialize request: " + e.getMessage());
        }
    }

    public static <T> Result<T, String> deserialize(String raw, FromJson<T> parser) {
        if (raw == null) {
            return Result.Err("Empty response");
        }
        try {
            return Result.Ok(parser.fromJson(new JSONObject(raw)));
        } catch (JSONException e) {
            return Result.Err("Failed to parse response (" + e.getMessage() + "): " + raw);
        }
    }

    private static String str(JSONObject o, String key) {
        return o.isNull(key) ? null : o.optString(key);
    }

    private static float flt(JSONObject o, String key) {
        return (float) o.optDouble(key, 0);
    }

    public static class TextRequest implements ToJson {
        public String text;
        public String model;
        public boolean public_dashboard_link;

        public TextRequest(String text, String model, boolean public_dashboard_link) {
            this.text = text;
            this.model = model;
            this.public_dashboard_link = public_dashboard_link;
        }

        @Override
        public JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("text", text)
                    .put("model", model)
                    .put("public_dashboard_link", public_dashboard_link);
        }
    }

    public static class ImageRequest implements ToJson {
        public String image_b64;
        public String model;

        public ImageRequest(String image_b64, String model) {
            this.image_b64 = image_b64;
            this.model = model;
        }

        @Override
        public JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("image_b64", image_b64)
                    .put("model", model);
        }
    }

    public static class IdResponse {
        public String task_id;

        public static IdResponse fromJson(JSONObject o) {
            IdResponse r = new IdResponse();
            r.task_id = str(o, "task_id");
            return r;
        }
    }

    public static class StageResponse {
        public String stage;

        public static StageResponse fromJson(JSONObject o) {
            StageResponse r = new StageResponse();
            r.stage = str(o, "stage");
            return r;
        }
    }

    public static class TextScan {
        public String task_id;
        public String stage;
        public String text;
        public String version;
        public String headline;
        public String prediction_short;
        public float fraction_ai;
        public float fraction_ai_assisted;
        public float fraction_human;
        public int num_ai_segments;
        public int num_ai_assisted_segments;
        public int num_human_segments;
        public String dashboard_link;
        public List<Window> windows;

        public static class Window {
            public String text;
            public String label;
            public float ai_assistance_score;
            public String confidence;
            public int start_index;
            public int end_index;
            public int word_count;
            public int token_length;
            public boolean is_humanized;
            public float humanizer_score;

            public static Window fromJson(JSONObject o) {
                Window w = new Window();
                w.text = str(o, "text");
                w.label = str(o, "label");
                w.ai_assistance_score = flt(o, "ai_assistance_score");
                w.confidence = str(o, "confidence");
                w.start_index = o.optInt("start_index");
                w.end_index = o.optInt("end_index");
                w.word_count = o.optInt("word_count");
                w.token_length = o.optInt("token_length");
                w.is_humanized = o.optBoolean("is_humanized");
                w.humanizer_score = flt(o, "humanizer_score");
                return w;
            }
        }

        public static TextScan fromJson(JSONObject o) throws JSONException {
            TextScan r = new TextScan();
            r.task_id = str(o, "task_id");
            r.stage = str(o, "stage");
            r.text = str(o, "text");
            r.version = str(o, "version");
            r.headline = str(o, "headline");
            r.prediction_short = str(o, "prediction_short");
            r.fraction_ai = flt(o, "fraction_ai");
            r.fraction_ai_assisted = flt(o, "fraction_ai_assisted");
            r.fraction_human = flt(o, "fraction_human");
            r.num_ai_segments = o.optInt("num_ai_segments");
            r.num_ai_assisted_segments = o.optInt("num_ai_assisted_segments");
            r.num_human_segments = o.optInt("num_human_segments");
            r.dashboard_link = str(o, "dashboard_link");

            JSONArray windows = o.optJSONArray("windows");
            if (windows != null) {
                r.windows = new ArrayList<>(windows.length());
                for (int i = 0; i < windows.length(); i++) {
                    r.windows.add(Window.fromJson(windows.getJSONObject(i)));
                }
            }
            return r;
        }
    }

    public static class ImageScan {
        public String task_id;
        public String stage;
        // `AI Detected`, `Likely AI`, `Unsure`, `Likely No AI`, `No AI`, `Partially AI`
        public String prediction;
        public float ai_likelihood;
        // `Low`, `Medium`, `High`
        public String confidence;
        // `model`, `c2pa`
        public String detection_source;
        public String version;
        public Heatmap heatmap;
        // disregard metadata for now.
        // 256px max data URL
        public String thumbnail;

        public static class Heatmap {
            public List<List<Float>> grid;
            public int patch_stride;

            public static Heatmap fromJson(JSONObject o) throws JSONException {
                Heatmap h = new Heatmap();
                h.patch_stride = o.optInt("patch_stride");

                JSONArray rows = o.optJSONArray("grid");
                if (rows != null) {
                    h.grid = new ArrayList<>(rows.length());
                    for (int i = 0; i < rows.length(); i++) {
                        JSONArray cols = rows.getJSONArray(i);
                        List<Float> row = new ArrayList<>(cols.length());
                        for (int j = 0; j < cols.length(); j++) {
                            row.add((float) cols.getDouble(j));
                        }
                        h.grid.add(row);
                    }
                }
                return h;
            }
        }

        public static ImageScan fromJson(JSONObject o) throws JSONException {
            ImageScan r = new ImageScan();
            r.task_id = str(o, "task_id");
            r.stage = str(o, "stage");
            r.prediction = str(o, "prediction");
            r.ai_likelihood = flt(o, "ai_likelihood");
            r.confidence = str(o, "confidence");
            r.detection_source = str(o, "detection_source");
            r.version = str(o, "version");
            r.thumbnail = str(o, "thumbnail");

            JSONObject heatmap = o.optJSONObject("heatmap");
            if (heatmap != null) {
                r.heatmap = Heatmap.fromJson(heatmap);
            }
            return r;
        }
    }
}
