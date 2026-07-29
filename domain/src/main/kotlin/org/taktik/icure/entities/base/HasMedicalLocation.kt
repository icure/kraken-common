/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

/**
 * Marks an entity as being associated with a medical location.
 *
 * @property medicalLocationId the medical location where this entity has been created.
 */
interface HasMedicalLocation {
	val medicalLocationId: String?
}
