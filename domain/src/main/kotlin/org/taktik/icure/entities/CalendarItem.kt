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
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.CalendarItemTag
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.FlowItem
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItem(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	@param:ContentValue(ContentValues.TIMESTAMP) override val endOfLife: Long? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:ContentValue(ContentValues.ANY_STRING) @NotNull val title: String? = null,
	val calendarItemTypeId: String? = null,
	val masterCalendarItemId: String? = null,
	@Deprecated("Use crypedForeignKeys instead") val patientId: String? = null,
	val important: Boolean? = null,
	val homeVisit: Boolean? = null,
	val phoneNumber: String? = null,
	val placeId: String? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITY) val address: Address? = null,
	val addressText: String? = null,
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val confirmationTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val cancellationTimestamp: Long? = null,
	val confirmationId: String? = null,
	val duration: Long? = null,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val allDay: Boolean? = null,
	val details: String? = null,
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
	 * Specify this calendar item should be considered by the algorithm calculating availabilities.
	 * Refer to the [Agenda] and [AvailabilitiesAssignmentStrategy] documentation for more information.
	 *
	 * For agendas created through the restricted endpoints this will be set to [AvailabilitiesAssignmentStrategy.Auto].
	 */
	@JsonInclude(JsonInclude.Include.NON_DEFAULT) val availabilitiesAssignmentStrategy: AvailabilitiesAssignmentStrategy = AvailabilitiesAssignmentStrategy.Auto,
	val hcpId: String? = null,
	val recurrenceId: String? = null,
	val meetingTags: Set<CalendarItemTag> = emptySet(),
	val flowItem: FlowItem? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadata? = null,
	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = null
) : StoredICureDocument, HasEncryptionMetadata, Encryptable {
	companion object : DynamicInitializer<CalendarItem>

	init {
		resourceGroup?.also { it.requireNormalized() }
	}

	/**
	 * Specify how this calendar item should affect availabilities.
	 */
	enum class AvailabilitiesAssignmentStrategy {
		/**
		 * If the calendar item is an off-schedule appointment (as defined in [Agenda]) use the [Loose] strategy,
		 * otherwise use the [Strict] strategy.
		 */
		Auto,
		/**
		 * Force the availability algorithm to use the strict strategy: this means that the calendar item will impact
		 * the availabilities during its time, and must be placeable exactly within the schedule together with all other
		 * calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy] or it will block availabilities.
		 *
		 * Off-schedule calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy] will block all
		 * availabilities for their entire duration.
		 */
		Strict,
		/**
		 * Force the availability algorithm to use the loose strategy: this means that during its time the calendar item
		 * will limit the availabilities for other calendar items with [Strict] [CalendarItem.availabilitiesAssignmentStrategy]
		 * and in the result, however, it won't black all availabilities if it can't be placed exactly within the
		 * schedule.
		 */
		Loose,
	}

	fun merge(other: CalendarItem) = CalendarItem(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: CalendarItem) = super<StoredICureDocument>.solveConflictsWith(other) + super<HasEncryptionMetadata>.solveConflictsWith(other) + super<Encryptable>.solveConflictsWith(other) + mapOf(
		"title" to (this.title ?: other.title),
		"calendarItemTypeId" to (this.calendarItemTypeId ?: other.calendarItemTypeId),
		"masterCalendarItemId" to (this.masterCalendarItemId ?: other.masterCalendarItemId),
		"patientId" to (this.patientId ?: other.patientId),
		"important" to (this.important ?: other.important),
		"homeVisit" to (this.homeVisit ?: other.homeVisit),
		"phoneNumber" to (this.phoneNumber ?: other.phoneNumber),
		"placeId" to (this.placeId ?: other.placeId),
		"address" to (this.address ?: other.address),
		"addressText" to (this.addressText ?: other.addressText),
		"startTime" to (this.startTime ?: other.startTime),
		"endTime" to (this.endTime ?: other.endTime),
		"confirmationTime" to (this.confirmationTime ?: other.confirmationTime),
		"confirmationId" to (this.confirmationId ?: other.confirmationId),
		"duration" to (this.duration ?: other.duration),
		"allDay" to (this.allDay ?: other.allDay),
		"details" to (this.details ?: other.details),
		"wasMigrated" to (this.wasMigrated ?: other.wasMigrated),
		"agendaId" to (this.agendaId ?: other.agendaId),
		"recurrenceId" to (this.recurrenceId ?: other.recurrenceId),
		"meetingTags" to (other.meetingTags + this.meetingTags),
		"flowItem" to (this.flowItem ?: other.flowItem)
	)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) =
		when {
			created != null && modified != null -> this.copy(created = created, modified = modified)
			created != null -> this.copy(created = created)
			modified != null -> this.copy(modified = modified)
			else -> this
		}
}
