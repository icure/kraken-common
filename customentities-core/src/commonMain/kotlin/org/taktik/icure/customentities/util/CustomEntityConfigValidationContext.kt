package org.taktik.icure.customentities.util

import org.taktik.icure.errorreporting.ScopedErrorCollector

open class CustomEntityValueValidationContext(
	val resolution: CustomEntityConfigResolutionContext,
	val validation: ScopedErrorCollector,
	val builtinValidation: ExtendableBuiltinEntityValidator,
)

class CustomEntityConfigValidationContext(
	resolution: CustomEntityConfigResolutionContext,
	validation: ScopedErrorCollector,
	builtinValidation: ExtendableBuiltinEntityValidator,
	val builtinDefinitions: BuiltinDefinitionsProvider,
) : CustomEntityValueValidationContext(resolution, validation, builtinValidation)