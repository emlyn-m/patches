package xyz.emlyn.patchextension.qbf.ig;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

// stubs: implemented by patch (see instagram/qbf/Stubs.kt)
@SuppressWarnings("unused")
public class IgMedia {
    // MediaUfiUiState -> Media
    public static Object fromUiState(Object _uiState) {
        throw new UnsupportedOperationException("IgMedia.fromUiState not implemented");
    }

    public static String id(Object _media) {
        throw new UnsupportedOperationException("IgMedia.id not implemented");
    }

    // nullable
    public static String caption(Object _media) {
        throw new UnsupportedOperationException("IgMedia.caption not implemented");
    }

    // List<ExtendedImageUrl>, nullable
    public static List<?> imageCandidates(Object _media) {
        throw new UnsupportedOperationException("IgMedia.imageCandidates not implemented");
    }

    // List<Media>, null/empty for non-carousels
    public static List<?> carousel(Object _media) {
        throw new UnsupportedOperationException("IgMedia.carousel not implemented");
    }

    // raw media_type (1 = photo, 2 = video, 8 = carousel), 0 if unknown
    public static int mediaType(Object _media) {
        throw new UnsupportedOperationException("IgMedia.mediaType not implemented");
    }

    public static String imageUrl(Object _imageUrl) {
        throw new UnsupportedOperationException("IgMedia.imageUrl not implemented");
    }

    public static int imageWidth(Object _imageUrl) {
        throw new UnsupportedOperationException("IgMedia.imageWidth not implemented");
    }

    public static int imageHeight(Object _imageUrl) {
        throw new UnsupportedOperationException("IgMedia.imageHeight not implemented");
    }

    // ImageUrl -> disk cache entry, nullable. blocking, off main only
    public static Object diskEntry(Object _imageUrl) {
        throw new UnsupportedOperationException("IgMedia.diskEntry not implemented");
    }

    // encoded bytes, caller closes
    public static InputStream diskEntryStream(Object _entry) {
        throw new UnsupportedOperationException("IgMedia.diskEntryStream not implemented");
    }

    // nullable, "scan" != -1 means (partial) progressive data
    public static JSONObject diskEntryMetadata(Object _entry) {
        throw new UnsupportedOperationException("IgMedia.diskEntryMetadata not implemented");
    }

    // Vito drawable -> its actual image layer, null for other drawables
    public static Object vitoImageLayer(Drawable _drawable) {
        throw new UnsupportedOperationException("IgMedia.vitoImageLayer not implemented");
    }

    // -1 before the image loads
    public static int layerSourceWidth(Object _layer) {
        throw new UnsupportedOperationException("IgMedia.layerSourceWidth not implemented");
    }

    public static int layerSourceHeight(Object _layer) {
        throw new UnsupportedOperationException("IgMedia.layerSourceHeight not implemented");
    }

    // drawable coords, nullable
    public static Rect layerBounds(Object _layer) {
        throw new UnsupportedOperationException("IgMedia.layerBounds not implemented");
    }

    // source -> drawable coords, null = source fills bounds
    public static Matrix layerMatrix(Object _layer) {
        throw new UnsupportedOperationException("IgMedia.layerMatrix not implemented");
    }

    // IgImageView getters, nullable
    public static Bitmap viewBitmap(View _igImageView) {
        throw new UnsupportedOperationException("IgMedia.viewBitmap not implemented");
    }

    public static AtomicInteger viewScans(View _igImageView) {
        throw new UnsupportedOperationException("IgMedia.viewScans not implemented");
    }

    // MediaUfiUiState -> per-post state
    public static Object stateFromUiState(Object _uiState) {
        throw new UnsupportedOperationException("IgMedia.stateFromUiState not implemented");
    }

    // MediaUfiUiState -> current carousel page (0 for non-carousels)
    public static int carouselIndex(Object _uiState) {
        throw new UnsupportedOperationException("IgMedia.carouselIndex not implemented");
    }
}
