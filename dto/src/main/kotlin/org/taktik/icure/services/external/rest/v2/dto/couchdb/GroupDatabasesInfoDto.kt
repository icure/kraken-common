/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class GroupDatabasesInfoDto(
	@get:Schema(required = true)
	val groupId: String,
	@get:Schema(required = true)
	val databasesInfo: List<DatabaseInfoDto>,
	@get:Schema(required = true)
	val gcpStorageSize: Long,
) : Serializable
