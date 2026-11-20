package xyz.emlyn.patch.twitter.settings

import xyz.emlyn.patch.shared.Constants.TWITTER_COMPAT

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.cloneMutable
import app.morphe.util.getMutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val SETTINGS_CLASS = "Lxyz/emlyn/patchextension/settings/Settings;"
private const val SETTINGS_STUBS_CLASS = "Lxyz/emlyn/patchextension/settings/SettingsStubs;"
private const val SETTINGS_ACTIVITY = "xyz.emlyn.patchextension.settings.SettingsActivity"

private fun MethodReference.smali() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

private val settingsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val activity = doc.createElement("activity").apply {
                setAttribute("android:name", SETTINGS_ACTIVITY)
                setAttribute("android:exported", "false")
                setAttribute("android:windowSoftInputMode", "adjustResize")
                // actual theme set in onCreate
                setAttribute("android:theme", "@android:style/Theme.DeviceDefault")
            }
            doc.getElementsByTagName("application").item(0).appendChild(activity)
        }
    }
}

@Suppress("unused")
val settingsPatch = bytecodePatch {
    compatibleWith(TWITTER_COMPAT)
    extendWith("extensions/extension.mpe")
    dependsOn(settingsResourcePatch)

    execute {
        fun stub(definingClass: String, name: String, additionalRegisters: Int) =
            mutableClassDefBy(definingClass).let { classDef ->
                val original = classDef.methods.first { it.name == name }.getMutableMethod()
                original.cloneMutable(additionalRegisters = additionalRegisters).also {
                    classDef.methods.remove(original)
                    classDef.methods.add(it)
                }
            }

        val root = RootSettingsListFingerprint.method
        val listOfNotNullIndex = RootSettingsListFingerprint.instructionMatches[2].index
        val listOfIndex = RootSettingsListFingerprint.instructionMatches.last().index
        val instructions = root.implementation!!.instructions.toList()

        // filled-new-array [SettingsListItem; -> listOfNotNull(items) -> new SettingsListSection(..., items, mask)
        val itemClass = (listOfNotNullIndex downTo 0).firstNotNullOfOrNull { i ->
            val type = ((instructions[i] as? ReferenceInstruction)?.reference as? TypeReference)?.type
            type?.takeIf { it.startsWith("[L") }?.removePrefix("[")
        } ?: throw PatchException("failed to find SettingsListItem array in root settings list")

        val sectionInit = (listOfNotNullIndex..listOfIndex).firstNotNullOfOrNull { i ->
            val ref = (instructions[i] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.takeIf {
                it.name == "<init>" && it.parameterTypes.map { p -> p.toString() }
                    .takeLast(2) == listOf("Ljava/util/List;", "I")
            }
        } ?: throw PatchException("failed to find SettingsListSection constructor in root settings list")

        // SettingsListItem(title: gd, description: gd?, icon, value, opensTray: Z, onClick: Function0?, navigationTarget, confirmationDialogParams, mask)
        val itemInit = instructions.firstNotNullOfOrNull { insn ->
            ((insn as? ReferenceInstruction)?.reference as? MethodReference)
                ?.takeIf { it.definingClass == itemClass && it.name == "<init>" }
        } ?: throw PatchException("failed to find SettingsListItem constructor call in root settings list")
        val itemParams = itemInit.parameterTypes.map { it.toString() }
        val textClass = itemParams[0]
        val navTargetIndex = itemParams.indexOf("Lkotlin/jvm/functions/Function0;") + 1
        if (navTargetIndex == 0 || itemParams.last() != "I" || itemParams.any { it == "J" || it == "D" } || itemParams.size > 14)
            throw PatchException("unexpected SettingsListItem constructor: ${itemInit.smali()}")
        val navTargetInterface = itemParams[navTargetIndex]

        // TextSpec.Literal(text: String)
        val literalClass = Fingerprint(
            name = "toString",
            strings = listOf("Literal(text="),
            custom = { _, classDef -> classDef.superclass == textClass },
        ).classDef.type

        // NavigateToIntent(intent: Intent) : NavigationTarget
        val navigateToIntentClass = Fingerprint(
            name = "<init>",
            parameters = listOf("Landroid/content/Intent;"),
            custom = { _, classDef -> navTargetInterface in classDef.interfaces },
        ).classDef.type

        stub(SETTINGS_STUBS_CLASS, "literal", 1).addInstructions(
            0,
            """
            new-instance v0, $literalClass
            invoke-direct {v0, p0}, $literalClass-><init>(Ljava/lang/String;)V
            return-object v0
            """
        )

        stub(SETTINGS_STUBS_CLASS, "navigateToIntent", 1).addInstructions(
            0,
            """
            new-instance v0, $navigateToIntentClass
            invoke-direct {v0, p0}, $navigateToIntentClass-><init>(Landroid/content/Intent;)V
            return-object v0
            """
        )

        // item(title, description, navigationTarget), descriptions are optional
        val itemArgs = itemParams.indices.joinToString("\n") { i ->
            val reg = "v${i + 1}"
            when (i) {
                0 -> "move-object/from16 $reg, p0\ncheck-cast $reg, $textClass"
                1 -> "move-object/from16 $reg, p1\ncheck-cast $reg, $textClass"
                navTargetIndex -> "move-object/from16 $reg, p2\ncheck-cast $reg, $navTargetInterface"
                else -> "const/4 $reg, 0x0"
            }
        }
        stub(SETTINGS_STUBS_CLASS, "item", itemParams.size + 1).addInstructions(
            0,
            """
            new-instance v0, $itemClass
            $itemArgs
            invoke-direct/range {v0 .. v${itemParams.size}}, ${itemInit.smali()}
            return-object v0
            """
        )

        // section(items) (untitled)
        stub(SETTINGS_STUBS_CLASS, "section", 5).addInstructions(
            0,
            """
            new-instance v0, ${sectionInit.definingClass}
            const/4 v1, 0x0
            const/4 v2, 0x0
            move-object v3, p0
            const/4 v4, 0x3
            invoke-direct/range {v0 .. v4}, ${sectionInit.smali()}
            return-object v0
            """
        )

        val moveResult = root.getInstruction(listOfIndex + 1)
        if (moveResult.opcode != Opcode.MOVE_RESULT_OBJECT)
            throw PatchException("expected move-result-object after root settings listOf")
        val r = (moveResult as OneRegisterInstruction).registerA

        root.addInstructions(
            listOfIndex + 2,
            """
            invoke-static/range {v$r .. v$r}, $SETTINGS_CLASS->injectRootSections(Ljava/util/List;)Ljava/util/List;
            move-result-object v$r
            """
        )
    }
}
