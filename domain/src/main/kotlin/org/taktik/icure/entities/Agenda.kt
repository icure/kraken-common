/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.CalendarItem.AvailabilitiesAssignmentStrategy
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
 * The existence of a schedule allows you to specify groups of users which will be able to take appointments,
 * represented by [CalendarItem]s, for the agenda within certain limits.
 * You can also allow some users to take appointments for the agenda even outside the defined schedule.
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
 * ## Availabilities
 *
 * You can configure an agenda to allow users to create calendar items for the agenda as long as it sits within the
 * limits specified by the agenda's schedule.
 *
 * These users will have to first interrogate the availabilities of the agenda, to get information about when a certain
 * type of appointment can be taken for that agenda.
 * An agenda has an availability for a calendar item at a specific time if:
 * 1. The schedule allows for that type of calendar item at that time
 * 2. Creating a calendar item at that time wouldn't cause conflicts with existing calendar items.
 *
 * ### Example
 *
 * Consider the agenda
 * - Group1
 *   - TimeTableItem1 09:00-11:30, can serve t1 or t2, each lasting 20 minutes
 * - Group2
 *   - TimeTableItem2 09:30-12:00, can serve t1 or t3, each lasting 20 minutes
 *
 * - There is no availability of type t2 at 11:20 due to rule 1
 * - There could be an availability of type t3 at 11:10 due to rule 1, however if there are already other calendar items
 *   we have to verify with rule 2. For example, the following situations would void the availability:
 *   - There are 2 calendar items of type t1 intersecting the time 11:10-11:30
 *   - There is 1 calendar item of type t3 intersecting the time 11:10-11:30
 *   - There is 1 calendar item of type t2 and one of type t1 intersecting the time 11:10-11:30
 *   - There is 1 calendar item of type t1 at time 11:20: this calendar item can't be served by group 1 as it sits
 *     partially outside its schedule, therefore it must be served by group 2 and won't allow to have an appointment of
 *     time t3 intersecting the time 11:20-11:40
 *
 * ## Irregularities and loose assignments
 *
 * You will encounter cases where you have "irregular" appointments, that is calendar items that can't be satisfied
 * exactly by the schedule. There are 2 types of irregularities:
 * - off-schedule appointments are calendar items that couldn't be satisfied even if they were the only
 *   booked appointments: this can be because their time is partially outside the schedule, or because their calendar
 *   item type is not allowed by the schedule at their specific time.
 * - overbookings happen when the appointments, even if not individually off-schedule, can't fit all together within the
 *   schedule
 *
 * Although unprivileged users won't be able to create off-schedule appointments or cause overbookings, there are still
 * situations where you may want or need to create these kinds of appointments.
 *
 * A common situation when this can happen is when an employee is on leave: you want to create a single calendar item
 * matching the time of the leave, to take away the availabilities from the resource group of that employee.
 * Usually this calendar item will not match exactly the schedule, so it will be an off-schedule appointment.
 * For pre-planned leave this will not have any additional implications, but if the leave is unplanned it is possible
 * that there are already some appointments scheduled and adding the leave will result in a overbooking.
 *
 * When there are overbookings or off-schedule appointments the availabilities algorithm may struggle to find
 * availabilites, since the irregular appointments are always going to be in conflict.
 *
 * To prevent this from happening, you can set [CalendarItem.availabilitiesAssignmentStrategy] to
 * [AvailabilitiesAssignmentStrategy.Loose] when creating irregular appointments
 *
 * Note: if you're creating the appointment in an agenda with multiple resource group you should also set the
 * [CalendarItem.resourceGroup] explicitly.
 *
 * When a calendar item requires the loose assignment strategy, the availabilities won't consider it when checking
 * conflicts; instead, the calendar item will only apply a flat reduction of the availabilities during the time of the
 * calendar item to all the timetable items of the affected resource group.
 *
 * Note: to limit the impact of overbookings and off-schedule appointments when using strict assignment the conflicts
 * are checked only with calendar items that are intersecting the availability directly, or indirectly through other
 * intersecting calendar items.
 *
 * ### Example
 *
 * Consider an Agenda with a single resource group and time table items 09:00-12:00 for calendar item type t1,
 * 13:00-16:00 for t2, each with 2 availabilities.
 *
 * If we have a calendar item of type t1 between 10:20-10:50 and an off-schedule calendar item of type t2 between
 * 10:00-10:30.
 * Depending on the assignment strategy configured for the second calendar item we will have different availabilities:
 * - If Strict blocks availabilities from 10:00-10:50: any calendar item intersecting that time would be blocked since
 *   it would intersect (directly or indirectly through the first calendar item) the off-schedule appointment that will
 *   always be conflicting.
 * - If Loose only blocks the availabilities 10:20-10:30, since we already have the maximum of two appointments during
 *   that time.
 * - If null same as Loose, since the calendar item is off-schedule.
 *
 * ### Loose calendar items with multiple resource groups
 *
 * If your agenda has multiple resource groups you should always specify the [CalendarItem.resourceGroup] for calendar
 * items with loose assignment, or you might get unexpected availabilities.
 *
 * For example, if we have an agenda with the following 2 groups
 * - Group 1 at 09:00-12:00 1 availability for calendar item type 0 or 1
 * - Group 2 at 09:00-12:00 1 availability for calendar item type 0 or 2
 *
 * If you have two calendar items at 10:30-11:00 of type 0 with loose assignment but no resource group specified, they
 * could both be assigned to the same group, since loose calendar items can never conflict with other loose calendar
 * items.
 * This means there will still be availabilities for type 0, 1, or 2, at 10:30-11:00. Linking one of the calendar items
 * to group 1 and the other to group 2 instead would remove the availability as you would expect.
 *
 * ## Multiple groups of resources vs multiple items in single group
 *
 * You may think the following agendas are equivalent:
 *
 * - Agenda 1
 *   - Group 1
 *     - TimeTableItem 1
 *       - Monday 09:00->12:00 - Calendar item types: t1, t2
 *     - TimeTableItem 2
 *       - Monday 12:15->16:00 - Calendar item types: t3, t4
 *     - TimeTableItem 3
 *       - Tuesday 09:00->12:00 - Calendar item types: t1, t2, availabilities: 2
 *
 * - Agenda 2
 *   - Group 1
 *     - TimeTableItem 1
 *       - Monday 09:00->12:00 - Calendar item types: t1, t2
 *     - TimeTableItem 2
 *       - Tuesday 09:00->12:00 - Calendar item types: t1, t2
 *   - Group 2
 *     - TimeTableItem 1
 *       - Monday 12:15->16:00 - Calendar item types: t3, t4
 *     - TimeTableItem 2
 *       - Tuesday 09:00->12:00 - Calendar item types: t1, t2
 *
 * As long as there are no overbookings and no off-schedule appointments there is going to be no real difference.
 *
 * However, in the presence of overbookings or off-schedule appointments, the availabilities algorithm will behave
 * differently depending on the representation you chose.
 * If your chosen representation of the agenda doesn't properly match the resources the solution found could omit
 * some availabilities, or worse, provide availabilities that can't be satisfied.
 *
 * Agenda 1 defines a single group of resources that all have the same capabilities (in case of workers these could be
 * competences). Even though the resources could allow for an appointment of type t3 at 09:00 on either monday or
 * tuesday, for some reason we don't want people to take this kind of appointment at that time.
 *
 * In agenda 2, instead, the groups of resources have distinct capabilities: there is no resource available at 09:00 on
 * Monday that could handle an appointment of type t3.
 *
 * For example, the two representations have significant differences if we have an off-schedule appointment 11:45-12:30
 * of type t3.
 * In the case of Agenda 1 this appointment would impact the availabilities of both TimeTableItem 1 and 2.
 * In the case of Agenda 2 instead the [CalendarItem.resourceGroup] should be set to group 2, and this will ensure that
 * only the availabilities of Group 2 - TimeTableItem 1 will be impacted.
 *
 * As a general rule use separate [ResourceGroupAllocationSchedule] for groups of resources that don't share exactly
 * the same capabilities and schedule. Another hint that you need to divide your timetable items across different
 * groups is having items with overlapping schedules.
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
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	val name: String? = null,
	@Deprecated("Use TODO instead") val userId: String? = null,
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
	 * An identifier for the zone of the agenda. Must be an id accepted by java's ZoneId.
	 * Mandatory if the agenda specifies any time-based constraint:
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
	 */
	val daySplitHour: Int? = null, // TODO in future maybe we can replace this by supporting division of time table hours for example [8-10,10-12] would be equivalent to split at 10 for that time table item only
	/**
	 * If true the agenda won't be available for availabilities and safe booking requests, if false (default) the agenda
	 * can be used with those features normally.
	 * An unpublished agenda has less strict integrity checks.
	 */
	val unpublished: Boolean = false,
	val slottingAlgorithm: AgendaSlottingAlgorithm? = null,
	/**
	 * If not null limits the amount of appointments that each user without special privileges is allowed to take for
	 * this agenda during each month.
 	 */
	val publicBookingQuota: Int? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredICureDocument {
	companion object : DynamicInitializer<Agenda>

	init {
		@Suppress("DEPRECATION")
		require(rights.isEmpty() || userRights.isEmpty()) {
			"You cannot specify legacy rights and userRights at the same time"
		}
		if (!unpublished) {
			if (zoneId == null) {
				if (
					schedules.any { tt -> tt.items.any { it.notAfterInMinutes != null || it.notBeforeInMinutes != null } }
				) {
					throw IllegalArgumentException("ZoneId must be provided for published agendas with time-based constraints")
				}
			}
			schedules.groupBy { it.resourceGroup?.id }.also { groupedSchedules ->
				groupedSchedules.forEach { (resourceGroup, schedule) ->
					require(
						schedule.size <= 1 ||
							schedule.asSequence().sortedBy {
								it.startDateTime ?: Long.MIN_VALUE
							}.zipWithNext().none { (first, second) ->
								first.endDateTime == null || second.startDateTime == null || first.endDateTime > second.startDateTime
							},
					) {
						"Resource group `$resourceGroup` has overlapping schedules. Not allowed in published agendas"
					}
				}
				require(groupedSchedules.size == 1 || !groupedSchedules.containsKey(null)) {
					"A published agenda can't specify a mix of schedules with null and non-null resource groups"
				}
			}
			if (schedules.isNotEmpty()) {
				require(slottingAlgorithm != null) { "`slottingAlgorithm` is required for published agendas with embedded schedule" }
			}
			schedules.forEach { it.checkPublishedRequirements() }
		}
		zoneId?.also {
			try {
				ZoneId.of(zoneId)
			} catch (e: DateTimeException) {
				throw IllegalArgumentException("Unsupported / invalid zone id $zoneId", e)
			}
		}
		if (daySplitHour != null) requireNotNull(FuzzyDates.getFullLocalTime(daySplitHour)) { "`daySplitHour` is not a valid fuzzy time" }
	}

	fun merge(other: Agenda) = Agenda(args = this.solveConflictsWith(other))

	@Suppress("DEPRECATION")
	fun solveConflictsWith(other: Agenda) = super.solveConflictsWith(other) +
		mapOf(
			"name" to (this.name ?: other.name),
			"userId" to (this.userId ?: other.userId),
			"rights" to MergeUtil.mergeListsDistinct(this.rights, other.rights, { a, b -> a == b }) { a, _ -> a },
			"userRights" to (other.userRights + this.userRights),
			"schedules" to this.schedules.ifEmpty { other.schedules },
			"properties" to (other.properties + this.properties),
			"zoneId" to (this.zoneId ?: other.zoneId),
			"daySplitHour" to (this.daySplitHour ?: other.daySplitHour),
			"slottingAlgorithm" to (this.slottingAlgorithm ?: other.slottingAlgorithm),
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
