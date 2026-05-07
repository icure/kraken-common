package org.taktik.icure.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError

/**
 * If [value] is [RawJson.JsonNull] requires that [nullable] is true and returns [RawJson.JsonNull] (fails if nullable is false).
 * Else executes the block and returns its value
 */
internal inline fun validatingNullForStore(
	validationContext: ScopedErrorCollector,
	value: RawJson,
	nullable: Boolean,
	validateNonNull: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		if (!nullable) validationContext.addError("GE-NULL")
		value
	} else {
		validateNonNull()
	}

// Purpose on identifiers validation:
// - helps make sure that they can be used in the generated SDK Should probably also block all keywords of langauges we generate SDKs for.
// - size limit helps to reduce impact that cache has on memory consumption
private const val IDENTIFIER_ALLOWED_LENGTH_MIN = 1
private const val IDENTIFIER_ALLOWED_LENGTH_MAX = 32 // Chosen arbitrarily, might be increased later if needed
/**
 * For most user-provided identifiers it is not really a problem to have clashes, due to the shadowing system, so it is
 * mostly a way of discouraging usage of identifiers that the user might regret later when they start using features
 * that they didn't know.
 *
 * `encryptedSelf` however could be a serious problem, due to the way we are currently doing recursive decryption of
 * entities.
 */
private val reservedIdentifiers = setOf(
	// Standard metadata
	"id",
	"rev",
	"created",
	"modified",
	"author",
	"responsible",
	"endOfLife",
	"deletionDate",
	// Encryptable
	"encryptedSelf",
	// HasEncryptionMetadata (root encryptables)
	"secretForeignKeys",
	"cryptedForeignKeys",
	"delegations",
	"encryptionKeys",
	"securityMetadata",
	// HasDataAttachments (entity with couchdb or object storage attachments)
	"dataAttachments",
	"deletedAttachments",

)
fun validateIdentifier(validationContext: ScopedErrorCollector, identifier: String) {
	if (identifier in reservedIdentifiers) {
		validationContext.addError(
			"GE-IDENTIFIER-RESERVED",
			"value" to truncateValueForErrorMessage(identifier)
		)
	}
	if (identifier.length !in IDENTIFIER_ALLOWED_LENGTH_MIN..IDENTIFIER_ALLOWED_LENGTH_MAX) validationContext.addError(
		"GE-IDENTIFIER-LENGTH",
		"value" to truncateValueForErrorMessage(identifier, IDENTIFIER_ALLOWED_LENGTH_MAX),
		"min" to IDENTIFIER_ALLOWED_LENGTH_MIN,
		"max" to IDENTIFIER_ALLOWED_LENGTH_MAX,
	)
	if (
		identifier.first() in '0' .. '9' || identifier.first() == '_' || !identifier.all { c ->
			c in 'A'..'Z' || c in 'a'..'z' || c == '_' || c in '0'..'9'
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