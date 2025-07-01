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
import org.taktik.icure.entities.embed.EmbeddedTimeTable
import org.taktik.icure.entities.embed.EmbeddedTimeTableItem
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.Right
import org.taktik.icure.entities.embed.UserAccessLevel
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.time.DateTimeException
import java.time.ZoneId

/**
 * Represents the working hours of a resource or group of resources that can handle calendar items (appointments).
 * Multiple embedded time tables can be used to represent different periods (for example winter and summer working
 * hours), or different groups of resources (usually hcps).
 *
 * # Multiple time tables vs multiple agendas for groups of resources
 *
 * When choosing how to implement your agendas you should consider that normally the calendar items are linked directly
 * to an agenda, and not directly to a timetable or timetable item.
 *
 * The most common use case for agenda is to create one for each doctor. This allows the patients to pick their desired
 * doctor.
 *
 * However, in some cases you may want to be able to let the patient only pick the type of appointment, while the hcp
 * that will perform the appointment is chosen automatically by the system in a second moment.
 * This can be done by having an agenda with multiple timetables, where each timetable can represent one or more hcps
 * that have the same capabilities (i.e. types of appointments that they can serve) and working hours.
 * This, howeverm us useful only if the different groups of resources have some non-empty intersection between their
 * capabilities. Resources that have fully disjointed sets of capabilities should be grouped in different agendas, as
 * there is no advantage to having them in the same agenda.
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
	val timeTables: List<EmbeddedTimeTable> = emptyList(),
	/**
	 * Custom properties of the agenda
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
				timeTables.any { tt -> tt.items.any { it.notAfterInMinutes != null || it.notBeforeInMinutes != null } }
			) throw IllegalArgumentException("ZoneId must be provided when in agendas with time-based constraints")
		} else {
			try {
				ZoneId.of(zoneId)
			} catch (e: DateTimeException) {
				throw IllegalArgumentException("Unsupported / invalid zone id $zoneId", e)
			}
		}
		require(timeTables.isNotEmpty() || daySplitHour == null) { "`daySplitHour` has effect only on embedded time tables" }
		if (daySplitHour != null) requireNotNull(FuzzyValues.strictTryParseFuzzyTime(daySplitHour)) { "`daySplitHour` is not a valid fuzzy time" }
	}

	fun merge(other: Agenda) = Agenda(args = this.solveConflictsWith(other))

	@Suppress("DEPRECATION")
	fun solveConflictsWith(other: Agenda) = super.solveConflictsWith(other) + mapOf(
		"name" to (this.name ?: other.name),
		"userId" to (this.userId ?: other.userId),
		"rights" to MergeUtil.mergeListsDistinct(this.rights, other.rights, { a, b -> a == b }) { a, _ -> a },
		"userRights" to (other.userRights + this.userRights),
		"timeTables" to this.timeTables.ifEmpty { other.timeTables },
		"properties" to (other.properties + this.properties),
		"zoneId" to (this.zoneId ?: other.zoneId),
		"lockCalendarItemsBeforeInMinutes" to (this.lockCalendarItemsBeforeInMinutes ?: other.lockCalendarItemsBeforeInMinutes),
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
