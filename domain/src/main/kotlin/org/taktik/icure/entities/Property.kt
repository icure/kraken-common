/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.TypedValue

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Property(
	/** The unique identifier of the property. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision identifier for optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The soft-delete timestamp in epoch milliseconds. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The type definition of this property. */
	val type: PropertyType? = null,
	/** The typed value held by this property. */
	val typedValue: TypedValue? = null,
	/** The encrypted content of this property, encoded as a Base64 string. */
	override val encryptedSelf: String? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredDocument,
	Encryptable {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
