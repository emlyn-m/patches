package xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal const val MEDIA_CLASS = "Lcom/instagram/feed/media/Media;"
internal const val MEDIA_EXT_CLASS = "Lcom/instagram/feed/media/MediaExtKt;"
internal const val IMAGE_INFO_CLASS = "Lcom/instagram/model/mediasize/ImageInfo;"
internal const val EXTENDED_IMAGE_URL_CLASS = "Lcom/instagram/model/mediasize/ExtendedImageUrl;"
internal const val IMAGE_URL_CLASS = "Lcom/instagram/common/typedurl/ImageUrl;"
internal const val IG_IMAGE_VIEW_CLASS = "Lcom/instagram/common/ui/widget/imageview/IgImageView;"

object UfiRendererSelectorFingerprint : Fingerprint(
    returnType = "Ljava/lang/Integer;",
    parameters = listOf("L", "Ljava/lang/String;"),
    filters = listOf(
        string("feed_timeline"),
        string("compose"),
        string("litho"),
        string("view"),
    )
)

object MediaUfiUiStateToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("MediaUfiUiState(mediaForActionHandler="),
)

object MediaCaptionFingerprint : Fingerprint(
    definingClass = MEDIA_CLASS,
    returnType = "L",
    parameters = listOf(),
    filters = listOf(string("caption")),
)

object MediaImageInfoFingerprint : Fingerprint(
    definingClass = MEDIA_CLASS,
    returnType = IMAGE_INFO_CLASS,
    parameters = listOf(),
    filters = listOf(string("image_versions2")),
)

object MediaCarouselFingerprint : Fingerprint(
    definingClass = MEDIA_CLASS,
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(string("carousel_media")),
)

// raw media_type: 1 = photo, 2 = video, 8 = carousel
object MediaTypeFingerprint : Fingerprint(
    definingClass = MEDIA_CLASS,
    returnType = "Ljava/lang/Integer;",
    parameters = listOf(),
    filters = listOf(string("media_type")),
)

object MediaImageInfoSetterFingerprint : Fingerprint(
    definingClass = MEDIA_CLASS,
    returnType = "V",
    parameters = listOf(IMAGE_INFO_CLASS),
    filters = listOf(
        string("image_versions2"),
        methodCall(definingClass = IMAGE_INFO_CLASS, returnType = "Ljava/util/List;"),
        checkCast(EXTENDED_IMAGE_URL_CLASS, location = InstructionLocation.MatchAfterWithin(5)),
    )
)

object ImageInfraFingerprint : Fingerprint(
    strings = listOf("IgImageInfra.prefetch"),
)

object DiskCacheLookupFingerprint : Fingerprint(
    strings = listOf("DiskCacheImageLoader.diskCacheLookup", "scan"),
)

object VitoLayerUpdateFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/graphics/ColorFilter;", "Landroid/graphics/Rect;", "L", "L", "L", "L"),
    filters = listOf(
        methodCall(definingClass = "Landroid/graphics/Matrix;", name = "setRectToRect"),
        methodCall(definingClass = "Landroid/graphics/BitmapShader;", name = "<init>"),
    )
)

object TypedUrlGetterFingerprint : Fingerprint(
    definingClass = IG_IMAGE_VIEW_CLASS,
    name = "getTypedUrl",
    returnType = IMAGE_URL_CLASS,
    parameters = listOf(),
)
