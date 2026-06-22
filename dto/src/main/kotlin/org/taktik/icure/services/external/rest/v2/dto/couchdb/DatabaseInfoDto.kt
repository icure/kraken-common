/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object containing information about a CouchDB database, including size metrics and cluster configuration.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.couchdb.DatabaseInfoDto")
data class DatabaseInfoDto(
	/** The database identifier. */
	@ActiveField val id: String,
	/** The current update sequence for the database. */
	@ActiveField val updateSeq: String? = null,
	/** The size of the database file on disk in bytes. */
	@ActiveField val fileSize: Long? = null,
	/** The uncompressed size of the database contents in bytes. */
	@ActiveField val externalSize: Long? = null,
	/** The size of live data in the database in bytes. */
	@ActiveField val activeSize: Long? = null,
	/** The number of documents in the database. */
	@ActiveField val docs: Long? = null,
	/** The number of shards for the database. */
	@ActiveField val q: Int? = null,
	/** The number of replicas of each shard. */
	@ActiveField val n: Int? = null,
	/** The number of copies that must be written before a write is considered successful. */
	@ActiveField val w: Int? = null,
	/** The number of copies that must be read before a read is considered successful. */
	@ActiveField val r: Int? = null,
) : Serializable
