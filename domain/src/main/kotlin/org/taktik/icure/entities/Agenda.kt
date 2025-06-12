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
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.time.DateTimeException
import java.time.ZoneId

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
