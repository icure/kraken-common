/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.mergers.annotations.NonMergeable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Insurance(
	/** The unique identifier of the insurance. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the insurance in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The name of the insurance in different languages. */
	@NonMergeable val name: Map<String, String> = emptyMap(),
	/** Whether this is a private insurance. */
	val privateInsurance: Boolean = false,
	/** Whether this insurance covers hospitalisation. */
	val hospitalisationInsurance: Boolean = false,
	/** Whether this insurance covers ambulatory care. */
	val ambulatoryInsurance: Boolean = false,
	/** The insurance code. */
	val code: String? = null,
	/** The agreement number for the insurance. */
	val agreementNumber: String? = null,
	/** The id of the parent insurance entity. */
	val parent: String? = null, // ID of the parent
	/** The address of the insurance company. */
	val address: Address = Address(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

	) : StoredDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
