/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.CalendarItemTag
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.FlowItem
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class CalendarItem(
	/** The Id of the calendar item. We encourage using either a v4 UUID or a HL7 Id. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the calendar item in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this calendar item. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the data owner that is responsible for this calendar item. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** The medical location where this entity was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the calendar item as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular calendar item. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	/** The title of the calendar item. */
	@field:NotNull val title: String? = null,
	/** The id of the calendar item type associated with this item. */
	val calendarItemTypeId: String? = null,
	/** The id of the master calendar item if this is a recurring instance. */
	val masterCalendarItemId: String? = null,
	/** The patient id. Deprecated: use cryptedForeignKeys instead. */
	@Deprecated("Use crypedForeignKeys instead") val patientId: String? = null,
	/** Whether this calendar item is marked as important. */
	val important: Boolean? = null,
	/** Whether this calendar item represents a home visit. */
	val homeVisit: Boolean? = null,
	/** A phone number associated with this calendar item. */
	val phoneNumber: String? = null,
	/** The id of the place where the appointment takes place. */
	val placeId: String? = null,
	/** The address where the appointment takes place. */
	val address: Address? = null,
	/** The textual representation of the address. */
	val addressText: String? = null,
	/** The start time of the calendar item in YYYYMMDDHHMMSS format. */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The end time of the calendar item in YYYYMMDDHHMMSS format. */
	val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The time of confirmation in YYYYMMDDHHMMSS format. */
	val confirmationTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The timestamp of cancellation. */
	val cancellationTimestamp: Long? = null,
	/** An id associated with the confirmation. */
	val confirmationId: String? = null,
	/** The duration of the appointment. */
	val duration: Long? = null,
	/** Whether this calendar item spans the entire day. */
	val allDay: Boolean? = null,
	/** Additional details about the calendar item. */
	val details: String? = null,
	/** Whether this calendar item was migrated from another system. */
	val wasMigrated: Boolean? = null,
	/**
	 * Id of the agenda linked to this CalendarItem.
	 * This calendar item will block the availabilities of that agenda.
	 */
	val agendaId: String? = null,
	/**
	 * The resource group of the agenda specified by [agendaId] that will handle the calendar item.
	 * Refer to the [Agenda] documentation for more information.
	 *
	 * Note that if the resource group doesn't exist in the agenda this calendar item will be ignored when calculating
	 * availabilities.
	 *
	 * Can only be specified when creating the calendar item through the unrestricted endpoints (accessible only to
	 * privileged users).
	 */
	val resourceGroup: CodeStub? = null,
	/**
	 * Specify how this calendar item should be considered by the algorithm calculating availabilities.
	 * Refer to the [Agenda] and [AvailabilitiesAssignmentStrategy] documentation for more information.
	 *
	 * For agendas created through the restricted endpoints this will be set to null.
	 */
	val availabilitiesAssignmentStrategy: AvailabilitiesAssignmentStrategy? = null,
	/**
	 * A value used only internally by iCure in the tentative calendar items, to mark the time when they were created.
	 * Used by the tentative calendar item cleanup process to chek if a calendar item should be cleaned up or not.
	 */
	val tentativeTimestamp: Long? = null,
	/** The healthcare party id associated with this calendar item. */
	val hcpId: String? = null,
	/** An id for linking recurring calendar items. */
	val recurrenceId: String? = null,
	/** Tags for the meeting associated with this calendar item. */
	val meetingTags: Set<CalendarItemTag> = emptySet(),
	/** Flow item information associated with this calendar item. */
	val flowItem: FlowItem? = null,
	/** Custom properties of this calendar item. */
	val properties: Set<PropertyStub> = emptySet(),
	/** The secret foreign keys, used for secure linking to patients. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys. */
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	/** The encryption keys used to encrypt secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	/** The base64-encoded encrypted fields of this calendar item. */
	override val encryptedSelf: String? = null,
	/** The security metadata of this entity, for access control. */
	override val securityMetadata: SecurityMetadata? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredICureDocument,
	HasEncryptionMetadata,
	Encryptable {

	init {
		resourceGroup?.also { it.requireNormalized() }
	}

	/**
	 * Specify how this calendar item should affect availabilities.
	 *
	 * If null and the calendar item is an off-schedule appointment (as defined in [Agenda]) use the [Loose] strategy,
	 * otherwise use the [Strict] strategy.
	 */
	enum class AvailabilitiesAssignmentStrategy {
		/**
		 * Force the availability algorithm to use the strict strategy: this means that the calendar item will impact
		 * the availabilities during its time, and must be placeable exactly within the schedule together with all other
		 * calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy] or it will block availabilities.
		 *
		 * Off-schedule calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy] will block all
		 * availabilities for their entire duration.
		 */
		@JsonProperty("S")
		Strict,

		/**
		 * Force the availability algorithm to use the loose strategy: this means that during its time the calendar item
		 * will limit the availabilities for other calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy]
		 * and in the result, however, it won't black all availabilities if it can't be placed exactly within the
		 * schedule.
		 */
		@JsonProperty("L")
		Loose,
	}

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
	override fun withEncryptionMetadata(
		secretForeignKeys: Set<String>,
		cryptedForeignKeys: Map<String, Set<Delegation>>,
		delegations: Map<String, Set<Delegation>>,
		encryptionKeys: Map<String, Set<Delegation>>,
		securityMetadata: SecurityMetadata?
	) = copy(
		secretForeignKeys = secretForeignKeys,
		cryptedForeignKeys = cryptedForeignKeys,
		delegations = delegations,
		encryptionKeys = encryptionKeys,
		securityMetadata = securityMetadata
	)
}
