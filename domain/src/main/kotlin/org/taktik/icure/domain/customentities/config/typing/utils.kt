package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * If [value] is [RawJson.JsonNull] requires that [nullable] is true and returns [RawJson.JsonNull] (fails if nullable is false).
 * Else executes the block and returns its value
 */
internal inline fun validatingAndIgnoringNullForStore(
	path: ResolutionPath,
	value: RawJson,
	nullable: Boolean,
	block: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		require(nullable) {
			"$path: invalid value `null`"
		}
		value
	} else {
		block()
	}

internal inline fun validatingAndIgnoringNullForRead(
	path: ResolutionPath,
	value: RawJson,
	nullable: Boolean,
	block: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		if (!nullable) throw IllegalEntityException("$path: invalid value `null`")
		value
	} else {
		block()
	}

private val UPPERCASE_LETTERS = 'A'..'Z'
private val LOWERCASE_LETTERS = 'a'..'z'
private val DIGITS = '0'..'9'
private val IDENTIFIER_ALLOWED_LENGTH = 1..32 // Chosen arbitrarily, might be increased later if needed
fun validateIdentifier(path: ResolutionPath, identifier: String) {
	require(identifier.length in IDENTIFIER_ALLOWED_LENGTH) {
		"${path}: invalid identifier `${
			identifier.take(IDENTIFIER_ALLOWED_LENGTH.last)
		}`${
			if (identifier.length > IDENTIFIER_ALLOWED_LENGTH.last) "..." else ""
		} length must be in ${IDENTIFIER_ALLOWED_LENGTH.first}-${IDENTIFIER_ALLOWED_LENGTH.last}"
	}
	require(identifier.all { c ->
		c in UPPERCASE_LETTERS || c in LOWERCASE_LETTERS || c in DIGITS || c == '_'
	}) {
		"${path}: invalid identifier `$identifier`, can only contain alphanumeric characters or underscores"
	}
}