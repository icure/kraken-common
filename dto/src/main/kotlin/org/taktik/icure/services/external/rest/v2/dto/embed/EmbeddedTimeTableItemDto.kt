/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EmbeddedTimeTableItemDto(
	val rrule: String,
	val rruleStartDate: Int? = null,
	val notBeforeInMinutes: Int? = null,
	val notAfterInMinutes: Int? = null,
	val hours: List<EmbeddedTimeTableHourDto>,
	@param:JsonInclude(JsonInclude.Include.ALWAYS) val calendarItemTypesIds: Set<String>,
	@get:Schema(defaultValue = "1")
	val availabilities: Int = 1,
	val reservingRights: Set<String> = emptySet(),
	@get:Schema(defaultValue = "false")
	val public: Boolean = false,
) : Serializable
