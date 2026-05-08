package org.taktik.icure.customentities.util

import org.taktik.icure.errorreporting.ScopedErrorCollector

open class CustomEntityValueValidationContext(
	val resolution: CustomEntityConfigResolutionContext,
	val validation: ScopedErrorCollector,
	val builtinValidation: ExtendableBuiltinEntityValidator,
	/**
	 * True if the validation of this value should be done in a context where the entity is expected to be decrypted /
	 * not encrypted.
	 * If false the entity is expected to be encrypted
	 */
	val isDecryptedContext: Boolean,
)

class CustomEntityConfigValidationContext(
	resolution: CustomEntityConfigResolutionContext,
	validation: ScopedErrorCollector,
	builtinValidation: ExtendableBuiltinEntityValidator,
	val builtinDefinitions: BuiltinDefinitionsProvider,
	isDecryptedContext: Boolean,
) : CustomEntityValueValidationContext(resolution, validation, builtinValidation, isDecryptedContext)