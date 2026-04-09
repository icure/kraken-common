/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.mergers.annotations.Mergeable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class CalendarItemType(
	/** The Id of the calendar item type. */
	@param:JsonProperty("_id") override val id: String,
	/** The revision of the calendar item type in the database, used for conflict management / optimistic locking. */
	@param:JsonProperty("_rev") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	/** The display name of this calendar item type. */
	val name: String? = null,
	/** The id of the healthcare party associated with this type. */
	val healthcarePartyId: String? = null,
	/** The id of the agenda to which this type can be applied. If null, the type can be applied to any agenda. */
	val agendaId: String? = null,
	/** Whether this is the default calendar item type for its agenda. */
	val defaultCalendarItemType: Boolean = false,
	/** The color associated with this type, in hex format (e.g. "#123456"). */
	val color: String? = null, // "#123456"
	/** The default duration in minutes for calendar items of this type. */
	val duration: Int = 0, // Duration in minutes
	/** Optional configuration for additional allowed durations. */
	val extraDurationsConfig: DurationConfig? = null,
	/** An external reference identifier. */
	val externalRef: String? = null,
	/** An external Mikrono identifier. */
	val mikronoId: String? = null,
	/** A set of document ids associated with this type. */
	val docIds: Set<String> = emptySet(),
	/** Additional information stored as key-value pairs. */
	val otherInfos: Map<String, String> = emptyMap(),
	/** Subject text for this calendar item type, by language. */
	val subjectByLanguage: Map<String, String> = emptyMap(),
	/**
	 * Public properties of public calendar items (i.e. calendar items available in public timetableitems) are exposed to anonymous endpoints.
	 */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val publicProperties: Set<PropertyStub>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
) : StoredDocument {

	init {
		require(extraDurationsConfig == null || extraDurationsConfig.canAccept(duration)) {
			"The default duration of the CalendarItemType must be included in the extraDurationConfig"
		}
	}

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
	fun canAccept(duration: Int): Boolean = extraDurationsConfig?.canAccept(duration) ?: (duration == this.duration)

	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface DurationConfig {

		fun canAccept(duration: Int): Boolean

		data class Set(val durations: kotlin.collections.Set<Int> = emptySet()): DurationConfig {
			override fun canAccept(duration: Int) = duration in durations
		}

		data class Formula(
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
