package xyz.emlyn.patch.twitter.qbf

import xyz.emlyn.patch.shared.Constants.SETTINGS_CLASS
import xyz.emlyn.patch.shared.Constants.LOG_CLASS

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.option
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getFreeRegisterProvider

private const val QBF_SETTINGS = "Lxyz/emlyn/patchextension/qbf/twitter/Settings;"
private const val QBF_SETTINGS_PREFS = $$"Lxyz/emlyn/patchextension/qbf/twitter/Settings$Prefs;"
private const val SETTINGS_ENTRY = "Lxyz/emlyn/patchextension/settings/SettingsEntry;"

internal val apiKey =
    stringOption(key = "apiKey", title = "API Key", description = "Sent as `x-api-key`", required = true)
internal val autoscan = option<Boolean>(key = "autoscan", title = "Automatically scan posts", default = false)
internal val autoOverlay =
    option<Boolean>(key = "autooverlay", title = "Automatically show overlay once scanned", default = false)
internal val textApi =
    stringOption(key = "apiText", title = "Text API override", description = $$"POST $API, GET $API/{id}")
internal val imgApi =
    stringOption(key = "apiImg", title = "Image API override", description = $$"POST $API, GET $API/{id}")

val qbfSettingsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        Fingerprint(
            definingClass = SETTINGS_CLASS,
            name = "drawGroups"
        ).method.addInstructions(
            0,
            """
            invoke-static {p0, p1}, $QBF_SETTINGS->draw(${SETTINGS_CLASS}Landroid/widget/LinearLayout;)V
            """
        )

        Fingerprint(
            definingClass = QBF_SETTINGS_PREFS,
            name = "<clinit>"
        ).method.apply {
            val injectIdx = instructions.size - 1
            val prov = getFreeRegisterProvider(injectIdx, 2)
            val tmpA = prov.getFreeRegister4Bit()
            val tmpB = prov.getFreeRegister4Bit()

            addInstructions(
                injectIdx,
                """
                sget-object v$tmpA, $QBF_SETTINGS_PREFS->ApiKey:$SETTINGS_ENTRY
                const-string v$tmpB, "${apiKey.value}"
                invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V

                sget-object v$tmpA, $QBF_SETTINGS_PREFS->Autoscan:$SETTINGS_ENTRY
                sget-object v$tmpB, Ljava/lang/Boolean;->${if (autoscan.value!!) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
                invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V

                sget-object v$tmpA, $QBF_SETTINGS_PREFS->AutoOverlay:$SETTINGS_ENTRY
                sget-object v$tmpB, Ljava/lang/Boolean;->${if (autoOverlay.value!!) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
                invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
                
                ${
                    if (textApi.value != null) """
                    sget-object v$tmpA, $QBF_SETTINGS_PREFS->ApiUrlText:$SETTINGS_ENTRY
                    const-string v$tmpB, "${textApi.value}"
                    invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
                    """ else ""
                }
                """
                // ${
                //     if (imgApi.value != null) """
                //     sget-object v$tmpA, $QBF_SETTINGS_PREFS->ApiUrlImage:$SETTINGS_ENTRY
                //     const-string v$tmpB, "${imgApi.value}"
                //     invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
                //     """ else ""
                // }
            )
        }
    }
}
