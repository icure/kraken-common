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
	override val nullable: Boolean = false,
) : GenericTypeConfig {
	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean =
		other is BooleanTypeConfig && (if (other.nullable == this.nullable) this == other else this == other.copy(nullable = this.nullable))

	override fun validateAndMapValueForStore(
		resolutionContext: CustomEntityConfigResolutionContext,
		validationContext: ScopedErrorCollector,
		value: RawJson
	): RawJson =
		validatingNullForStore(
			validationContext,
			value,
			nullable
		) {
			if (value !is RawJson.JsonBoolean) validationContext.addError("GE-BOOL-JSON")
			value
		}
}