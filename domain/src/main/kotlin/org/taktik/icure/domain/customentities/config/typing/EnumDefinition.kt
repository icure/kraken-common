package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.domain.customentities.util.ResolutionPath

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class EnumDefinition(
	val entries: Set<String>
) {
	fun validateDefinition(
		path: ResolutionPath
	) {
		require(entries.isNotEmpty()) {
			"$path: invalid enum definition, at least one entry is required"
		}
		path.appending(".entries") {
			entries.forEach { entry -> validateIdentifier(path, entry) }
		}
	}
}