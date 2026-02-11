/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

/**
 * Created by aduchate on 06/07/13, 10:09
 *
 * This entity represents a sub-contact. It is serialized in JSON and saved in the underlying icure-contact CouchDB database.
 *
 * A sub-contact represents the links between a series of services contained in a contact to other structuring elements such a sa healthcare element, healthcare approach, forms, etc.
 * A Contact conforms to a series of interfaces:
 * - StoredICureDocument
 * - Encryptable
 *
 * @property id The Id of the sub-contact. We encourage using either a v4 UUID or a HL7 Id.
 * @property created The timestamp (unix epoch in ms) of creation of the sub-contact, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of the latest modification of the sub-contact, will be filled automatically if missing. Not enforced by the application server.
 * @property author The id of the User that has created this sub-contact, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible The id of the HealthcareParty that is responsible for this sub-contact, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId The id of the medical location where the sub-contact was created.
 * @property tags Tags that qualify the sub-contact as being member of a certain class.
 * @property codes Codes that identify or qualify this particular sub-contact.
 * @property endOfLife Soft delete (unix epoch in ms) timestamp of the object.
 * @property descr Description of the sub-contact
 * @property protocol Protocol based on which the sub-contact was used for linking services to structuring elements
 * @property status
 * @property formId Id of the form used in the sub-contact. Several sub-contacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID
 * @property planOfActionId Id of the plan of action (healthcare approach) that is linked by the sub-contact to a service.
 * @property healthElementId Id of the healthcare element that is linked by the sub-contact to a service
 * @property classificationId
 * @property services List of all services provided to the patient under a given contact which is linked by this sub-contact to other structuring elements.
 * @property encryptedSelf The encrypted fields of this sub-contact.
 *
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubContact(
	@param:JsonProperty("_id") override val id: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	val descr: String? = null,
	val protocol: String? = null,
	val status: Int? = null, // To be refactored
	val formId: String? = null, // form or subform unique ID. Several subcontacts with the same form ID can coexist as long as they are in different contacts or they relate to a different planOfActionID
	val planOfActionId: String? = null,
	val healthElementId: String? = null,
	val classificationId: String? = null,
	val services: List<ServiceLink> = emptyList(),
	override val encryptedSelf: String? = null,
) : Encryptable,
	ICureDocument<String?> {
	companion object : DynamicInitializer<SubContact> {
		const val STATUS_LABO_RESULT = 1
		const val STATUS_UNREAD = 2
		const val STATUS_ALWAYS_DISPLAY = 4
		const val RESET_TO_DEFAULT_VALUES = 8
		const val STATUS_COMPLETE = 16
		const val STATUS_PROTOCOL_RESULT = 32
		const val STATUS_UPLOADED_FILES = 64
	}

	fun merge(other: SubContact) = SubContact(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: SubContact) = super<Encryptable>.solveConflictsWith(other) +
		super<ICureDocument>.solveConflictsWith(other) +
		mapOf(
			"descr" to (this.descr ?: other.descr),
			"protocol" to (this.protocol ?: other.protocol),
			"status" to (this.status ?: other.status),
			"formId" to (this.formId ?: other.formId),
			"planOfActionId" to (this.planOfActionId ?: other.planOfActionId),
			"healthElementId" to (this.healthElementId ?: other.healthElementId),
			"classificationId" to (this.classificationId ?: other.classificationId),
			"services" to MergeUtil.mergeListsDistinct(this.services, other.services, { a, b -> a.serviceId == b.serviceId }),
		)

	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
