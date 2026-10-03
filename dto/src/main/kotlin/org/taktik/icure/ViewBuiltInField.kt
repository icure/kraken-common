package org.taktik.icure

/**
 * Annotation to pass as argument to [CardinalMetadataProperty], to specify that the field can be used in custom views
 * but only as a built-in field (e.g. delegates or secret foreign key).
 * @param type is the type of built-in fields that can be used when configuring the view.
 * @param nameOverride can be used if the built-in fields needs a name parameter and that parameter is different from the name in the DTO
 * (e.g. if the entity specifies a JsonName).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ViewBuiltInField(
	val type: Type,
	val nameOverride: String = "",
) {

	enum class Type {
		Delegates,
		LinkedEntityId
	}

}