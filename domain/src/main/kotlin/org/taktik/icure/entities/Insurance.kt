/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Insurance(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@JsonProperty("_rev") override val rev: String? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,

	val name: Map<String, String> = emptyMap(),
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val privateInsurance: Boolean = false,
	val hospitalisationInsurance: Boolean = false,
	val ambulatoryInsurance: Boolean = false,
	val code: String? = null,
	val agreementNumber: String? = null,
	val parent: String? = null, //ID of the parent
	@param:ContentValue(ContentValues.NESTED_ENTITY) val address: Address = Address(),

	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
	@JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = emptyMap()

) : StoredDocument {
	companion object : DynamicInitializer<Insurance>

	fun merge(other: Insurance) = Insurance(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Insurance) = super.solveConflictsWith(other) + mapOf(
		"privateInsurance" to (this.privateInsurance),
		"hospitalisationInsurance" to (this.hospitalisationInsurance),
		"ambulatoryInsurance" to (this.ambulatoryInsurance),
		"code" to (this.code ?: other.code),
		"agreementNumber" to (this.agreementNumber ?: other.agreementNumber),
		"parent" to (this.parent ?: other.parent),
		"address" to (this.address.merge(other.address)),
		"name" to (other.name + this.name)
	)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
