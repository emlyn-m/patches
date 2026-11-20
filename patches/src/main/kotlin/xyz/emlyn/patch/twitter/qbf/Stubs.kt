package xyz.emlyn.patch.twitter.qbf

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.InstructionLocation
import app.morphe.util.cloneMutable
import app.morphe.util.getMutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TWEET_CLASS = "Lxyz/emlyn/patchextension/qbf/twitter/Tweet;"
private const val COMPOSE_CLASS = "Lxyz/emlyn/patchextension/qbf/twitter/Compose;"
private const val TEXT_STUBS_CLASS = "Lxyz/emlyn/patchextension/qbf/twitter/text/TextStubs;"

private const val SPAN_STYLE_PARAMS = 15
private const val SPAN_STYLE_BACKGROUND_ONLY_MASK = 0xf7ff

internal fun MethodReference.smali() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

internal fun Method.references() = (implementation?.instructions ?: emptyList())
    .mapNotNull { (it as? ReferenceInstruction)?.reference }

val qbfStubsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        fun getFieldFromToString(
            definingClass: String,
            prefix: String,
            maxDist: Int = 2,
            opcode: Opcode = Opcode.IGET_OBJECT,
        ): FieldReference {
            return Fingerprint(
                definingClass = definingClass,
                name = "toString",
                filters = listOf(
                    string(prefix),
                    fieldAccess(
                        definingClass = "this",
                        opcode = opcode,
                        location = InstructionLocation.MatchAfterWithin(maxDist),
                    )
                )
            ).instructionMatches.last().getInstruction<ReferenceInstruction>().reference as FieldReference
        }

        fun stub(definingClass: String, name: String, additionalRegisters: Int) =
            mutableClassDefBy(definingClass).let { classDef ->
                val original = classDef.methods.first { it.name == name }.getMutableMethod()
                original.cloneMutable(additionalRegisters = additionalRegisters).also {
                    classDef.methods.remove(original)
                    classDef.methods.add(it)
                }
            }

        // Tweet.fromObfuscated() stub
        val availableTweetClass = AvailablePostToStringFingerprint.method.definingClass
        val postIdField = getFieldFromToString(availableTweetClass, ", postId=")
        val postTextField = getFieldFromToString(availableTweetClass, ", text=", 4)
        val notePostField = getFieldFromToString(availableTweetClass, ", notePostState=")
        val notePostTextField = getFieldFromToString(
            NotePostStateToStringFingerprint.method.definingClass, "NotePostState(text="
        )

        stub(TWEET_CLASS, "fromObfuscated", 5).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $availableTweetClass

            iget-object v1, p0, $postIdField
            invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;
            move-result-object v1

            iget-object v2, p0, $postTextField
            move-object v3, v2
            iget-object v4, p0, $notePostField
            if-eqz v4, :no_note
            iget-object v4, v4, $notePostTextField
            if-eqz v4, :no_note
            move-object v3, v4
            :no_note

            new-instance v0, $TWEET_CLASS
            invoke-direct {v0, v1, v2, v3}, $TWEET_CLASS-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
            return-object v0
            """
        )

        // Compose.padding
        val padding = (HeaderPaddingFingerprint.instructionMatches.last()
            .getInstruction<ReferenceInstruction>().reference as MethodReference).smali()

        stub(COMPOSE_CLASS, "padding", 6).addInstructions(
            0,
            """
            check-cast p0, Landroidx/compose/ui/Modifier;
            move-object v0, p0
            move v1, p1
            move v2, p2
            move v3, p3
            move v4, p4
            const/4 v5, 0x0
            invoke-static/range {v0 .. v5}, $padding
            move-result-object v0
            return-object v0
            """
        )

        // Compose.headerAlign
        GrokButtonFingerprint.method.apply {
            val alignInit = GrokButtonFingerprint.instructionMatches[1]
            val alignInitRef = alignInit.getInstruction<ReferenceInstruction>().reference as MethodReference
            val alignmentReg = alignInit.getInstruction<FiveRegisterInstruction>().registerD
            val alignmentField = (alignInit.index - 1 downTo 0).firstNotNullOfOrNull { i ->
                val insn = getInstruction(i)
                if (insn.opcode == Opcode.SGET_OBJECT && (insn as OneRegisterInstruction).registerA == alignmentReg)
                    (insn as ReferenceInstruction).reference.toString()
                else null
            } ?: throw PatchException("failed to find alignment for modifier")

            stub(COMPOSE_CLASS, "headerAlign", 2).addInstructions(
                0,
                """
                sget-object v1, $alignmentField
                new-instance v0, ${alignInitRef.definingClass}
                invoke-direct {v0, v1}, ${alignInitRef.smali()}
                return-object v0
                """
            )
        }

        // Compose.androidView
        stub(COMPOSE_CLASS, "androidView", 6).addInstructions(
            0,
            """
            check-cast p0, Lkotlin/jvm/functions/Function1;
            check-cast p1, Landroidx/compose/ui/Modifier;
            check-cast p2, Lkotlin/jvm/functions/Function1;
            check-cast p3, Landroidx/compose/runtime/Composer;
            move-object v0, p0
            move-object v1, p1
            move-object v2, p2
            move-object v3, p3
            const/4 v4, 0x0
            const/4 v5, 0x0
            invoke-static/range {v0 .. v5}, ${AndroidViewFingerprint.method.smali()}
            return-void
            """
        )

        val style = RichTextStyleFingerprint.method
        val styleRefs = style.references()
        val builderClass = style.parameterTypes[0].toString()
        val annotatedStringClass = RichTextBuildFingerprint.method.returnType

        // TextStubs.htmlUnescape: ksoup
        val ksoupUnescape = styleRefs.filterIsInstance<MethodReference>().firstOrNull {
            it.definingClass.startsWith("Lcom/mohamedrejeb/ksoup/") &&
                    it.parameterTypes.map { p -> p.toString() } == listOf("Ljava/lang/String;") &&
                    it.returnType == "Ljava/lang/String;"
        } ?: throw PatchException("failed to find ksoup unescape in rich text styling")

        stub(TEXT_STUBS_CLASS, "htmlUnescape", 1).addInstructions(
            0,
            """
            invoke-static {p0}, ${ksoupUnescape.smali()}
            move-result-object v0
            return-object v0
            """
        )

        val entityClass = AdjustedTextEntityToStringFingerprint.method.definingClass
        for ((name, prefix) in listOf("entityStart" to ", adjustedStartIdx=", "entityEnd" to ", adjustedEndIdx=")) {
            val field = getFieldFromToString(entityClass, prefix, opcode = Opcode.IGET)
            stub(TEXT_STUBS_CLASS, name, 1).addInstructions(
                0,
                """
                check-cast p0, $entityClass
                iget v0, p0, $field
                return v0
                """
            )
        }

        val builderTextField = styleRefs.filterIsInstance<FieldReference>().firstOrNull {
            it.definingClass == builderClass && it.type == "Ljava/lang/StringBuilder;"
        } ?: throw PatchException("failed to find AnnotatedString.Builder text field")

        stub(COMPOSE_CLASS, "builderText", 1).addInstructions(
            0,
            """
            check-cast p0, $builderClass
            iget-object v0, p0, $builderTextField
            invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
            move-result-object v0
            return-object v0
            """
        )

        val builderMethods = classDefBy(builderClass).methods
        builderMethods.singleOrNull {
            it.name == "<init>" && it.parameterTypes.map { p -> p.toString() } == listOf(annotatedStringClass)
        } ?: throw PatchException("failed to find AnnotatedString.Builder(AnnotatedString)")
        val toAnnotatedString = builderMethods.singleOrNull {
            it.parameterTypes.isEmpty() && it.returnType == annotatedStringClass
        } ?: throw PatchException("failed to find AnnotatedString.Builder.toAnnotatedString")

        stub(COMPOSE_CLASS, "builderFrom", 1).addInstructions(
            0,
            """
            check-cast p0, $annotatedStringClass
            new-instance v0, $builderClass
            invoke-direct {v0, p0}, $builderClass-><init>($annotatedStringClass)V
            return-object v0
            """
        )

        stub(COMPOSE_CLASS, "builderBuild", 1).addInstructions(
            0,
            """
            check-cast p0, $builderClass
            invoke-virtual {p0}, ${toAnnotatedString.smali()}
            move-result-object v0
            return-object v0
            """
        )

        val spanStyleInit = styleRefs.filterIsInstance<MethodReference>().firstOrNull {
            it.name == "<init>" && it.parameterTypes.size == SPAN_STYLE_PARAMS &&
                    it.parameterTypes[0].toString() == "J" && it.parameterTypes.last().toString() == "I"
        } ?: throw PatchException("failed to find SpanStyle constructor in rich text styling")
        val spanStyleClass = spanStyleInit.definingClass
        val addStyle = styleRefs.filterIsInstance<MethodReference>().firstOrNull {
            it.definingClass == builderClass && it.returnType == "V" &&
                    it.parameterTypes.map { p -> p.toString() } == listOf(spanStyleClass, "I", "I")
        } ?: throw PatchException("failed to find AnnotatedString.Builder.addStyle")

        stub(COMPOSE_CLASS, "builderAddStyle", 0).addInstructions(
            0,
            """
            check-cast p0, $builderClass
            check-cast p1, $spanStyleClass
            invoke-virtual {p0, p1, p2, p3}, ${addStyle.smali()}
            return-void
            """
        )

        stub(COMPOSE_CLASS, "spanStyleBackground", 20).addInstructions(
            0,
            """
            new-instance v0, $spanStyleClass
            const-wide/16 v1, 0x0
            const-wide/16 v3, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x0
            const/4 v7, 0x0
            const/4 v8, 0x0
            const/4 v9, 0x0
            const-wide/16 v10, 0x0
            const/4 v12, 0x0
            const/4 v13, 0x0
            const/4 v14, 0x0
            move-wide/from16 v15, p0
            const/16 v17, 0x0
            const/16 v18, 0x0
            const v19, $SPAN_STYLE_BACKGROUND_ONLY_MASK
            invoke-direct/range {v0 .. v19}, ${spanStyleInit.smali()}
            return-object v0
            """
        )

        val (mutableStateOfCall, _) = RememberUpdatedStateFingerprint.instructionMatches
        val mutableStateOf =
            (mutableStateOfCall.getInstruction<ReferenceInstruction>().reference as MethodReference).smali()
        val mutableStateClass = RememberUpdatedStateFingerprint.method.returnType
        val stateClass = classDefBy(mutableStateClass).interfaces.firstOrNull { iface ->
            classDefBy(iface).methods.any { it.name == "getValue" && it.parameterTypes.isEmpty() }
        } ?: throw PatchException("failed to find State interface of $mutableStateClass")

        stub(COMPOSE_CLASS, "mutableStateOf", 1).addInstructions(
            0,
            """
            invoke-static {p0}, $mutableStateOf
            move-result-object v0
            return-object v0
            """
        )

        stub(COMPOSE_CLASS, "stateGet", 1).addInstructions(
            0,
            """
            check-cast p0, $stateClass
            invoke-interface {p0}, $stateClass->getValue()Ljava/lang/Object;
            move-result-object v0
            return-object v0
            """
        )

        stub(COMPOSE_CLASS, "stateSet", 0).addInstructions(
            0,
            """
            check-cast p0, $mutableStateClass
            invoke-interface {p0, p1}, $mutableStateClass->setValue(Ljava/lang/Object;)V
            return-void
            """
        )
    }
}
