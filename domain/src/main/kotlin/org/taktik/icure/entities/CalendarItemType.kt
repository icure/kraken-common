/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CalendarItemType(
	@param:ContentValue(ContentValues.UUID) @param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val name: String? = null,
	val healthcarePartyId: String? = null,
	val agendaId: String? = null,
	val defaultCalendarItemType: Boolean = false,
	val color: String? = null, // "#123456"
	val duration: Int = 0, // Duration in minutes
	val extraDurationsConfig: DurationConfig? = null,
	val externalRef: String? = null,
	val mikronoId: String? = null,
	val docIds: Set<String> = emptySet(),
	val otherInfos: Map<String, String> = emptyMap(),
	val subjectByLanguage: Map<String, String> = emptyMap(),
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredDocument {
	companion object : DynamicInitializer<CalendarItemType>

	init {
		require(extraDurationsConfig == null || extraDurationsConfig.canAccept(duration)) {
			"The default duration of the CalendarItemType must be included in the extraDurationConfig"
		}
	}

	fun merge(other: CalendarItemType) = CalendarItemType(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: CalendarItemType) = super.solveConflictsWith(other) +
		mapOf(
			"name" to (this.name ?: other.name),
			"color" to (this.color ?: other.color),
			"duration" to (this.duration.coerceAtLeast(other.duration)),
			"externalRef" to (this.externalRef ?: other.externalRef),
			"mikronoId" to (this.mikronoId ?: other.mikronoId),
			"docIds" to (other.docIds + this.docIds),
			"otherInfos" to (other.otherInfos + this.otherInfos),
			"subjectByLanguage" to (other.subjectByLanguage + this.subjectByLanguage),
		)

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface DurationConfig {

		fun canAccept(duration: Int): Boolean

		data class DurationList(val durations: List<Int> = emptyList()): DurationConfig {
			override fun canAccept(duration: Int) = duration in durations
		}

		data class DurationFormula(
			val min: Int,
			val max: Int,
			val step: Int,
		): DurationConfig {

			init {
				require(min % step == 0) { "Minimum CalendarItemType duration must be a multiple of $step" }
				require(max % step == 0) { "Maximum CalendarItemType duration must be a multiple of $step" }
			}
			override fun canAccept(duration: Int): Boolean = duration >= min && duration <= max && (duration % step == 0)
		}

	}
}
