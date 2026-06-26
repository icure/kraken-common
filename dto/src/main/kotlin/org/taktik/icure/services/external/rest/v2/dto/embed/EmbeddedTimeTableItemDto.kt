/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a recurring time table item with scheduling rules, availability slots, and access control.
 */
data class EmbeddedTimeTableItemDto(
	/** The recurrence rule (RFC 5545 RRULE format) defining the schedule pattern. */
	@ActiveField val rrule: String,
	/** The start date of the recurrence rule as an integer (YYYYMMDD). */
	@ActiveField val rruleStartDate: Int? = null,
	/** Minimum number of minutes before the slot that booking is allowed. */
	@ActiveField val notBeforeInMinutes: Int? = null,
	/** Maximum number of minutes after the slot start that booking is allowed. */
	@ActiveField val notAfterInMinutes: Int? = null,
	/** The list of hour ranges within this time table item. */
	@ActiveField val hours: List<EmbeddedTimeTableHourDto>,
	/** The set of calendar item type identifiers associated with this item. */
	@param:JsonInclude(JsonInclude.Include.ALWAYS) @ActiveField val calendarItemTypesIds: Set<String>,
	/** The number of available slots for this time table item. */
	@param:Schema(defaultValue = "1")
	@ActiveField val availabilities: Int = 1,
	/** The set of identifiers for parties allowed to reserve slots. */
	@ActiveField val reservingRights: Set<String> = emptySet(),
	/** Whether this time table item is publicly visible. */
	@param:Schema(defaultValue = "false")
	@ActiveField val public: Boolean = false,
) : Serializable
