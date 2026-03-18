package org.taktik.icure.domain.customentities.util

import org.taktik.icure.errorreporting.ScopedErrorCollector

class CustomEntityConfigValidationContext(
	val resolution: CustomEntityConfigResolutionContext,
	val validation: ScopedErrorCollector,
	val builtinValidation: ExtendableBuiltinEntityValidator,
	val builtinDefinitions: BuiltinDefinitionsProvider,
)