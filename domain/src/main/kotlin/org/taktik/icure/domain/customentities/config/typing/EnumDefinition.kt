package org.taktik.icure.domain.customentities.config.typing

data class EnumDefinition(
	val name: String,
	val entries: Set<String>
) {
	fun validateDefinition() {
		// TODO place limit on size of entries?
		require(entries.isNotEmpty()) {
			"$name: invalid enum definition, at least one entry is required"
		}
	}
}