package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.services.external.rest.v2.handlers.JacksonFieldDeserializer

@JsonDeserialize(using = JacksonFieldDeserializer::class)
@JsonDiscriminator("type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface Field : StructureElement {
	val field: String
	val shortLabel: String?
	val rows: Int?
	val columns: Int?
	val grows: Boolean?
	val schema: String?
	val tags: List<String>?
	val codifications: List<String>?
	val options: Map<String, String>?
	val hideCondition: String?
	val required: Boolean?
	val multiline: Boolean?
	val value: String?
	val labels: Map<String, String>?
	val unit: String?
	val now: Boolean?
	val translate: Boolean?

	val type: FieldType
		get() = FieldType.fromClass(this::class)
}
