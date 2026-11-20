package xyz.emlyn.patch.twitter.noageverif

import xyz.emlyn.patch.shared.Constants.TWITTER_COMPAT

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string


@Suppress("unused")
val noAgeVerifPatch = bytecodePatch(
    name = "Bypass Age Verification",
    description = "Disable age verification requirement for sensitive media.",
    default = true
) {
    compatibleWith(TWITTER_COMPAT)

    execute {
        val blurImageInterstitialFingerprint = Fingerprint(
            name = "toString",
            filters = listOf(string("BlurImageInterstitial(opacity=")),
        )

        val buildAgeVerificationInterstitialFingerprint = Fingerprint(
            name = "<init>",
            custom = { _, classDef ->
                classDef.type == blurImageInterstitialFingerprint.originalClassDef.type
            },
        )

        buildAgeVerificationInterstitialFingerprint.method.addInstructionsWithLabels(
            5,
            """
            if-eqz p6, :noid
            invoke-interface {p6}, Ljava/util/List;->clear()V
            :noid
            nop
            """
        )
    }
}
