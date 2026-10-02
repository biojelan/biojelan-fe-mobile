package id.biojelan.app.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/*
 * Backend-nya Laravel; kolom `decimal` Eloquent (mis. `total_price`, `price_per_liter`) di-cast lewat
 * PHP `(float)` sebelum di-encode JSON, jadi angka utuh tetap punya ".0" (mis. 8000.0, bukan 8000) dan
 * kadang datang sebagai string. Serializer di bawah menerima kedua bentuk supaya app tidak crash.
 */

private fun JsonDecoder.primitiveOrNull(): JsonPrimitive? {
    val el = decodeJsonElement()
    return if (el is JsonPrimitive && el !is JsonNull) el else null
}

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        return json.primitiveOrNull()?.content ?: ""
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val json = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return json.primitiveOrNull()?.content?.toDoubleOrNull() ?: 0.0
    }

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val json = decoder as? JsonDecoder ?: return decoder.decodeLong()
        val content = json.primitiveOrNull()?.content ?: return 0L
        return content.toLongOrNull() ?: content.toDoubleOrNull()?.toLong() ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val json = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val content = json.primitiveOrNull()?.content?.trim()?.lowercase() ?: return false
        return content == "true" || content == "1"
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}
