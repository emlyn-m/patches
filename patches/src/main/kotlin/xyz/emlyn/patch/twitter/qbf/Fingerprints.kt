package xyz.emlyn.patch.twitter.qbf

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.resource.ResourceType
import com.android.tools.smali.dexlib2.Opcode


object GrokButtonFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "post_option_ask_grok"),
        methodCall(
            definingClass = "Landroidx/compose/foundation/layout/",
            name = "<init>",
            parameters = listOf("Landroidx/compose/ui/h;"),
        ),
        methodCall(
            parameters = listOf(
                "Lcom/x/icons/",
                "Ljava/lang/String;",
                "Landroidx/compose/ui/Modifier;",
                "J",
                "Z",
                "Landroidx/compose/runtime/Composer;",
                "I",
                "I",
            ),
            returnType = "V",
        ),
    )
)

object ShouldShowGrokButtonFingerprint : Fingerprint(
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        resourceLiteral(
            ResourceType.STRING,
            "post_option_ask_grok",
            location = InstructionLocation.MatchAfterWithin(5)
        ),
    ),
)

object FocalHeaderFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "super_follow"),
        methodCall(
            parameters = listOf(
                "Lcom/x/urt/items/post/options/",
                "Landroidx/compose/ui/Modifier;",
                "Landroidx/compose/runtime/Composer;",
                "I",
            ),
            returnType = "V",
        ),
    )
)

object HeaderPaddingFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "post_option_ask_grok"),
        methodCall(
            definingClass = "Landroidx/compose/foundation/layout/",
            parameters = listOf("Landroidx/compose/ui/Modifier;", "F", "F", "F", "F", "I"),
            returnType = "Landroidx/compose/ui/Modifier;",
        ),
    )
)

object AndroidViewFingerprint : Fingerprint(
    definingClass = "Landroidx/compose/ui/viewinterop/",
    returnType = "V",
    parameters = listOf(
        "Lkotlin/jvm/functions/Function1;",
        "Landroidx/compose/ui/Modifier;",
        "Lkotlin/jvm/functions/Function1;",
        "Landroidx/compose/runtime/Composer;",
        "I",
        "I",
    ),
    filters = listOf(
        literal(-0x6a521d79),
    )
)

object AvailablePostToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("AvailablePost(entryId="),
    filters = listOf(
        string(", postId="),
        fieldAccess(
            definingClass = "this",
            opcode = Opcode.IGET_OBJECT,
            location = InstructionLocation.MatchAfterWithin(2),
        ),
    )
)

object RichTextBuildFingerprint : Fingerprint(
    definingClass = "Lcom/x/ui/common/text/",
    returnType = "Landroidx/compose/ui/text/",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "Z",
        "Z",
        "Ljava/util/Map;",
        "Ljava/util/Map;",
        "Landroidx/compose/runtime/Composer;",
        "I",
    ),
)

object RichTextStyleFingerprint : Fingerprint(
    definingClass = "Lcom/x/ui/common/text/",
    returnType = "V",
    parameters = listOf(
        "Landroidx/compose/ui/text/e;",
        "Ljava/lang/String;",
        "Ljava/util/List;",
        "Ljava/util/ArrayList;",
        "J",
    ),
)

object AdjustedTextEntityToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("AdjustedTextEntity(entity="),
)

object NotePostStateToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("NotePostState(text="),
)

object RememberUpdatedStateFingerprint : Fingerprint(
    definingClass = "Landroidx/compose/runtime/",
    returnType = "Landroidx/compose/runtime/",
    parameters = listOf("Ljava/lang/Object;", "Landroidx/compose/runtime/Composer;"),
    filters = listOf(
        // mutableStateOf(value)
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Landroidx/compose/runtime/",
        ),
        methodCall(
            name = "setValue",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "V",
        ),
    )
)
