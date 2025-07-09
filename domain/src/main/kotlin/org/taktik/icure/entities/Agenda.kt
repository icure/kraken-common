/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.AgendaSlottingAlgorithm
import org.taktik.icure.entities.embed.EmbeddedTimeTableItem
import org.taktik.icure.entities.embed.ResourceGroupAllocationSchedule
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.Right
import org.taktik.icure.entities.embed.UserAccessLevel
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.time.DateTimeException
import java.time.ZoneId
import kotlin.collections.isNotEmpty

/**
 * An agenda allows keeping track of appointments (calendar items) for a resource (usually a doctor or other hcp) or
 * group of resources.
 *
 * # Schedule
 *
 * Normally, an agenda specifies a schedule for the resources, which is done through the
 * [ResourceGroupAllocationSchedule] in [schedules].
 * This allows users with limited privileges to take appointment in the schedule of the agenda using the availabilities
 * and safe booking features.
 * Users with special privileges are allowed to take appointments outside the agenda schedule.
 *
 * An agenda can have multiple entries in [schedules] to represent the following scenarios:
 * - The schedule of the agenda will change on a certain date
 * - There are multiple subgroups of resources that have partially overlapping responsibilities
 *
 * ## Representing changes to schedule
 *
 * If the agenda changes schedule at some point in time, for example, due to seasonal changes or internal
 * reorganization, you can create a new schedule for the same [ResourceGroupAllocationSchedule.resourceGroup]
 * and set the [ResourceGroupAllocationSchedule.startDateTime] of the new schedule and
 * [ResourceGroupAllocationSchedule.endDateTime] of the existing schedule to the date of the change.
 *
 * Note: different schedules in the same agenda for the same [ResourceGroupAllocationSchedule.resourceGroup] can't overlap.
 *
 * Note: to present differences in daily hours of operation you should use multiple [ResourceGroupAllocationSchedule.items]
 * with appropriate [EmbeddedTimeTableItem.rrule].
 *
 * ## Multiple groups of resources
 *
 * Sometimes you can have multiple groups of resources that have overlaps between their responsibilities and/or
 * capabilities.
 *
 * For example, you could have a group of senior and junior workers: the junior workers are capable of handling only the
 * easy tasks (calendar items), while the senior workers are capable of handling both easy and hard tasks.
 *
 * If you create separate agendas for the two types of workers, you will have one of two cases:
 * - You never allow users to take simple appointments with the senior workers, even if they are available but no junior
 * worker is available.
 * - You always allow users to take simple appointment with senior workers, even if the juniors are available. This will
 * limit the amount of hard appointments that can be taken.
 *
 * If instead, you use a single agenda with both groups of junior and senior workers the users will be able to take
 * appointments with more flexibility:
 * - as long as the existing appointments can be organized in a way that leaves a senior worker free, then users will be
 * able to book both simple and hard appointments;
 * - as long as the existing appointments can be organized in a way that leaves a junior worker free, then users will be
 * able to book simple appointments
 *
 * For example, consider that at time `t` there are 1 simple and 1 hard appointment booked, and there are 2 junior and
 * 2 senior workers on shift:
 * - A user will be able to book either a simple or a hard appointment.
 * - If an additional hard appointment is booked then the only availability left is for a simple appointment
 * - If an additional simple appointment is booked then the availability left can be for either a simple or hard
 *   appointment. If a last simple appointment is booked, then one of the 2 senior workers will do it.
 *
 * Note: the groups will need to have different [ResourceGroupAllocationSchedule.resourceGroup] (you can't have
 * schedules active in parallel for the same [ResourceGroupAllocationSchedule.resourceGroup])
 *
 * ### Multiple groups vs multiple items in single group
 *
 * You may think the following agendas are equivalent:
 * - Agenda 1
 *   - Group 1
 *     - TimeTableItem - 09:00->14:00 - Calendar item types: t1, t2
 *     - TimeTableItem - 13:00->18:00 - Calendar item types: t1, t3
 *
 * - Agenda 2
 *   - Group 1
 *     - TimeTableItem - 09:00->14:00 - Calendar item types: t1, t2
 *   - Group 2
 *     - TimeTableItem - 13:00->18:00 - Calendar item types: t1, t3
 *
 * That is, however, not the case.
 *
 * Agenda 1 defines a single group of resources that all have the same capabilities (in case of workers these could be
 * competences). Even though the resources could allow for an appointment of type t3 at 09:00, for some reason we don't
 * want people to take this kind of appointment at that time.
 *
 * In agenda 2, instead, the groups of resources have distinct capabilities. There is no resource available at 09:00 that
 * could handle an appointment of type t3.
 *
 * As long as all calendar items adhere to what is defined in the schedule there is actually going to be no real
 * difference, but in the real world sometimes there will be an appointment that doesn't really fit the schedule:
 * users with appropriate permission can forcefully create calendar items outside of this schedule.
 *
 * In these cases the availabilities algorithm will attempt to still find a decent solution, but if your representation
 * of the agenda doesn't properly match the resources the solution found could allow for users to take appointments
 * that can't be actually satisfied.
 *
 * As a general rule use separate [ResourceGroupAllocationSchedule] for groups of resources that don't share exactly
 * the same capabilities.
 *
 * TODO example once actually implemented
 *
 * TODO explain how to assign directly to group of resources to help.
 *
 * ## When to use multiple agendas
 *
 * You should use different agendas in all cases not covered by the previous situations. This includes:
 * - You have groups of workers that have no overlapping responsibilities
 * - You have groups of workers that have no overlapping working hours
 * - You want to let the end-user pick the resource that will handle the appointment (e.g. doctor, or clinic location)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Agenda(
	@JsonProperty("_id") override val id: String,
	@JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@JsonProperty("deleted") override val deletionDate: Long? = null,
	val name: String? = null,
	val userId: String? = null,
	@Deprecated("Use `userRights` instead") val rights: List<Right> = emptyList(),
	/**
	 * Associates a user id to the permission that user has on the entity.
	 */
	val userRights: Map<String, UserAccessLevel> = emptyMap(),
	val schedules: List<ResourceGroupAllocationSchedule> = emptyList(),
	/**
	 * Custom properties of the agenda. Public on public agenda.
	 */
	val properties: Set<PropertyStub> = emptySet(),
	/**
	 * If not null prevents unprivileged users from canceling or moving the calendar items linked to this agenda
	 * item less than [lockCalendarItemsBeforeInMinutes] minutes before its scheduled time.
	 */
	val lockCalendarItemsBeforeInMinutes: Int? = null,
	/**
	 * An identifier for the zone of the agenda. Must be an id accepted by java's ZoneId.
	 * Mandatory if the agenda specifies any time-based constraint:
	 * - [lockCalendarItemsBeforeInMinutes]
	 * - A nested timetable has an item with non-null [EmbeddedTimeTableItem.notAfterInMinutes]
	 * - A nested timetable has an item with non-null [EmbeddedTimeTableItem.notBeforeInMinutes]
	 */
	val zoneId: String? = null,
	/**
	 * A value used by the appointment availabilities and safe booking features, usually unnecessary but may be required
	 * for some complex agendas.
	 * The value should be a fuzzy time in HHMMSS format
	 *
	 * # Motivation
	 *
	 * When using the appointment availabilities, and safe booking features, iCure requires that the combined working
	 * hours of all timetables and timetable items can be split in blocks of at most 24 hours.
	 *
	 * For example, an agenda with timetable items that give the following 3 working hours:
	 * - Monday from 12:00 to 22:00
	 * - Monday from 20:00 to Tuesday 6:00
	 * - Tuesday from 4:00 to 14:00
	 * is not accepted as it creates a contiguous block from 12:00 of Monday to 14:00 of Tuesday (26 hours).
	 * It would be acceptable instead to have the following working:
	 * - Monday from 12:00 to 22:00
	 * - Monday from 20:00 to Tuesday 5:00
	 * - Tuesday from 5:00 to 14:00
	 * As we have two blocks (M12-T05, T05-T14) each less than 24 hours long
	 *
	 * If in your agenda the blocks are too long you have two options: either split the agenda in multiple smaller
	 * agendas, or provide a value for [daySplitHour], which will force a split in the blocks at the specified time.
	 * A `daySplitHour=4_00_00` in the first example would force the split of the block in two blocks: [M12-T4, T4-14]
	 *
	 * Notes:
	 * - The split hour if provided (not null) is always applied, even if not needed. If set to 5_00_00 on the second
	 * example, there would be no change, but if set to 4_00_00 we would get 3 groups (M12-T04, T04-T05, T05-T14).
	 * - The need for a split is checked only during the execution of appointment availabilities and safe booking
	 * methods (if needed but not provided they will fail). Changes to the agenda entity won't check the need for a
	 * split hour.
	 *
	 * # Effect on availabilities and safe booking
	 *
	 * A [daySplitHour] if not set properly may give availabilities that do not exist. To ensure the correctness of the
	 * availabilities and safe booking features you will have to make sure that no calendar items are created across
	 * the day split hour (for example if set to 2AM don't have a calendar item from 1:50AM to 2:10AM).
	 *
	 * The safe booking methods will validate this, but there is no validation done on calendar items created through
	 * the unrestricted methods.
	 *
	 * You could also have issues in cases where the [daySplitHour] is changed without taking appropriate migration
	 * steps.
	 *
	 * TODO exception for manually assigned calendar items?
	 */
	val daySplitHour: Int? = null, // TODO in future maybe we can replace this by supporting division of time table hours for example [8-10,10-12] would be equivalent to split at 10 for that time table item only
	val slottingAlgorithm: AgendaSlottingAlgorithm? = null,
	@JsonProperty("_attachments") override val attachments: Map<String, Attachment>? =  null,
	@JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@JsonProperty("rev_history") override val revHistory: Map<String, String>? = null
) : StoredICureDocument {
	companion object : DynamicInitializer<Agenda>

	init {
		@Suppress("DEPRECATION")
		require(rights.isEmpty() || userRights.isEmpty()) {
			"You cannot specify legacy rights and userRights at the same time"
		}
		if (zoneId == null) {
			if (
				schedules.any { tt -> tt.items.any { it.notAfterInMinutes != null || it.notBeforeInMinutes != null } }
			) throw IllegalArgumentException("ZoneId must be provided when in agendas with time-based constraints")
		} else {
			try {
				ZoneId.of(zoneId)
			} catch (e: DateTimeException) {
				throw IllegalArgumentException("Unsupported / invalid zone id $zoneId", e)
			}
		}
		require(schedules.isNotEmpty() || daySplitHour == null) { "`daySplitHour` has effect only on agendas with embedded schedule" }
		if (daySplitHour != null) requireNotNull(FuzzyDates.getFullLocalTime(daySplitHour)) { "`daySplitHour` is not a valid fuzzy time" }
		schedules.groupBy { it.resourceGroup }.forEach {  (resourceGroup, schedule) ->
			require (
				schedule.size <= 1 || schedule.asSequence().sortedBy {
					it.startDateTime ?: Long.MIN_VALUE
				}.zipWithNext().none { (first, second) ->
					first.endDateTime == null || second.startDateTime == null || first.endDateTime > second.startDateTime
				}
			) {
				"Resource group `${resourceGroup?.id}` has overlapping schedules"
			}
		}
		if (schedules.isEmpty()) {
			require(slottingAlgorithm == null) { "`slottingAlgorithm` has not effect on agendas without schedules" }
		}
	}

	fun merge(other: Agenda) = Agenda(args = this.solveConflictsWith(other))

	@Suppress("DEPRECATION")
	fun solveConflictsWith(other: Agenda) = super.solveConflictsWith(other) + mapOf(
		"name" to (this.name ?: other.name),
		"userId" to (this.userId ?: other.userId),
		"rights" to MergeUtil.mergeListsDistinct(this.rights, other.rights, { a, b -> a == b }) { a, _ -> a },
		"userRights" to (other.userRights + this.userRights),
		"schedules" to this.schedules.ifEmpty { other.schedules },
		"properties" to (other.properties + this.properties),
		"lockCalendarItemsBeforeInMinutes" to (this.lockCalendarItemsBeforeInMinutes ?: other.lockCalendarItemsBeforeInMinutes),
		"zoneId" to (this.zoneId ?: other.zoneId),
		"daySplitHour" to (this.daySplitHour ?: other.daySplitHour),
		"slottingAlgorithm" to (this.slottingAlgorithm ?: other.slottingAlgorithm)
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
