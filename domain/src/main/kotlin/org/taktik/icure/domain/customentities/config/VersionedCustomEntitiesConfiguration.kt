package org.taktik.icure.domain.customentities.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.config.typing.validateIdentifier
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
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
	suspend fun validateDefinition() {
		val path = ResolutionPath()
		val context = CustomEntityConfigResolutionContext.ofConfig(this)
		path.appending("objects") {
			objects.forEach { (name, objDef) ->
				validateIdentifier(path, name)
				path.appending(".", name) {
					objDef.validateDefinition(context, path)
				}
			}
		}
		path.appending("enums") {
			enums.forEach { (name, objDef) ->
				validateIdentifier(path, name)
				path.appending(".", name) {
					objDef.validateDefinition(path)
				}
			}
		}
	}
}