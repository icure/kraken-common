/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedList<T : Serializable?>(
	val pageSize: Int = 0,
	val totalSize: Int = 0,
	val rows: List<T> = emptyList(),
	val nextKeyPair: PaginatedDocumentKeyIdPair<*>? = null
) : Serializable
