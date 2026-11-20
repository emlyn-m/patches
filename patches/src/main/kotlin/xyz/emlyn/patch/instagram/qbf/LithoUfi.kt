package xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lxyz/emlyn/patchextension/qbf/ig/IgQbfButton;"
private const val QBF_BUTTON_METHOD = "qbfButton"
private const val SCALE_TYPE = $$"Landroid/widget/ImageView$ScaleType;"
private const val DRAWABLE = "Landroid/graphics/drawable/Drawable;"

// Optional visual search button
object LithoUfiExtraButtonFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L", "L", "L", "L", "Z"),
    filters = listOf(
        string("android.widget.Button"),
        string("getVisualSearchButtonComponent"),
    )
)

private val Any.methodRef get() = (this as ReferenceInstruction).reference as MethodReference

// target bar render
internal fun BytecodePatchContext.lithoUfiRender(): MutableMethod {
    val original = LithoUfiExtraButtonFingerprint.method
    return LithoUfiExtraButtonFingerprint.classDef.methods.first { method ->
        method.implementation?.instructions?.any {
            it.opcode == Opcode.INVOKE_DIRECT_RANGE && it.methodRef.let { ref ->
                ref.definingClass == original.definingClass && ref.name == original.name
            }
        } == true
    }
}

private fun BytecodePatchContext.commonSuperclass(type: String, other: String): String {
    fun chain(t: String) = generateSequence(t) { classDefByOrNull(it)?.superclass }.toList()
    val others = chain(other).toSet()
    return chain(type).first { it in others }
}

