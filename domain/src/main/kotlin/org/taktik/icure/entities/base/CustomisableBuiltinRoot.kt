package org.taktik.icure.entities.base

/**
 * A builtin root entity that can be customised through the builtin entity extensions of a custom entities
 * configuration (see `StandardRootEntitiesExtensionConfig`).
 */
interface CustomisableBuiltinRoot : CustomisableRoot {
	/**
	 * Id of the customised type of this entity: a key of the extensions configured for this builtin entity type, or
	 * null for unextended entities. It is null if and only if [customisedModelVersion] is null.
	 *
	 * Unlike the type of custom entities, the customised type of a builtin entity can change, as long as the new type
	 * is enabled.
	 */
	val customisedTypeId: String?
}
