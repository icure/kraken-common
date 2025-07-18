package org.taktik.icure.services.external.rest.v2.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.handlers.JacksonStructureElementDeserializer

@JsonDeserialize(using = JacksonStructureElementDeserializer::class)
sealed interface StructureElement

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("textfield")
data class TextField(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("measure-field")
data class MeasureField(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("number-field")
data class NumberField(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("date-picker")
data class DatePicker(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("time-picker")
data class TimePicker(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("multiple-choice")
data class MultipleChoice(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("dropdown")
data class DropdownField(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("radio-button")
data class RadioButton(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("checkbox")
data class CheckBox(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("date-time-picker")
data class DateTimePicker(
	@get:Schema(required = true)
	override val field: String,
	override val shortLabel: String? = null,
	override val rows: Int? = null,
	override val columns: Int? = null,
	override val grows: Boolean? = null,
	override val multiline: Boolean? = null,
	override val schema: String? = null,
	override val tags: List<String>? = null,
	override val codifications: List<String>? = null,
	override val options: Map<String, String>? = null,
	override val labels: Map<String, String>? = null,
	override val value: String? = null,
	override val unit: String? = null,
	override val required: Boolean? = null,
	override val hideCondition: String? = null,
	override val now: Boolean? = null,
	override val translate: Boolean? = null,
) : Field
