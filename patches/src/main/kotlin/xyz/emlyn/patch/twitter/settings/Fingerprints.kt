package xyz.emlyn.patch.twitter.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType


// root settings list
object RootSettingsListFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "settings_root_account_title"),
        resourceLiteral(ResourceType.STRING, "settings_additional_resources_item_title"),
        methodCall(
            definingClass = "Lkotlin/collections/CollectionsKt;",
            name = "listOfNotNull",
            parameters = listOf("[Ljava/lang/Object;"),
            returnType = "Ljava/util/List;",
        ),
        methodCall(
            definingClass = "Lkotlin/collections/CollectionsKt;",
            name = "listOf",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Ljava/util/List;",
        ),
    )
)
