package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminator
import org.taktik.icure.services.external.rest.v2.handlers.JacksonFieldDeserializer
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonDeserialize(using = JacksonFieldDeserializer::class)
@JsonDiscriminator("type")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Sealed interface representing a form field within a form template. Concrete implementations define
 * specific field types (text, number, date picker, etc.). Each field has a name, display configuration,
 * validation rules, and optional codification and tagging.
 */
sealed interface Field : StructureElement {
	@ActiveField val field: String
	@ActiveField val shortLabel: String?
	@ActiveField val rows: Int?
	@ActiveField val columns: Int?
	@ActiveField val grows: Boolean?
	@ActiveField val schema: String?
	@ActiveField val tags: List<String>?
	@ActiveField val codifications: List<String>?
	@ActiveField val options: Map<String, String>?
	@ActiveField val hideCondition: String?
	@ActiveField val required: Boolean?
	@ActiveField val multiline: Boolean?
	@ActiveField val value: String?
	@ActiveField val labels: Map<String, String>?
	@ActiveField val unit: String?
	@ActiveField val now: Boolean?
	@ActiveField val translate: Boolean?

	@ActiveField
	val type: FieldType
		get() = FieldType.fromClass(this::class)
}
