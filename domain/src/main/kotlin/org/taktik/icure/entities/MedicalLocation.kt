/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.Named
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.RevisionInfo

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalLocation(
	/** The unique identifier of the medical location. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the medical location in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The name of the medical location. */
	override val name: String? = null,
	/** A description of the medical location. */
	val description: String? = null,
	/** The id of the healthcare party responsible for this medical location. */
	val responsible: String? = null,
	/** Whether this medical location is a guard post. */
	val guardPost: Boolean? = null,
	/** The CBE (Crossroads Bank for Enterprises) number of the medical location. */
	val cbe: String? = null,
	/** The Bank Identifier Code (BIC/SWIFT) of the medical location. */
	val bic: String? = null,
	/** The bank account number (IBAN) of the medical location. */
	val bankAccount: String? = null,
	/** The NIHII number of the medical location. */
	val nihii: String? = null,
	/** The social security inscription number associated with the medical location. */
	val ssin: String? = null,
	/** The address of the medical location. */
	val address: Address? = null,
	/** The set of agenda ids linked to this medical location. */
	val agendaIds: Set<String> = emptySet(),
	/** Additional options for the medical location. */
	val options: Map<String, String> = emptyMap(),
	/** Public information about the medical location, in multiple languages. */
	val publicInformations: Map<String, String> = emptyMap(),
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredDocument,
	Named {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
