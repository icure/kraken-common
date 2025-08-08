package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonDiscriminated
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v1.handlers.JacksonStructureElementDeserializer

@JsonDeserialize(using = JacksonStructureElementDeserializer::class)
interface StructureElement

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("textfield")
class TextField(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("measure-field")
class MeasureField(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("number-field")
class NumberField(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("date-picker")
class DatePicker(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("time-picker")
class TimePicker(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("multiple-choice")
class MultipleChoice(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("dropdown")
class DropdownField(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("radio-button")
class RadioButton(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("checkbox")
class CheckBox(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)

@JsonPolymorphismRoot(Field::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDiscriminated("date-time-picker")
class DateTimePicker(
	field: String,
	shortLabel: String? = null,
	rows: Int? = null,
	columns: Int? = null,
	grows: Boolean? = null,
	multiline: Boolean? = null,
	schema: String? = null,
	tags: List<String>? = null,
	codifications: List<String>? = null,
	options: Map<String, Any>? = null,
	labels: Map<String, Any>? = null,
	value: String? = null,
	unit: String? = null,
	required: Boolean? = null,
	hideCondition: String? = null,
	now: Boolean? = null,
	translate: Boolean? = null,
) : Field(
	field,
	shortLabel,
	rows,
	columns,
	grows,
	schema,
	tags,
	codifications,
	options,
	hideCondition,
	required,
	multiline,
	value,
	labels,
	unit,
	now,
	translate,
)
