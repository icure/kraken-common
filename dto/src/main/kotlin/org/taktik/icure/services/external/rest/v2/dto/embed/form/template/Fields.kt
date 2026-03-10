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
/**
 * Marker interface for elements that can appear in a form template structure, including fields and groups.
 */
sealed interface StructureElement

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("textfield")
/**
 * A text input field in a form template.
 */
data class TextField(
	/** The field identifier. */
	@param:Schema(required = true)
	override val field: String,
	/** A short label for the field. */
	override val shortLabel: String? = null,
	/** The number of rows for display. */
	override val rows: Int? = null,
	/** The number of columns for display. */
	override val columns: Int? = null,
	/** Whether the field grows dynamically. */
	override val grows: Boolean? = null,
	/** Whether the field supports multiline input. */
	override val multiline: Boolean? = null,
	/** The schema reference for validation. */
	override val schema: String? = null,
	/** Tags associated with this field. */
	override val tags: List<String>? = null,
	/** Codification references for this field. */
	override val codifications: List<String>? = null,
	/** Additional options as key-value pairs. */
	override val options: Map<String, String>? = null,
	/** Localized labels keyed by language code. */
	override val labels: Map<String, String>? = null,
	/** The default value. */
	override val value: String? = null,
	/** The unit of measurement. */
	override val unit: String? = null,
	/** Whether the field is required. */
	override val required: Boolean? = null,
	/** A condition expression for hiding the field. */
	override val hideCondition: String? = null,
	/** Whether to default to the current date/time. */
	override val now: Boolean? = null,
	/** Whether the field value should be translated. */
	override val translate: Boolean? = null,
) : Field

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("measure-field")
/**
 * A measurement input field in a form template, used for capturing numeric values with units.
 */
data class MeasureField(
	@param:Schema(required = true)
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
/**
 * A numeric input field in a form template.
 */
data class NumberField(
	@param:Schema(required = true)
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
/**
 * A date picker field in a form template.
 */
data class DatePicker(
	@param:Schema(required = true)
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
/**
 * A time picker field in a form template.
 */
data class TimePicker(
	@param:Schema(required = true)
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
/**
 * A multiple choice field in a form template, allowing selection of multiple options.
 */
data class MultipleChoice(
	@param:Schema(required = true)
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
/**
 * A dropdown selection field in a form template.
 */
data class DropdownField(
	@param:Schema(required = true)
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
/**
 * A radio button field in a form template, allowing selection of a single option.
 */
data class RadioButton(
	@param:Schema(required = true)
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
/**
 * A checkbox field in a form template.
 */
data class CheckBox(
	@param:Schema(required = true)
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
/**
 * A date-time picker field in a form template, combining date and time selection.
 */
data class DateTimePicker(
	@param:Schema(required = true)
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
