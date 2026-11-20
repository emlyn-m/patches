package xyz.emlyn.patch.twitter.noforyou

import xyz.emlyn.patch.shared.Constants.TWITTER_COMPAT

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c

@Suppress("unused")
val noForYouPatch = bytecodePatch(
    name = "Disable For You",
    description = "Disable the 'For You' feed.",
    default = true
) {
    compatibleWith(TWITTER_COMPAT)
    extendWith("extensions/extension.mpe")

    execute {
        val forYouClass = ForYouTabFingerprint.classDef
        PreferredTabFingerprint.method.addInstructions(
            0,
            """
            sget-object p0, $forYouClass->INSTANCE:$forYouClass
            return-object p0
            """
        )

        BuildTabFingerprint.method.findInstructionIndicesReversedOrThrow(opcode(Opcode.FILLED_NEW_ARRAY)).forEach { i ->
            val followingReg = BuildTabFingerprint.method.getInstruction<BuilderInstruction35c>(i).getRegisterD()
            val tabGroupClass = TabFingerprint.classDef
            BuildTabFingerprint.method.replaceInstruction(
                i,
                """
                filled-new-array {v$followingReg}, [$tabGroupClass
                """
            )

        }
    }
}
