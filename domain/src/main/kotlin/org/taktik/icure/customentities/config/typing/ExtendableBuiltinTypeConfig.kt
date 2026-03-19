package org.taktik.icure.customentities.config.typing

import org.taktik.icure.customentities.config.ExtendableEntityName
import org.taktik.icure.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson

data class ExtendableBuiltinTypeConfig(
	val typeName: ExtendableEntityName,
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