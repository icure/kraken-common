/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.dto.annotations.filtering.NonEmpty
import org.taktik.icure.dto.annotations.filtering.Omit
import org.taktik.icure.dto.annotations.filtering.SerializationPolicy
import org.taktik.icure.dto.annotations.filtering.Since

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object aggregating database information for all databases belonging to a specific group,
 * including their storage sizes and GCP storage usage.
 */
data class GroupDatabasesInfoDto(
	/** The identifier of the group. */
	@param:Schema(required = true)
	@ActiveField
	val groupId: String,
	/** The list of database information entries for this group. */
	@param:Schema(required = true)
	@ActiveField
	val databasesInfo: List<DatabaseInfoDto>,
	/** The total GCP storage size in bytes used by this group. */
	@param:Schema(required = true)
	@ActiveField
	val objectStorageSize: Long,
	@SerializationPolicy(
		Since("2.0.0", Omit::class),
		Since("3.0.0", NonEmpty::class)
	)
	val objectStorageInfo: Map<String, ObjectStorageInfoDto> = emptyMap(),
) : Serializable {
	data class ObjectStorageInfoDto(val size: Long, val regions: Set<String>)
}
