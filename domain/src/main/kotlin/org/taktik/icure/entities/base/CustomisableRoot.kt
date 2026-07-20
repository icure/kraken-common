/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

// Currently, all entities with configurable encryption also have extensions fields but it might change in future
/**
 * An interface for root entities that can be customized with additional custom fields or with configurable encryption.
 */
interface CustomisableRoot {
	val customisedModelVersion: Int?
}
