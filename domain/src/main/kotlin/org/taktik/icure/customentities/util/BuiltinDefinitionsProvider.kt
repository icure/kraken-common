package org.taktik.icure.customentities.util

import org.taktik.icure.customentities.config.ExtendableEntityName
import org.taktik.icure.customentities.config.typing.EnumDefinition
import org.taktik.icure.customentities.config.typing.ObjectDefinition

interface BuiltinDefinitionsProvider {
	fun getBuiltinEnumDefinition(name: String): EnumDefinition?
	fun getBuiltinObjectDefinition(name: ExtendableEntityName): Map<String, ObjectDefinition.PropertyConfiguration>?
}