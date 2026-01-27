package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.errorreporting.ScopedErrorCollector

/**
 * Represents a configuration for a boolean type.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class BooleanTypeConfig(
	val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson =
		validatingAndIgnoringNullForStore(
			validationContext,
			value,
			nullable
		) {
			if (value !is RawJson.JsonBoolean) validationContext.addError("GE-BOOL-JSON", emptyMap())
			value
		}
}