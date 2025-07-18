package org.taktik.icure.services.external.rest.v1.dto.embed.form.template

import kotlin.reflect.KClass

@Suppress("EnumEntryName")
enum class FieldType(
	val clazz: KClass<out Field>,
) {
	textfield(TextField::class),
	`measure-field`(MeasureField::class),
	`number-field`(NumberField::class),
	`date-picker`(DatePicker::class),
	`time-picker`(TimePicker::class),
	`date-time-picker`(DateTimePicker::class),
	`multiple-choice`(MultipleChoice::class),
	dropdown(DropdownField::class),
	`radio-button`(RadioButton::class),
	checkbox(CheckBox::class),
	;

	companion object {
		fun fromClass(clazz: KClass<out Field>): FieldType = values().firstOrNull { it.clazz == clazz } ?: throw IllegalArgumentException("Unknown field type $clazz")
	}
}
