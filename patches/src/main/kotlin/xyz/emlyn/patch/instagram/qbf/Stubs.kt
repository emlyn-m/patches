package xyz.emlyn.patch.instagram.qbf

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.util.cloneMutable
import app.morphe.util.getMutableMethod
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val IG_MEDIA_CLASS = "Lxyz/emlyn/patchextension/qbf/ig/IgMedia;"

internal fun MethodReference.smali() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"
private fun FieldReference.smali() = "$definingClass->$name:$type"

private val Instruction.methodRef get() = (this as ReferenceInstruction).reference as MethodReference
private val Instruction.fieldRef get() = (this as ReferenceInstruction).reference as FieldReference
private val Instruction.typeRef get() = ((this as ReferenceInstruction).reference as TypeReference).type

internal lateinit var uiStateClass: String
internal lateinit var mediaStateClass: String

val igQbfStubsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        fun stub(definingClass: String, name: String, additionalRegisters: Int) =
            mutableClassDefBy(definingClass).let { classDef ->
                val original = classDef.methods.first { it.name == name }.getMutableMethod()
                original.cloneMutable(additionalRegisters = additionalRegisters).also {
                    classDef.methods.remove(original)
                    classDef.methods.add(it)
                }
            }

        uiStateClass = MediaUfiUiStateToStringFingerprint.method.definingClass
        val mediaField = classDefBy(uiStateClass).fields.singleOrNull { it.type == MEDIA_CLASS }
            ?: throw PatchException("failed to find Media field in $uiStateClass")

        stub(IG_MEDIA_CLASS, "fromUiState", 1).addInstructions(
            0,
            """
            check-cast p0, $uiStateClass
            iget-object v0, p0, $uiStateClass->${mediaField.name}:$MEDIA_CLASS
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "id", 1).addInstructions(
            0,
            """
            check-cast p0, $MEDIA_CLASS
            invoke-virtual {p0}, $MEDIA_CLASS->getId()Ljava/lang/String;
            move-result-object v0
            return-object v0
            """
        )

        val captionGetter = MediaCaptionFingerprint.method
        val captionClass = captionGetter.returnType
        val captionText = Fingerprint(
            definingClass = MEDIA_EXT_CLASS,
            returnType = "Ljava/lang/String;",
            parameters = listOf(MEDIA_CLASS),
            filters = listOf(
                methodCall(definingClass = MEDIA_CLASS, name = captionGetter.name, returnType = captionClass),
                methodCall(
                    opcode = Opcode.INVOKE_INTERFACE,
                    definingClass = captionClass,
                    returnType = "Ljava/lang/String;",
                    location = InstructionLocation.MatchAfterWithin(4),
                ),
            )
        ).instructionMatches.last().getInstruction<ReferenceInstruction>().reference as MethodReference

        stub(IG_MEDIA_CLASS, "caption", 1).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $MEDIA_CLASS
            invoke-virtual {p0}, $MEDIA_CLASS->${captionGetter.name}()$captionClass
            move-result-object v0
            if-eqz v0, :none
            invoke-interface {v0}, ${captionText.smali()}
            move-result-object v0
            :none
            return-object v0
            """
        )

        val imageInfoGetter = MediaImageInfoFingerprint.method
        val (_, candidatesCall, _) = MediaImageInfoSetterFingerprint.instructionMatches
        val candidatesGetter = candidatesCall
            .getInstruction<ReferenceInstruction>().reference as MethodReference

        stub(IG_MEDIA_CLASS, "imageCandidates", 1).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $MEDIA_CLASS
            invoke-virtual {p0}, $MEDIA_CLASS->${imageInfoGetter.name}()$IMAGE_INFO_CLASS
            move-result-object v0
            if-eqz v0, :none
            invoke-interface {v0}, ${candidatesGetter.smali()}
            move-result-object v0
            :none
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "carousel", 1).addInstructions(
            0,
            """
            check-cast p0, $MEDIA_CLASS
            invoke-virtual {p0}, $MEDIA_CLASS->${MediaCarouselFingerprint.method.name}()Ljava/util/List;
            move-result-object v0
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "mediaType", 1).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $MEDIA_CLASS
            invoke-virtual {p0}, $MEDIA_CLASS->${MediaTypeFingerprint.method.name}()Ljava/lang/Integer;
            move-result-object v0
            if-eqz v0, :none
            invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I
            move-result v0
            return v0
            :none
            const/4 v0, 0x0
            return v0
            """
        )

        stub(IG_MEDIA_CLASS, "imageUrl", 1).addInstructions(
            0,
            """
            check-cast p0, $IMAGE_URL_CLASS
            invoke-interface {p0}, $IMAGE_URL_CLASS->getUrl()Ljava/lang/String;
            move-result-object v0
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "imageWidth", 1).addInstructions(
            0,
            """
            check-cast p0, $IMAGE_URL_CLASS
            invoke-interface {p0}, $IMAGE_URL_CLASS->getWidth()I
            move-result v0
            return v0
            """
        )

        stub(IG_MEDIA_CLASS, "imageHeight", 1).addInstructions(
            0,
            """
            check-cast p0, $IMAGE_URL_CLASS
            invoke-interface {p0}, $IMAGE_URL_CLASS->getHeight()I
            move-result v0
            return v0
            """
        )

        val infra = ImageInfraFingerprint.classDef
        val singleton = infra.methods.singleOrNull {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes.isEmpty() && it.returnType == infra.type
        } ?: throw PatchException("failed to find image loader singleton in ${infra.type}")
        val contains = infra.methods.singleOrNull {
            it.returnType == "Ljava/lang/Boolean;" && it.parameterTypes.map { t -> t.toString() } == listOf(
                IMAGE_URL_CLASS
            )
        } ?: throw PatchException("failed to find disk presence check in ${infra.type}")
        val containsInsns = contains.implementation!!.instructions
        val holderField = containsInsns.first {
            it.opcode == Opcode.IGET_OBJECT && it.fieldRef.definingClass == infra.type
        }.fieldRef
        val cacheKey = containsInsns.first {
            it.opcode == Opcode.INVOKE_VIRTUAL && it.methodRef.definingClass == infra.type &&
                    it.methodRef.returnType == "Ljava/lang/String;"
        }.methodRef

        val lookup = DiskCacheLookupFingerprint.method.implementation!!.instructions.toList()
        val readIndex = lookup.indexOfFirst {
            it.opcode == Opcode.INVOKE_INTERFACE &&
                    it.methodRef.parameterTypes.map { t -> t.toString() } == listOf(
                "Ljava/lang/String;",
                "Ljava/util/Map;"
            )
        }
        if (readIndex < 0)
            throw PatchException("failed to find disk read in ${DiskCacheLookupFingerprint.method.definingClass}")
        val read = lookup[readIndex].methodRef
        val afterRead = lookup.drop(readIndex + 1)
        val optionalValue = afterRead.first {
            it.opcode == Opcode.IGET_OBJECT && it.fieldRef.definingClass == read.returnType
        }.fieldRef
        val entryClass = afterRead.first { it.opcode == Opcode.CHECK_CAST }.typeRef
        val entryMetadata = afterRead.first {
            it.opcode == Opcode.IGET_OBJECT && it.fieldRef.definingClass == entryClass
        }.fieldRef
        val metaClass = afterRead.filter { it.opcode == Opcode.CHECK_CAST }
            .first { it.typeRef != entryClass }.typeRef
        val metaJson = afterRead.first {
            it.opcode == Opcode.IGET_OBJECT && it.fieldRef.definingClass == metaClass && it.fieldRef.type == "Lorg/json/JSONObject;"
        }.fieldRef
        val entryStream = classDefBy(entryClass).fields.singleOrNull {
            classDefByOrNull(it.type)?.superclass == "Ljava/io/InputStream;"
        } ?: throw PatchException("failed to find stream field in $entryClass")
        val cacheGetter = classDefBy(holderField.type).methods.singleOrNull {
            it.parameterTypes.isEmpty() && it.returnType == read.definingClass
        } ?: throw PatchException("failed to find disk cache getter in ${holderField.type}")

        stub(IG_MEDIA_CLASS, "diskEntry", 3).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $IMAGE_URL_CLASS
            invoke-static {}, ${singleton.smali()}
            move-result-object v0
            if-eqz v0, :none
            invoke-virtual {v0, p0}, ${cacheKey.smali()}
            move-result-object v1
            if-eqz v1, :none
            iget-object v0, v0, ${holderField.smali()}
            invoke-virtual {v0}, ${cacheGetter.smali()}
            move-result-object v0
            new-instance v2, Ljava/util/HashMap;
            invoke-direct {v2}, Ljava/util/HashMap;-><init>()V
            invoke-interface {v0, v1, v2}, ${read.smali()}
            move-result-object v0
            iget-object v0, v0, ${optionalValue.smali()}
            return-object v0
            :none
            const/4 v0, 0x0
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "diskEntryStream", 1).addInstructions(
            0,
            """
            check-cast p0, $entryClass
            iget-object v0, p0, $entryClass->${entryStream.name}:${entryStream.type}
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "diskEntryMetadata", 1).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $entryClass
            iget-object v0, p0, ${entryMetadata.smali()}
            if-eqz v0, :none
            iget-object v0, v0, ${optionalValue.smali()}
            if-eqz v0, :none
            check-cast v0, $metaClass
            iget-object v0, v0, ${metaJson.smali()}
            return-object v0
            :none
            const/4 v0, 0x0
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "viewBitmap", 1).addInstructions(
            0,
            """
            check-cast p0, $IG_IMAGE_VIEW_CLASS
            invoke-virtual {p0}, $IG_IMAGE_VIEW_CLASS->getBitmap()Landroid/graphics/Bitmap;
            move-result-object v0
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "viewScans", 1).addInstructions(
            0,
            """
            check-cast p0, $IG_IMAGE_VIEW_CLASS
            invoke-virtual {p0}, $IG_IMAGE_VIEW_CLASS->getCurrentScans()Ljava/util/concurrent/atomic/AtomicInteger;
            move-result-object v0
            return-object v0
            """
        )

        val layerUpdate = VitoLayerUpdateFingerprint.method
        val layer = layerUpdate.definingClass
        val sourceType = layerUpdate.parameterTypes[5].toString()
        val layerInsns = layerUpdate.implementation!!.instructions.toList()
        val layerSource = layerInsns.first {
            it.opcode == Opcode.IPUT_OBJECT && it.fieldRef.definingClass == layer && it.fieldRef.type == sourceType
        }.fieldRef
        val layerBounds = layerInsns.first {
            it.opcode == Opcode.IPUT_OBJECT && it.fieldRef.definingClass == layer && it.fieldRef.type == "Landroid/graphics/Rect;"
        }.fieldRef
        val layerMatrix = layerInsns.first {
            it.opcode == Opcode.IPUT_OBJECT && it.fieldRef.definingClass != layer && it.fieldRef.type == "Landroid/graphics/Matrix;"
        }.fieldRef
        val layerScale = classDefBy(layer).fields.singleOrNull { it.type == layerMatrix.definingClass }
            ?: throw PatchException("failed to find scale holder in $layer")
        val (sourceWidth, sourceHeight) = layerInsns.filter {
            it.opcode == Opcode.INVOKE_VIRTUAL && it.methodRef.definingClass == sourceType &&
                    it.methodRef.returnType == "I" && it.methodRef.parameterTypes.isEmpty()
        }.map { it.methodRef }.take(2).also {
            if (it.size != 2) throw PatchException("failed to find source size getters in $layer")
        }

        val vitoDrawable = Fingerprint(
            returnType = "V",
            parameters = listOf("Landroid/graphics/Rect;"),
            filters = listOf(methodCall(definingClass = layer, name = layerUpdate.name)),
            custom = { _, classDef -> classDef.superclass == "Landroid/graphics/drawable/Drawable;" },
        ).classDef.type
        val imageLayer = Fingerprint(
            filters = listOf(
                methodCall(definingClass = vitoDrawable, parameters = listOf("Ljava/io/Closeable;"), returnType = "V"),
                fieldAccess(
                    opcode = Opcode.IGET_OBJECT,
                    definingClass = vitoDrawable,
                    type = layer,
                    location = InstructionLocation.MatchAfterImmediately(),
                ),
            )
        ).instructionMatches.last().getInstruction<ReferenceInstruction>().reference as FieldReference

        stub(IG_MEDIA_CLASS, "vitoImageLayer", 1).addInstructionsWithLabels(
            0,
            """
            instance-of v0, p0, $vitoDrawable
            if-eqz v0, :none
            check-cast p0, $vitoDrawable
            iget-object v0, p0, ${imageLayer.smali()}
            return-object v0
            :none
            const/4 v0, 0x0
            return-object v0
            """
        )

        listOf("layerSourceWidth" to sourceWidth, "layerSourceHeight" to sourceHeight).forEach { (name, getter) ->
            stub(IG_MEDIA_CLASS, name, 1).addInstructionsWithLabels(
                0,
                """
                check-cast p0, $layer
                iget-object v0, p0, ${layerSource.smali()}
                if-eqz v0, :none
                invoke-virtual {v0}, ${getter.smali()}
                move-result v0
                return v0
                :none
                const/4 v0, -0x1
                return v0
                """
            )
        }

        stub(IG_MEDIA_CLASS, "layerBounds", 1).addInstructions(
            0,
            """
            check-cast p0, $layer
            iget-object v0, p0, ${layerBounds.smali()}
            return-object v0
            """
        )

        stub(IG_MEDIA_CLASS, "layerMatrix", 1).addInstructionsWithLabels(
            0,
            """
            check-cast p0, $layer
            iget-object v0, p0, $layer->${layerScale.name}:${layerScale.type}
            if-eqz v0, :none
            iget-object v0, v0, ${layerMatrix.smali()}
            return-object v0
            :none
            const/4 v0, 0x0
            return-object v0
            """
        )

        val mediaStateField = classDefBy(uiStateClass).fields.firstOrNull { field ->
            classDefByOrNull(field.type)?.methods?.any { method ->
                method.returnType == "V" && method.parameterTypes.map { it.toString() } == listOf("I") &&
                        method.implementation?.instructions?.any {
                            (it as? ReferenceInstruction)?.reference.let { ref ->
                                ref is MethodReference && ref.definingClass == "Ljava/lang/Math;" && ref.name == "max"
                            }
                        } == true
            } == true
        } ?: throw PatchException("failed to find per-post state field in $uiStateClass")
        mediaStateClass = mediaStateField.type
        val indexField = carouselIndexFingerprint.originalMethod.implementation!!.instructions
            .first { it.opcode == Opcode.IPUT }
            .let { (it as ReferenceInstruction).reference as FieldReference }

        stub(IG_MEDIA_CLASS, "carouselIndex", 1).addInstructions(
            0,
            """
            check-cast p0, $uiStateClass
            iget-object v0, p0, $uiStateClass->${mediaStateField.name}:$mediaStateClass
            iget v0, v0, $indexField
            return v0
            """
        )

        stub(IG_MEDIA_CLASS, "stateFromUiState", 1).addInstructions(
            0,
            """
            check-cast p0, $uiStateClass
            iget-object v0, p0, $uiStateClass->${mediaStateField.name}:$mediaStateClass
            return-object v0
            """
        )

    }
}

internal val carouselIndexFingerprint by lazy {
    Fingerprint(
        definingClass = mediaStateClass,
        returnType = "V",
        parameters = listOf("I"),
        filters = listOf(
            methodCall(definingClass = "Ljava/lang/Math;", name = "max"),
        )
    )
}
