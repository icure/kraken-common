package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.appending

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EnumDefinition(
	val entries: Set<String>
) {
	fun validateDefinition(
		context: ScopedErrorCollector,
	) {
		if (entries.isEmpty()) context.addError("Invalid enum definition, at least one entry is required")
		context.appending(".entries") {
			entries.forEach { entry -> validateIdentifier(context, entry) }
		}
	}
}