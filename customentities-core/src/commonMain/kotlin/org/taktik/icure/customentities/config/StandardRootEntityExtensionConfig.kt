package org.taktik.icure.customentities.config

import kotlinx.serialization.Serializable
import org.taktik.icure.jackson.annotations.JsonInclude
import org.taktik.icure.jackson.annotations.JsonIncludeValue
import org.taktik.icure.customentities.config.typing.ObjectDefinition

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
@Serializable
data class StandardRootEntityExtensionConfig(
	/**
	 * Reference to the object definition (i.e. key of [VersionedCustomEntitiesConfiguration.objects]) for this entity
	 * extensions.
	 * This value is also used as the name for the type in the generated custom SDK.
	 * The referenced object definition must have a [ObjectDefinition.builtinExtension] that is equal to the root
	 * builtin entity being extended.
	 */
	val objectDefinitionReference: String,
	/**
	 * Version of the configuration for this specific entity.
	 * If an update to a [VersionedCustomEntitiesConfiguration] changes nothing in the definitions of this entity
	 * extension, and on types this extension depends on, this version should be kept the same, else it must be
	 * incremented.
	 */
	val version: Int,
)

@JsonInclude(JsonIncludeValue.NON_DEFAULT)
@Serializable
data class StandardRootEntitiesExtensionConfig(
	override val accessLog: StandardRootEntityExtensionConfig? = null,
	override val agenda: StandardRootEntityExtensionConfig? = null,
	override val calendarItem: StandardRootEntityExtensionConfig? = null,
	override val calendarItemType: StandardRootEntityExtensionConfig? = null,
	override val contact: StandardRootEntityExtensionConfig? = null,
	override val device: StandardRootEntityExtensionConfig? = null,
	override val document: StandardRootEntityExtensionConfig? = null,
	override val healthcareParty: StandardRootEntityExtensionConfig? = null,
	override val healthElement: StandardRootEntityExtensionConfig? = null,
	override val message: StandardRootEntityExtensionConfig? = null,
	override val patient: StandardRootEntityExtensionConfig? = null,
	override val place: StandardRootEntityExtensionConfig? = null,
	override val user: StandardRootEntityExtensionConfig? = null,
	override val topic: StandardRootEntityExtensionConfig? = null,
) : ExtendableRootEntitiesConfiguration<StandardRootEntityExtensionConfig>