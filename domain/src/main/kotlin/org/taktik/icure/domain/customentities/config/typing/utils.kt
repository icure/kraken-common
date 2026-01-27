package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError

/**
 * If [value] is [RawJson.JsonNull] requires that [nullable] is true and returns [RawJson.JsonNull] (fails if nullable is false).
 * Else executes the block and returns its value
 */
internal inline fun validatingAndIgnoringNullForStore(
	validationContext: ScopedErrorCollector,
	value: RawJson,
	nullable: Boolean,
	block: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		if (!nullable) validationContext.addError("GE-NULL", emptyMap())
		value
	} else {
		block()
	}

// Purpose on identifiers validation:
// - helps make sure that they can be used in the generated SDK Should probably also block all keywords of langauges we generate SDKs for.
// - size limit helps to reduce impact that cache has on memory consumption
private val UPPERCASE_LETTERS = 'A'..'Z'
private val LOWERCASE_LETTERS = 'a'..'z'
private val DIGITS = '0'..'9'
private val IDENTIFIER_ALLOWED_LENGTH = 1..32 // Chosen arbitrarily, might be increased later if needed
fun validateIdentifier(validationContext: ScopedErrorCollector, identifier: String) {
	if (identifier.length !in IDENTIFIER_ALLOWED_LENGTH) validationContext.addError(
		"GE-IDENTIFIER-LENGTH",
		"value" to truncateValueForErrorMessage(identifier, IDENTIFIER_ALLOWED_LENGTH.last),
		"min" to IDENTIFIER_ALLOWED_LENGTH.first.toString(),
		"max" to IDENTIFIER_ALLOWED_LENGTH.last.toString(),
	)
	if (
		(identifier.first() in DIGITS) || !identifier.all { c ->
			c in UPPERCASE_LETTERS || c in LOWERCASE_LETTERS || c == '_' || c in DIGITS
		}
	) {
		validationContext.addError(
			"GE-IDENTIFIER-CHARS",
			"value" to truncateValueForErrorMessage(identifier),
		)
	}
}

fun truncateValueForErrorMessage(value: String, maxLength: Int = 32): String =
	if (value.length > maxLength) {
		value.take(maxLength) + "..."
	} else {
		value
	}