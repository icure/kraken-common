package org.taktik.icure.entities.serialization

interface MultiplatformJsonGenerator {
	fun writeStartObject()
	fun writeFieldName(key: String)
	fun writeEndObject()
	fun writeStartArray()
	fun writeEndArray()
	fun writeString(value: String)
	fun writeNumber(value: Long)
	fun writeNumber(value: Double)
	fun writeBoolean(bool: Boolean)
	fun writeNull()
}