// build+add our clone of visual search button
internal fun BytecodePatchContext.installLithoUfiButton() {
    val original = LithoUfiExtraButtonFingerprint.method
    val classDef = LithoUfiExtraButtonFingerprint.classDef
    val uiStateParam = 3 // (scope, theme, ?, MediaUfiUiState, Z)

    // bypass ordinary tinting to ensure full draw control + animation
    val buttonClass = original.implementation!!.instructions
        .last { it.opcode == Opcode.INVOKE_DIRECT_RANGE && it.methodRef.name == "<init>" }.methodRef.definingClass
    val imageCtor = classDefBy(buttonClass).methods.firstNotNullOfOrNull { method ->
        method.implementation?.instructions?.firstOrNull {
            it.opcode == Opcode.INVOKE_DIRECT && it.methodRef.name == "<init>" &&
                    it.methodRef.parameterTypes.map { type -> type.toString() }.take(2) == listOf(DRAWABLE, SCALE_TYPE)
        }?.methodRef
    } ?: throw PatchException("litho ufi: failed to find image primitive in $buttonClass")

    val button = original.cloneMutable(
        name = QBF_BUTTON_METHOD,
        returnType = commonSuperclass(buttonClass, imageCtor.definingClass),
    )
    val insns = button.implementation!!.instructions.toList()
    val p0 = button.implementation!!.registerCount - button.parameters.size - 1
    val uiStateReg = p0 + 1 + uiStateParam

    val showVisualSearchIndex = insns.indexOfFirst { it.opcode == Opcode.IGET_BOOLEAN }

    val onClickBuilderIndex = insns.indexOfFirst {
        it.opcode == Opcode.INVOKE_STATIC && it.methodRef.parameterTypes.size == 2 &&
                it.methodRef.parameterTypes[1].toString() == "Lkotlin/jvm/functions/Function1;"
    }
    val onClickLambdaReg = (insns[onClickBuilderIndex] as Instruction35c).registerD

    // visual search tracing wrapper - drop it with its lambda
    val traceIndex = insns.indexOfFirst {
        it.opcode == Opcode.INVOKE_STATIC && it.methodRef.parameterTypes.size == 4 &&
                it.methodRef.parameterTypes[2].toString() == "Ljava/lang/String;"
    }

    // button drawable ctor (ScaleType, Modifier, Integer pressed tint, I drawable, I colour, Z)
    val vsearchCtorIdx = insns.indexOfLast { it.opcode == Opcode.INVOKE_DIRECT_RANGE && it.methodRef.name == "<init>" }
    val vsearchCtor = insns[vsearchCtorIdx] as RegisterRangeInstruction
    val vsearchCtorReg = vsearchCtor.startRegister
    val newVsearchIdx = (vsearchCtorIdx downTo 0).first {
        insns[it].opcode == Opcode.NEW_INSTANCE && (insns[it] as OneRegisterInstruction).registerA == vsearchCtorReg
    }
    val vsearchDrawableReg = vsearchCtorReg + 4
    val vsearchDrawableIdx = (vsearchCtorIdx downTo 0).first {
        val insn = insns[it]
        (insn.opcode == Opcode.CONST || insn.opcode == Opcode.CONST_HIGH16) &&
                (insn as OneRegisterInstruction).registerA == vsearchDrawableReg
    }
    val vsearchScaleIdx = insns.indexOfLast {
        it.opcode == Opcode.SGET_OBJECT && ((it as ReferenceInstruction).reference as FieldReference).type == SCALE_TYPE
    }
    val scaleReg = (insns[vsearchScaleIdx] as OneRegisterInstruction).registerA

    if (listOf(showVisualSearchIndex, onClickBuilderIndex, traceIndex).any { it < 0 })
        throw PatchException("litho ufi: unexpected visual search builder shape ($showVisualSearchIndex, $onClickBuilderIndex, $traceIndex)")
    if (vsearchCtor.registerCount != 7 || vsearchCtorReg + 4 > 0xff)
        throw PatchException("litho ufi: unexpected 03mO ctor range (v$vsearchCtorReg, ${vsearchCtor.registerCount})")

    button.replaceInstruction(
        vsearchCtorIdx,
        "invoke-static/range {v$vsearchCtorReg .. v${vsearchCtorReg + 6}}, $EXTENSION_CLASS->drawable(Ljava/lang/Object;${SCALE_TYPE}Ljava/lang/Object;Ljava/lang/Integer;IIZ)$DRAWABLE"
    )
    button.addInstructions(
        vsearchCtorIdx + 1,
        """
        move-result-object v$vsearchCtorReg
        move-object/16 v${vsearchCtorReg + 3}, v${vsearchCtorReg + 2}
        move-object/16 v${vsearchCtorReg + 2}, v${vsearchCtorReg + 1}
        move-object/16 v${vsearchCtorReg + 1}, v$vsearchCtorReg
        const/16 v${vsearchCtorReg + 4}, 0x1
        new-instance v$vsearchCtorReg, ${imageCtor.definingClass}
        invoke-direct/range {v$vsearchCtorReg .. v${vsearchCtorReg + 4}}, ${imageCtor.smali()}
        """
    )
    button.replaceInstruction(newVsearchIdx, "move-object/16 v$vsearchCtorReg, v$uiStateReg")

    button.replaceInstruction(vsearchScaleIdx, "sget-object v$scaleReg, $SCALE_TYPE->FIT_CENTER:$SCALE_TYPE")
    button.replaceInstruction(vsearchDrawableIdx, "invoke-static {}, $EXTENSION_CLASS->iconRes()I")
    button.addInstruction(vsearchDrawableIdx + 1, "move-result v$vsearchDrawableReg")

    button.removeInstructions(traceIndex - 4, 6)

    button.replaceInstruction(
        onClickBuilderIndex - 2,
        "invoke-static {v$uiStateReg}, $EXTENSION_CLASS->onClick(Ljava/lang/Object;)Lkotlin/jvm/functions/Function1;"
    )
    button.replaceInstruction(onClickBuilderIndex - 1, "move-result-object v$onClickLambdaReg")

    val gateReg = (insns[showVisualSearchIndex] as TwoRegisterInstruction).registerA
    button.replaceInstruction(showVisualSearchIndex, "const/4 v$gateReg, 0x1")

    classDef.methods.add(button)

    // call site in render: moves into the range, invoke, move-result, if-eqz, list.add, :label
    val render = lithoUfiRender()
    val renderInsns = render.implementation!!.instructions.toList()
    val renderCallIdx = renderInsns.indexOfFirst {
        it.opcode == Opcode.INVOKE_DIRECT_RANGE && it.methodRef.name == original.name && it.methodRef.definingClass == classDef.type
    }
    val renderCall = renderInsns[renderCallIdx] as RegisterRangeInstruction
    val addIndex = renderCallIdx + 3
    if (renderInsns[addIndex].opcode != Opcode.INVOKE_VIRTUAL || renderInsns[addIndex].methodRef.name != "add")
        throw PatchException("litho ufi: unexpected call site shape at $addIndex")
    val listReg = (renderInsns[addIndex] as Instruction35c).registerC
    val resultReg = (renderInsns[renderCallIdx + 1] as OneRegisterInstruction).registerA

    // set up range args on our cloned branch
    val moves = renderInsns.subList(renderCallIdx - renderCall.registerCount, renderCallIdx).joinToString("\n") {
        val move = it as TwoRegisterInstruction
        val op = when (it.opcode) {
            Opcode.MOVE, Opcode.MOVE_FROM16, Opcode.MOVE_16 -> "move/from16"
            Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16 -> "move-object/from16"
            else -> throw PatchException("litho ufi: unexpected arg setup ${it.opcode}")
        }
        "$op v${move.registerA}, v${move.registerB}"
    }
    val signature = original.parameterTypes.joinToString("")

    render.addInstructionsAtControlFlowLabel(
        addIndex + 1,
        """
        $moves
        invoke-direct/range {v${renderCall.startRegister} .. v${renderCall.startRegister + renderCall.registerCount - 1}}, ${classDef.type}->$QBF_BUTTON_METHOD($signature)${button.returnType}
        move-result-object v$resultReg
        if-eqz v$resultReg, :qbf_skip
        invoke-virtual {v$listReg, v$resultReg}, Ljava/util/AbstractCollection;->add(Ljava/lang/Object;)Z
        :qbf_skip
        nop
        """
    )
}
