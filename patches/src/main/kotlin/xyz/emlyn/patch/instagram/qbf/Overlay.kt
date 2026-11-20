package xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val IG_OVERLAY_CLASS = "Lxyz/emlyn/patchextension/qbf/ig/IgOverlay;"
private const val IG_CAROUSEL_CLASS = "Lxyz/emlyn/patchextension/qbf/ig/IgCarousel;"

// carousel swipe: notify after the page setter's changed branch (the label before the
// return is the unchanged branch's jump target, so code inserted there runs on change only)
internal fun BytecodePatchContext.installCarouselListener() {
    val setter = carouselIndexFingerprint.method
    val returnIndex = setter.implementation!!.instructions.indexOfFirst { it.opcode == Opcode.RETURN_VOID }
    if (returnIndex < 0 || setter.implementation!!.instructions.elementAt(returnIndex).location.labels.isEmpty())
        throw PatchException("overlay: unexpected carousel setter shape in ${setter.definingClass}")

    setter.addInstructions(
        returnIndex,
        "invoke-static {p0, p1}, $IG_CAROUSEL_CLASS->onPageChanged(Ljava/lang/Object;I)V"
    )
}

private val MutableMethod.isStatic get() = AccessFlags.STATIC.isSet(accessFlags)
private val MutableMethod.params get() = parameterTypes.map { it.toString() }
private fun slots(type: String) = if (type == "J" || type == "D") 2 else 1

// p-register for parameter index (accounts for `this` + wide params)
private fun MutableMethod.paramReg(index: Int) =
    "p${(if (isStatic) 0 else 1) + params.take(index).sumOf(::slots)}"

// IgOverlay.onImageUrl(this, url) at method start, clobbers v0/v1 (unused before first insn)
private fun MutableMethod.notifyImageUrlAtStart() {
    val urlParam = params.indexOf(IMAGE_URL_CLASS)
    val locals = implementation!!.registerCount - params.sumOf(::slots) - 1
    if (isStatic || urlParam < 0 || locals < 2)
        throw PatchException("overlay: unexpected url setter shape $name$params ($locals locals)")

    addInstructions(
        0,
        """
        move-object/from16 v0, p0
        move-object/from16 v1, ${paramReg(urlParam)}
        invoke-static {v0, v1}, $IG_OVERLAY_CLASS->onImageUrl(Landroid/view/View;Ljava/lang/Object;)V
        """
    )
}

internal fun BytecodePatchContext.installMediaOverlay() {
    val typedUrl = TypedUrlGetterFingerprint.method.implementation!!.instructions
        .first { it.opcode == Opcode.IGET_OBJECT }
        .let { (it as ReferenceInstruction).reference as FieldReference }
    val methods = mutableClassDefBy(IG_IMAGE_VIEW_CLASS).methods
    val setter = methods.singleOrNull { method ->
        method.implementation?.instructions?.any {
            it.opcode == Opcode.IPUT_OBJECT && (it as ReferenceInstruction).reference == typedUrl
        } == true
    } ?: throw PatchException("overlay: no single writer of $typedUrl")
    val callers = methods.filter { method ->
        method.implementation?.instructions?.any {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.smali() == setter.smali()
        } == true
    }

    (callers + setter).forEach { it.notifyImageUrlAtStart() }
}
