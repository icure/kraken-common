/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.couchdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object containing information about a CouchDB database, including size metrics and cluster configuration.
 */
data class DatabaseInfoDto(
	/** The database identifier. */
	val id: String,
	/** The current update sequence for the database. */
	val updateSeq: String? = null,
	/** The size of the database file on disk in bytes. */
	val fileSize: Long? = null,
	/** The uncompressed size of the database contents in bytes. */
	val externalSize: Long? = null,
	/** The size of live data in the database in bytes. */
	val activeSize: Long? = null,
	/** The number of documents in the database. */
	val docs: Long? = null,
	/** The number of shards for the database. */
	val q: Int? = null,
	/** The number of replicas of each shard. */
	val n: Int? = null,
	/** The number of copies that must be written before a write is considered successful. */
	val w: Int? = null,
	/** The number of copies that must be read before a read is considered successful. */
	val r: Int? = null,
) : Serializable
