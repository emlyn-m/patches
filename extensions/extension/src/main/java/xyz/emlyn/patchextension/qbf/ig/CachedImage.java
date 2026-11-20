package xyz.emlyn.patchextension.qbf.ig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.os.SystemClock;

// Encoded image as IG downloaded it, read from its disk cache. Off main thread only.
class CachedImage {
    private static final long POLL_MS = 250;
    private static final long TIMEOUT_MS = 15000;

    final byte[] bytes;
    final int width;
    final int height;
    final String mimeType;

    private CachedImage(byte[] bytes, int width, int height, String mimeType) {
        this.bytes = bytes;
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
    }

    // waits for the complete image, null on timeout
    static CachedImage awaitFull(Object imageUrl) throws IOException, InterruptedException {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (true) {
            Object entry = IgMedia.diskEntry(imageUrl);
            if (entry != null) {
                JSONObject meta = IgMedia.diskEntryMetadata(entry);
                boolean full = meta == null || meta.optInt("scan", -1) == -1;
                try (InputStream in = IgMedia.diskEntryStream(entry)) {
                    if (full && in != null)
                        return decode(readAll(in));
                }
            }
            if (SystemClock.uptimeMillis() > deadline)
                return null;
            Thread.sleep(POLL_MS);
        }
    }

    private static CachedImage decode(byte[] bytes) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        return new CachedImage(bytes, opts.outWidth, opts.outHeight, opts.outMimeType);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        for (int n; (n = in.read(buf)) > 0;)
            out.write(buf, 0, n);
        return out.toByteArray();
    }
}
