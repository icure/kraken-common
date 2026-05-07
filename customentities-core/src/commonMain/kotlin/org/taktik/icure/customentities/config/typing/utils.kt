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
 * There are many properties which have some specific meaning in iCure:
 * - Standard metadata
 *   - "id",
 *   - "rev",
 *   - "created",
 *   - "modified",
 *   - "author",
 *   - "responsible",
 *   - "endOfLife",
 *   - "deletionDate",
 * - Encryptable entities metadata
 *   - "encryptedSelf",
 * - Root encryptable entities metadata
 *   - "secretForeignKeys",
 *   - "cryptedForeignKeys",
 *   - "delegations",
 *   - "encryptionKeys",
 *   - "securityMetadata",
 * - Entities with couchdb or object storage attachments metadata
 *   - "dataAttachments",
 *   - "deletedAttachments",
 *
 * Most of them can be safely used by the user thanks to the shadowing system, however some identifiers may not be
 * acceptable even with the shadowing system due to how they are used:
 * - `encryptedSelf` currently decryption at the SDK scans on objects recursively for "encrytpedSelf" - this is
 *   required because we don't know if a previous version of the user application was using different encrypted fields
 *   manifest.
 *
 * If in future we need to add more fields that should go here we might want to consider using _property as a name to
 * avoid clashing with custom properties (we don't allow identifiers starting with _ in custom entities)
 */
private val reservedIdentifiers = setOf(
	"encryptedSelf"
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