package xyz.emlyn.patch.instagram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

object StringLookupFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        string("arsc"),
        string("unknown"),
    )
)

object LiteralValueFingerprint : Fingerprint(
    parameters = listOf(
        "Lcom/instagram/settings2/core/model/FbtModelSource;"
    )
)

object WhitehatSettingsFingerprint : Fingerprint(
    filters = listOf(
        string("whitehat_settings_android_ig"),
        opcode(Opcode.CONST, location = MatchAfterWithin(15)),
        string("whitehat_settings"),
        opcode(Opcode.INVOKE_DIRECT_RANGE, location = MatchAfterWithin(10)),
    )
)

object NativeRouteResolveFingerprint : Fingerprint(
    filters = listOf(
        string("feed_favorites"),
        string("homecoming_opt_out_bottom_sheet"),
        string("settings2_screen_id")
    )
)

object GotoRouteFingerprint : Fingerprint(
    name = "run",
    filters = listOf(
        string("new_settings_session"),
        string("bottom_sheet")
    )
)
