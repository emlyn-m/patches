package xyz.emlyn.patch.instagram.qbf

import xyz.emlyn.patch.shared.Constants.INSTAGRAM_COMPAT
import xyz.emlyn.patch.instagram.qbf.qbfResourcesPatch
import xyz.emlyn.patch.instagram.settings.igSettingsPatch

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions

@Suppress("unused")
val igQbfPatch = bytecodePatch(
    name = "qbf",
    description = "todo.",
    default = true
) {
    compatibleWith(INSTAGRAM_COMPAT)
    extendWith("extensions/extension.mpe")

    // attach preferences
    apiKey()
    autoOverlay()
    imgApi()

    dependsOn(igQbfStubsPatch)
    dependsOn(qbfResourcesPatch)
    dependsOn(igSettingsPatch)

    execute {
        // force null - all impls draw inside the litho footer (for now)
        UfiRendererSelectorFingerprint.method.addInstructions(
            0,
            """
            const/4 v0, 0x0
            return-object v0
            """
        )

        installLithoUfiButton()
        installMediaOverlay()
        installCarouselListener()

        registerSettingsDraw()
        setDefaultSettings()
    }
}
