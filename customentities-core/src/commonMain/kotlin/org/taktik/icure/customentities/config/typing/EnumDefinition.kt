package org.taktik.icure.customentities.config.typing

import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.Include
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending

@JsonInclude(Include.NON_DEFAULT)
data class EnumDefinition(
	val entries: Set<String>
) {
	fun validateDefinition(
		context: ScopedErrorCollector,
	) {
		if (entries.isEmpty()) context.addError("GE-ENUM-EMPTY")
		context.appending(".entries") {
			entries.forEach { entry -> validateIdentifier(context, entry) }
		}
	}
}