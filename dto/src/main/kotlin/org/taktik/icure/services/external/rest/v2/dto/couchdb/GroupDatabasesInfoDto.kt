/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object aggregating database information for all databases belonging to a specific group,
 * including their storage sizes and GCP storage usage.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.couchdb.GroupDatabasesInfoDto")
data class GroupDatabasesInfoDto(
	/** The identifier of the group. */
	@param:Schema(required = true)
	@ActiveField val groupId: String,
	/** The list of database information entries for this group. */
	@param:Schema(required = true)
	@ActiveField val databasesInfo: List<DatabaseInfoDto>,
	/** The total GCP storage size in bytes used by this group. */
	@param:Schema(required = true)
	@ActiveField val gcpStorageSize: Long,
) : Serializable
