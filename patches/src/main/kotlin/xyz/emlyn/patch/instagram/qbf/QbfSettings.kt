package  xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.option
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getFreeRegisterProvider
import xyz.emlyn.patch.shared.Constants

private const val QBF_SETTINGS = "Lxyz/emlyn/patchextension/qbf/ig/QbfSettings;"
private const val QBF_SETTINGS_PREFS = $$"Lxyz/emlyn/patchextension/qbf/ig/QbfSettings$Prefs;"
private const val SETTINGS_ENTRY = "Lxyz/emlyn/patchextension/settings/SettingsEntry;"

internal val apiKey =
    stringOption(key = "apiKey", title = "API Key", description = "Sent as `x-api-key`", required = true)
internal val autoOverlay =
    option<Boolean>(key = "autoOverlay", title = "Automatically show overlay once scanned", default = false)
internal val imgApi =
    stringOption(key = "apiImg", title = "Image API override", description = $$"POST $API, GET $API/{id}")
// internal val autoscan = option<Boolean>(key = "autoscan", title = "Automatically scan posts", default = false)


internal fun BytecodePatchContext.registerSettingsDraw() {
    Fingerprint(
        definingClass = Constants.SETTINGS_CLASS,
        name = "drawGroups"
    ).method.addInstructions(
        0,
        """
        invoke-static {p0, p1}, $QBF_SETTINGS->draw(${Constants.SETTINGS_CLASS}Landroid/widget/LinearLayout;)V
        """
    )
}

internal fun BytecodePatchContext.setDefaultSettings() {
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

            sget-object v$tmpA, $QBF_SETTINGS_PREFS->AutoOverlay:$SETTINGS_ENTRY
            sget-object v$tmpB, Ljava/lang/Boolean;->${if (autoOverlay.value!!) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
            invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
    
            ${
                if (imgApi.value != null) """
                sget-object v$tmpA, $QBF_SETTINGS_PREFS->ApiUrlImage:$SETTINGS_ENTRY
                const-string v$tmpB, "${imgApi.value}"
                invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
                """ else ""
            }
            """
            // sget-object v$tmpA, $QBF_SETTINGS_PREFS->Autoscan:$SETTINGS_ENTRY
            // sget-object v$tmpB, Ljava/lang/Boolean;->${if (autoscan.value!!) "TRUE" else "FALSE"}:Ljava/lang/Boolean;
            // invoke-virtual {v$tmpA, v$tmpB}, $SETTINGS_ENTRY->setDefault(Ljava/lang/Object;)V
        )
    }
}
