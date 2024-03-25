/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import java.io.Serializable

data class IndexingInfoDto(
	val statuses: Map<String, Int>?
): Serializable
