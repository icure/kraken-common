package org.taktik.icure.domain.customentities.config

/**
 * All extendable builtin entities.
 */
enum class ExtendableEntityName(
	/**
	 * If the entity is a root entity
	 */
	val isRootEntity: Boolean = false
) {
	Address, // org.taktik.icure.entities.embed.Address
	Patient(isRootEntity = true); // org.taktik.icure.entities.Patient
}