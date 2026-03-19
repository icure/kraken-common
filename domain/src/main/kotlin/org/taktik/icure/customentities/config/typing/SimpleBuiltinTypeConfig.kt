package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.errorreporting.ScopedErrorCollector

data class SimpleBuiltinTypeConfig(
	val typeName: String,
	override val nullable: Boolean,
) : GenericTypeConfig {
	override fun validateAndMapValueForStore(
		context: CustomEntityConfigValidationContext,
		value: RawJson
	): RawJson {
		TODO("Not yet implemented")
	}

	override fun equalsIgnoringNullability(other: GenericTypeConfig): Boolean {
		TODO("Not yet implemented")
	}
}