package org.taktik.icure.domain.customentities.util

import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition

interface BuiltinDefinitionsProvider {
	fun getBuiltinEnumDefinition(name: String): EnumDefinition?
	fun getBuiltinObjectDefinition(name: ExtendableEntityName): Map<String, ObjectDefinition.PropertyConfiguration>?
}