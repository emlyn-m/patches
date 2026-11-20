package xyz.emlyn.patch.twitter.noforyou

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType


object PreferredTabFingerprint : Fingerprint(
    parameters = listOf("Z"),
    filters = listOf(
        string("PreferredTabRepo")
    )
)

object TabFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(
        string("Tab(homeTabType=")
    )
)

object ForYouTabFingerprint : Fingerprint(
    name = "toString",
    filters = listOf(
        string("ForYou")
    )
)

object BuildTabFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "swish_following_tweets")
    )
)
