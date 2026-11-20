package xyz.emlyn.patch.twitter.qbf

import xyz.emlyn.patch.twitter.settings.settingsPatch
import xyz.emlyn.patch.shared.Constants.TWITTER_COMPAT

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.option
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getFreeRegisterProvider

private const val EXTENSION_CLASS = "Lxyz/emlyn/patchextension/qbf/twitter/QbfTriggerButton;"
private const val TEXT_CLASS = "Lxyz/emlyn/patchextension/qbf/twitter/text/QbfText;"

@Suppress("unused")
val qbfPatch = bytecodePatch(
    name = "qbf",
    description = "todo.",
    default = true
) {
    compatibleWith(TWITTER_COMPAT)
    extendWith("extensions/extension.mpe")

    // attach preferences
    apiKey()
    autoscan()
    autoOverlay()
    textApi()
    imgApi()

    dependsOn(settingsPatch)
    dependsOn(qbfStubsPatch)
    dependsOn(qbfSettingsPatch)
    dependsOn(qbfResourcesPatch)

    execute {
        val render = "$EXTENSION_CLASS->render(Ljava/lang/Object;Ljava/lang/Object;)V"
        val renderFocal = "$EXTENSION_CLASS->renderFocal(Ljava/lang/Object;Ljava/lang/Object;)V"

        GrokButtonFingerprint.method.apply {
            val iconCall = GrokButtonFingerprint.instructionMatches.last()
            val r = iconCall.getInstruction<RegisterRangeInstruction>().startRegister
            val composerReg = r + 6

            removeInstruction(iconCall.index)
            addInstructions(
                iconCall.index,
                """
                move-object/from16 v$r, p0
                move-object/from16 v${r + 1}, v$composerReg
                invoke-static/range {v$r .. v${r + 1}}, $render
                """
            )
        }

        FocalHeaderFingerprint.method.apply {
            val call = FocalHeaderFingerprint.instructionMatches.last()
            val callInsn = call.getInstruction<FiveRegisterInstruction>()
            val postClass = AvailablePostToStringFingerprint.method.definingClass
            val postReg = (call.index - 1 downTo 0).firstNotNullOfOrNull { i ->
                val insn = getInstruction(i)
                if (insn.opcode == Opcode.IGET_OBJECT &&
                    (insn as TwoRegisterInstruction).registerA == callInsn.registerC &&
                    ((insn as ReferenceInstruction).reference as FieldReference).definingClass == postClass
                )
                    insn.registerB
                else null
            } ?: throw PatchException("failed to find post for focal header")

            val composerReg = callInsn.registerE
            val (p, c) = if (postReg < 16 && composerReg < 16) postReg to composerReg
            else getFreeRegisterProvider(call.index, 2, postReg, composerReg).let {
                it.getFreeRegister4Bit() to it.getFreeRegister4Bit()
            }
            addInstructions(call.index, "invoke-static {v$p, v$c}, $renderFocal")
            if (p != postReg)
                addInstructions(
                    call.index,
                    """
                    move-object/from16 v$p, v$postReg
                    move-object/from16 v$c, v$composerReg
                    """
                )
            addInstructions(
                0,
                "invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->register(Ljava/lang/Object;)V"
            )
        }

        ShouldShowGrokButtonFingerprint.match(GrokButtonFingerprint.originalClassDef).let {
            it.method.removeInstruction(it.instructionMatches.first().index)
        }

        GrokButtonFingerprint.method.addInstructions(
            0,
            "invoke-static/range {p0 .. p0}, $EXTENSION_CLASS->register(Ljava/lang/Object;)V"
        )

        // rich text hooks, see QbfText
        val build = RichTextBuildFingerprint.method
        val buildSmali = build.smali()
        val annotatedStringClass = build.returnType

        val composable = mutableClassDefBy(build.definingClass).methods.singleOrNull { m ->
            m.references().any { it is MethodReference && it.smali() == buildSmali }
        } ?: throw PatchException("failed to find rich text composable calling $buildSmali")

        composable.apply {
            val callIndex = implementation!!.instructions.indexOfFirst {
                ((it as? ReferenceInstruction)?.reference as? MethodReference)?.smali() == buildSmali
            }
            val moveResult = getInstruction(callIndex + 1)
            if (moveResult.opcode != Opcode.MOVE_RESULT_OBJECT)
                throw PatchException("expected move-result-object after rich text build call")
            val r = (moveResult as OneRegisterInstruction).registerA

            addInstructions(
                callIndex + 2,
                """
                invoke-static/range {v$r .. v$r}, $TEXT_CLASS->decorate(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$r
                check-cast v$r, $annotatedStringClass
                """
            )
        }

        composable.apply {
            val plainRenderers = implementation!!.instructions.withIndex().filter { (_, insn) ->
                if (insn.opcode != Opcode.INVOKE_STATIC_RANGE) return@filter false
                val ref = (insn as ReferenceInstruction).reference as MethodReference
                ref.returnType == "V" && ref.definingClass != build.definingClass &&
                        ref.parameterTypes.firstOrNull()?.toString() == annotatedStringClass
            }
            if (plainRenderers.size != 2)
                throw PatchException("expected 2 plain text renderers in rich text composable, found ${plainRenderers.size}")

            for ((index, insn) in plainRenderers.reversed()) {
                val r = (insn as RegisterRangeInstruction).startRegister
                addInstructions(
                    index,
                    """
                    invoke-static/range {v$r .. v$r}, $TEXT_CLASS->decoratePlain(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$r
                    check-cast v$r, $annotatedStringClass
                    """
                )
            }

            addInstructions(
                0,
                "invoke-static/range {p0 .. p0}, $TEXT_CLASS->onRichTextEntry(Ljava/lang/String;)V"
            )
        }

        build.addInstructions(
            0,
            "invoke-static/range {p1 .. p1}, $TEXT_CLASS->onBuildEntry(Ljava/lang/String;)V"
        )

        RichTextStyleFingerprint.method.addInstructions(
            0,
            "invoke-static/range {p0 .. p2}, $TEXT_CLASS->onStyle(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V"
        )
    }

}
