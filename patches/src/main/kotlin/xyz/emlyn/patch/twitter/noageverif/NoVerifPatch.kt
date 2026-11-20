package xyz.emlyn.patch.twitter.noageverif

import xyz.emlyn.patch.shared.Constants.TWITTER_COMPAT

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string


@Suppress("unused")
val noAgeVerifPatch = bytecodePatch(
    name = "Bypass Age Verification",
    description = "Disable age verification requirement for sensitive media.",
    default = true
) {
    compatibleWith(TWITTER_COMPAT)
    extendWith("extensions/extension.mpe")

    execute {
        val blurImageInterstitialFingerprint = Fingerprint(
            filters = listOf(string("BlurImageInterstitial(opacity=")),
        )

        val buildAgeVerificationInterstitialFingerprint = Fingerprint(
            name = "<init>",
            custom = { _, classDef ->
                classDef.type == blurImageInterstitialFingerprint.originalClassDef.type
            },
        )

        buildAgeVerificationInterstitialFingerprint.method.addInstructions(
            5,
            """
            invoke-interface {p6}, Ljava/util/List;->clear()V
            """
        )
    }
}
