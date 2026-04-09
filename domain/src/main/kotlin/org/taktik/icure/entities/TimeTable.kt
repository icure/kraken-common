/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.StoredICureDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.mergers.annotations.MergeStrategyUse
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeTable(
	/** The unique identifier of the timetable. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the timetable in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The id of the User that created this timetable. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this timetable. */
	@field:NotNull(
		autoFix = AutoFix.CURRENTDATAOWNERID,
		applyOnModify = false,
	) override val responsible: String? = null,
	/** The id of the medical location where this timetable was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the timetable as being member of a certain class. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** Codes that identify or qualify this particular timetable. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	/** The name of the timetable. */
	val name: String? = null,
	/** The id of the agenda this timetable belongs to. */
	val agendaId: String? = null,
	/** The start time of the timetable period (format: YYYYMMDDHHMMSS). */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The end time of the timetable period (format: YYYYMMDDHHMMSS). */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	/** The list of time table items defining individual time slots. */
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeListsDistinct({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		imports = ["org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct"]
	)
	val items: List<TimeTableItem> = emptyList(),
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredICureDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
