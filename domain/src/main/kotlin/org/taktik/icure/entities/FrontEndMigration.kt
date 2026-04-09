/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.FrontEndMigrationStatus
import org.taktik.icure.entities.embed.RevisionInfo

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FrontEndMigration(
	/** The unique identifier of the front-end migration. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the front-end migration in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The name of the migration. */
	val name: String? = null,
	/** The start date (unix epoch in ms) of the migration. */
	val startDate: Long? = null,
	/** The end date (unix epoch in ms) of the migration. */
	val endDate: Long? = null,
	/** The current status of the migration. */
	val status: FrontEndMigrationStatus? = null,
	/** Logs produced during the migration process. */
	val logs: String? = null,
	/** The id of the user that initiated the migration. */
	val userId: String? = null,
	/** The start key used for pagination during migration. */
	val startKey: String? = null,
	/** The start key document id used for pagination during migration. */
	val startKeyDocId: String? = null,
	/** The number of items processed during the migration. */
	val processCount: Long? = null,
	/** Extra properties for the front-end migration. Those properties are typed (see class Property). */
	val properties: Set<PropertyStub> = emptySet(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
