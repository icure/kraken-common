/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Annotation
import org.taktik.icure.entities.embed.CareTeamMember
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.Episode
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Laterality
import org.taktik.icure.entities.embed.PlanOfAction
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * A Healthcare Element
 *
 * This entity is a root level object. It represents a healthcare element. It is serialized in JSON and saved in the underlying CouchDB database.
 *
 * A healthcare element is a patient centric representation of a healthcare problem.
 * The healthcare element embeds the patient's view of their own health problem. It evolves over time in its name/label and characteristics.
 * For example, a healthcare element that had been initially labelled “headache” by a patient, might evolve into a “migraine” after a consultation with a physician.
 * The new and the old version of the healthcare element share the same healthElementId if they represent an evolution of the same problem.
 *
 * A healthcare element is a central element in the organisation of the electronic health record. All sorts of services (biometrics, technical acts, diagnosis, lab requests, results, ...) will be linked to it.
 * Healthcare elements are used to filter the data inside a medical file in a meaningful way.
 *
 * A Healthcare Element conforms to a series of interfaces:
 *
 * - StoredICureDocument
 * - Encryptable
 *
 * @property id The Id of the healthcare element. We encourage using either a v4 UUID or a HL7 Id.
 * @property rev The revision of the patient in the database, used for conflict management / optimistic locking.
 * @property healthElementId The logical id of the healthcare element, used to link together different versions of the same healthcare element. We encourage using either a v4 UUID or a HL7 Id.
 * @property created The timestamp (unix epoch in ms) of creation of the healthcare element, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of latest modification of the healthcare element, will be filled automatically if missing. Not enforced by the application server.
 * @property author The id of the User that has created this healthcare element, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible The id of the HealthcareParty that is responsible for this healthcare element, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId The id of the medical location where the healthcare element is recorded
 * @property tags Tags that qualify the healthcare element as a member of a certain class.
 * @property codes Codes that identify or qualify this particular healthcare element.
 * @property endOfLife Soft delete (unix epoch in ms) timestamp of the object.
 * @property deletionDate Hard delete (unix epoch in ms) timestamp of the object.
 * @property valueDate The date (unix epoch in ms) when the healthcare element is noted to have started and also closes on the same date
 * @property openingDate The date (unix epoch in ms) of the start of the healthcare element.
 * @property closingDate The date (unix epoch in ms) marking the end of the healthcare element.
 * @property descr Description of the healthcare element.
 * @property note A text note (can be confidential, encrypted by default).
 * @property relevant If the healthcare element is relevant or not (Set relevant by default).
 * @property idOpeningContact Id of the opening contact when the healthcare element was created.
 * @property idClosingContact Id of the closing contact for the healthcare element.
 * @property idService Id of the service when a service is used to create a healthcare element.
 * @property status bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
 * @property laterality Left or Right dominance/preference.
 * @property plansOfAction List of healthcare approaches.
 * @property episodes List of episodes of occurrences of the healthcare element.
 * @property careTeam List of care team members assigned for the healthcare element.
 * @property delegations The delegations giving access to all connected healthcare information.
 * @property encryptionKeys The patient secret encryption key used to encrypt the secured properties (like note for example), encrypted for separate Crypto Actors.
 * @property encryptedSelf The encrypted fields of this healthcare element.
 *
 */

data class HealthElement(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val identifiers: List<Identifier> = emptyList(),
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	@field:NotNull(autoFix = AutoFix.UUID) val healthElementId: String? = null, // Several versions of the same healthcare element share the same healthElementId while having different ids
	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val valueDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val closingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@param:ContentValue(ContentValues.ANY_STRING) val descr: String? = null,
	val note: String? = null,
	val notes: List<Annotation> = emptyList(),
	val relevant: Boolean = true,
	val idOpeningContact: String? = null,
	val idClosingContact: String? = null,
	val idService: String? = null, // When a service is used to create the healthElement
	val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	val laterality: Laterality? = null,
	@field:Valid val plansOfAction: List<PlanOfAction> = emptyList(),
	@field:Valid val episodes: List<Episode> = emptyList(),
	@field:Valid val careTeam: List<CareTeamMember> = emptyList(),

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,

) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {
	companion object : DynamicInitializer<HealthElement>

	fun merge(other: HealthElement) = HealthElement(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: HealthElement) = super<StoredICureDocument>.solveConflictsWith(other) +
		super<HasEncryptionMetadata>.solveConflictsWith(other) +
		super<Encryptable>.solveConflictsWith(other) +
		mapOf(
			"identifiers" to mergeListsDistinct(
				this.identifiers,
				other.identifiers,
				{ a, b -> a.system == b.system && a.value == b.value },
			),
			"healthElementId" to (this.healthElementId ?: other.healthElementId),
			"valueDate" to (valueDate?.coerceAtMost(other.valueDate ?: Long.MAX_VALUE) ?: other.valueDate),
			"openingDate" to (openingDate?.coerceAtMost(other.openingDate ?: Long.MAX_VALUE) ?: other.openingDate),
			"closingDate" to (closingDate?.coerceAtLeast(other.closingDate ?: 0L) ?: other.closingDate),
			"descr" to (this.descr ?: other.descr),
			"note" to (this.note ?: other.note),
			"relevant" to (this.relevant),
			"idOpeningContact" to (this.idOpeningContact ?: other.idOpeningContact),
			"idClosingContact" to (this.idClosingContact ?: other.idClosingContact),
			"idService" to (this.idService ?: other.idService),
			"status" to (this.status),
			"laterality" to (this.laterality ?: other.laterality),
			"plansOfAction" to mergeListsDistinct(this.plansOfAction, other.plansOfAction, { a, b -> a.id == b.id }, { a, b -> a.merge(b) }),
			"episodes" to mergeListsDistinct(this.episodes, other.episodes, { a, b -> a.id == b.id }, { a, b -> a.merge(b) }),
			"careTeam" to mergeListsDistinct(this.careTeam, other.careTeam, { a, b -> a.id == b.id }, { a, b -> a.merge(b) }),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
