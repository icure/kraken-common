/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegimenItemDto(
	// Day definition (One and only one of the three following should be not null)
	// The three are null if it applies to every day
	val date: Long? = null, // yyyymmdd at this date
	val dayNumber: Int? = null, // day 1 of treatment. 1 based numeration
	val weekday: Weekday? = null, // on monday
	// Time of day definition (One and only one of the three following should be not null)
	// Both are null if not specified
	val dayPeriod: CodeStubDto? = null, // CD-DAYPERIOD
	val timeOfDay: Long? = null, // hhmmss 103010
	val administratedQuantity: AdministrationQuantity? = null,
) : Serializable
