package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.services.external.rest.v1.handlers.JacksonFieldDeserializer

@JsonDeserialize(using = JacksonFieldDeserializer::class)
@JsonDiscriminator("type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
open class Field(
	val field: String,
	val shortLabel: String? = null,
	val rows: Int? = null,
	val columns: Int? = null,
	val grows: Boolean? = null,
	val schema: String? = null,
	val tags: List<String>? = null,
	val codifications: List<String>? = null,
	val options: Map<String, *>? = null,
	val hideCondition: String? = null,
	val required: Boolean? = null,
	val multiline: Boolean? = null,
	val value: String? = null,
	val labels: Map<String, *>? = null,
	val unit: String? = null,
	val now: Boolean? = null,
	val translate: Boolean? = null,
) : StructureElement {
	val type: FieldType
		get() = FieldType.fromClass(this::class)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Field) return false
		if (type != other.type) return false

		if (field != other.field) return false
		if (shortLabel != other.shortLabel) return false
		if (rows != other.rows) return false
		if (columns != other.columns) return false
		if (grows != other.grows) return false
		if (schema != other.schema) return false
		if (tags != other.tags) return false
		if (codifications != other.codifications) return false
		if (options != other.options) return false
		if (hideCondition != other.hideCondition) return false
		if (required != other.required) return false
		if (multiline != other.multiline) return false
		if (value != other.value) return false
		if (labels != other.labels) return false
		if (unit != other.unit) return false
		if (now != other.now) return false
		return translate == other.translate
	}

	override fun hashCode(): Int {
		var result = field.hashCode()
		result = 31 * result + type.hashCode()
		result = 31 * result + (shortLabel?.hashCode() ?: 0)
		result = 31 * result + (rows ?: 0)
		result = 31 * result + (columns ?: 0)
		result = 31 * result + (grows?.hashCode() ?: 0)
		result = 31 * result + (schema?.hashCode() ?: 0)
		result = 31 * result + (tags?.hashCode() ?: 0)
		result = 31 * result + (codifications?.hashCode() ?: 0)
		result = 31 * result + (options?.hashCode() ?: 0)
		result = 31 * result + (hideCondition?.hashCode() ?: 0)
		result = 31 * result + (required?.hashCode() ?: 0)
		result = 31 * result + (multiline?.hashCode() ?: 0)
		result = 31 * result + (value?.hashCode() ?: 0)
		result = 31 * result + (labels?.hashCode() ?: 0)
		result = 31 * result + (unit?.hashCode() ?: 0)
		result = 31 * result + (now?.hashCode() ?: 0)
		result = 31 * result + (translate?.hashCode() ?: 0)
		return result
	}
}
