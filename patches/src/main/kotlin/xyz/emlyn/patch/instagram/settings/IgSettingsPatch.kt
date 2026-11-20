package xyz.emlyn.patch.instagram.settings

import app.morphe.patcher.Fingerprint
import xyz.emlyn.patch.shared.Constants.INSTAGRAM_COMPAT
import xyz.emlyn.patch.shared.Constants.SETTINGS_CLASS

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.getReference
import app.morphe.util.writeRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SETTINGS_ACTIVITY = "xyz.emlyn.patchextension.settings.SettingsActivity"
private const val SETTINGS_TITLE = "Patch settings"
private const val SETTINGS_TAG = "xyz_emlyn_morphe_settings"

private val igSettingsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val activity = doc.createElement("activity").apply {
                setAttribute("android:name", SETTINGS_ACTIVITY)
                setAttribute("android:exported", "false")
                setAttribute("android:theme", "@android:style/Theme.DeviceDefault")
            }
            doc.getElementsByTagName("application").item(0).appendChild(activity)
        }

    }
}

val igSettingsPatch = bytecodePatch {
    compatibleWith(INSTAGRAM_COMPAT)
    dependsOn(igSettingsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        val sentinel = StringLookupFingerprint.method.let { m ->
            m.findInstructionIndicesReversedOrThrow(Opcode.CONST_HIGH16)
                .map { m.getInstruction<NarrowLiteralInstruction>(it).narrowLiteral }
                .first { it > 0x7f00_0000 } or 0xffff
        }
        StringLookupFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                const v0, $sentinel
                if-ne p1, v0, :not_ours
                const-string v0, "$SETTINGS_TITLE"
                return-object v0
                :not_ours
                nop
                """
            )
        }

        WhitehatSettingsFingerprint.apply {
            val (_, titleId, routeId, entryInit) = instructionMatches

            val literalValue = LiteralValueFingerprint.method.returnType
            val entryInitIsn = entryInit.getInstruction<RegisterRangeInstruction>()
            val routeIdIsn = routeId.getInstruction<OneRegisterInstruction>()

            val gateRegProvider = method.getFreeRegisterProvider(entryInit.index, 2)
            val gt1 = gateRegProvider.getFreeRegister4Bit()
            val gt2 = gateRegProvider.getFreeRegister4Bit()

            method.addInstructions(
                entryInit.index,
                """
                new-instance v$gt1, $literalValue
                sget-object v$gt2, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-direct {v$gt1, v$gt2}, $literalValue-><init>(Ljava/lang/Object;)V
                move-object/16 v${entryInitIsn.startRegister + 2}, v$gt1
                """
            )
            method.replaceInstruction(
                routeId.index,
                """
                const-string v${routeIdIsn.registerA}, "$SETTINGS_TAG"
                """
            )

            val titleIdReg = titleId.getInstruction<NarrowLiteralInstruction>().writeRegister!!
            method.replaceInstruction(
                titleId.index,
                """
                const v$titleIdReg, $sentinel
                """
            )
            // todo: Would like to update our icon at some point
        }


        GotoRouteFingerprint.method.apply {
            val resolvedRouteClass = NativeRouteResolveFingerprint.method.instructions
                .filter { it.opcode == Opcode.NEW_INSTANCE }
                .map { it.getReference<TypeReference>()!!.type }
                .first { it != "Landroid/os/Bundle;" }
            val routeAnchor = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CHECK_CAST && getReference<TypeReference>()?.type == resolvedRouteClass
            }

            val contextIndex = indexOfFirstInstructionReversedOrThrow(routeAnchor) {
                getReference<MethodReference>()?.name == "requireContext"
            }
            val contextReg = getInstruction<OneRegisterInstruction>(contextIndex + 1).registerA
            val routeIdIdx = indexOfFirstInstructionOrThrow(routeAnchor) {
                opcode == Opcode.IGET_OBJECT && getReference<FieldReference>()?.type == "Ljava/lang/String;"
            }
            val routeIdReg = getInstruction(routeIdIdx).writeRegister!!
            val tmpRegs = getFreeRegisterProvider(routeIdIdx + 1, 2, routeIdReg, contextReg)
            val tmpReg0 = tmpRegs.getFreeRegister4Bit()
            val tmpReg1 = tmpRegs.getFreeRegister4Bit()
            addInstructionsWithLabels(
                routeIdIdx + 1,
                """
                const-string v$tmpReg0, "$SETTINGS_TAG"
                invoke-virtual {v$tmpReg0, v$routeIdReg}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v$tmpReg0
                if-eqz v$tmpReg0, :notours
                new-instance v$tmpReg0, Landroid/content/Intent;
                const-class v$tmpReg1, $SETTINGS_CLASS
                invoke-direct {v$tmpReg0, v$contextReg, v$tmpReg1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
                invoke-virtual {v$contextReg, v$tmpReg0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
                return-void
                :notours
                nop
                """
            )
        }


    }
}
