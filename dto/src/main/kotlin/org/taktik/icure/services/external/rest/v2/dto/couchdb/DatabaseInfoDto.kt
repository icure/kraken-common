/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseInfoDto(
	val id: String,
	val updateSeq: String? = null,
	val fileSize: Long? = null,
	val externalSize: Long? = null,
	val activeSize: Long? = null,
	val docs: Long? = null,
	val q: Int? = null,
	val n: Int? = null,
	val w: Int? = null,
	val r: Int? = null,
) : Serializable
