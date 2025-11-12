package org.taktik.icure.domain.customentities.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo

data class VersionedCustomEntitiesConfiguration(
	val rev: String? = null,
	val owningGroup: String,
	val version: Int,
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