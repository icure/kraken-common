package org.taktik.icure.domain.customentities.config

import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition

data class VersionedCustomEntitiesConfiguration(
	val id: String,
	val objects: Map<String, ObjectDefinition>,
	val enums: Map<String, EnumDefinition>,
	val extensions: ExtensionConfiguration,
	val published: Boolean,
) {
	init {
		require((objects.keys + enums.keys).size == (objects.size + enums.size)) {
			"Objects and enums must have unique names" // Not really needed now but leaves more freedom for future changes
		}
	}
}