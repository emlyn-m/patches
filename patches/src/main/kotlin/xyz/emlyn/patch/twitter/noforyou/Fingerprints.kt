package xyz.emlyn.patch.twitter.noforyou

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

object PreferredTabFingerprint : Fingerprint(
    definingClass = "Lcom/x/home/tabbed/t;",
    name = "a",
)

object TabFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(
        string(", persistLogoWhenDeselected=")
    )
)

object ForYouTabFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(
        string("ForYou")
    )
)

object BuildTabFingerprint : Fingerprint(
    definingClass = "Lcom/x/home/tabbed/s;",
    name = "<init>",
    filters = listOf(
        string("co_timeline_topic_filter_enabled")
    )
)
