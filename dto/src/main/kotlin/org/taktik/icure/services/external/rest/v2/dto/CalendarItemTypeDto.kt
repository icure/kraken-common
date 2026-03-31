/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.entities.RawJson
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableRootDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a type of calendar item, defining properties like duration, color, and name for appointments.
 * Calendar item types are used to categorize calendar items within an agenda.
 */
data class CalendarItemTypeDto(
	/** The Id of the calendar item type. */
	override val id: String,
	/** The revision of the calendar item type in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The id of the healthcare party associated with this type. */
	val healthcarePartyId: String? = null,
	/** The id of the agenda this type belongs to. */
	val agendaId: String? = null,
	/** Whether this is the default calendar item type for its agenda. */
	val defaultCalendarItemType: Boolean = false,
	/** The display name of this calendar item type. */
	val name: String? = null,
	/** The color associated with this type, in hex format (e.g. "#123456"). */
	val color: String? = null, // "#123456"
	/** The default duration in minutes for calendar items of this type. */
	@param:Schema(defaultValue = "0") val duration: Int = 0,
	/** Optional configuration for additional allowed durations. */
	val extraDurationsConfig: DurationConfigDto? = null,
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
	/** Public properties exposed to anonymous endpoints for public calendar items. */
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT) val publicProperties: Set<PropertyStubDto>? = null,
	override val extensions: RawJson.JsonObject? = null,
	override val extensionsVersion: Int? = null,
) : StoredDocumentDto,
	ExtendableRootDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface DurationConfigDto {
		data class Set(val durations: kotlin.collections.Set<Int> = emptySet()) : DurationConfigDto
		data class Formula(val min: Int, val max: Int, val step: Int) : DurationConfigDto
	}
}
