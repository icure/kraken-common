/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import java.io.Serializable
import java.text.SimpleDateFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.commons.lang3.StringUtils
import org.taktik.icure.entities.base.CodeStub

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegimenItem(
	//Day definition (One and only one of the three following should be not null)
	//The three are null if it applies to every day
	val date: Long? = null, //yyyymmdd at this date
	val dayNumber: Int? = null, //day 1 of treatment. 1 based numeration
	val weekday: Weekday? = null, //on monday

	//Time of day definition (One and only one of the three following should be not null)
	//Both are null if not specified
	val dayPeriod: CodeStub? = null, //CD-DAYPERIOD
	val timeOfDay: Long? = null, //hhmmss 103010
	val administratedQuantity: AdministrationQuantity? = null
) : Serializable {
	override fun toString(): String {
		val df = SimpleDateFormat("dd/MM/yyyy")
		var result = if (date != null) String.format("the %s", df.format(date)) else if (dayNumber != null) String.format("on day %d", dayNumber) else if (weekday?.weekday?.code != null) String.format("on %s", weekday.weekday.code) else null
		if (dayPeriod != null && !StringUtils.isEmpty(dayPeriod.code)) {
			result = if (result != null) String.format("%s %s", result, dayPeriod.code) else dayPeriod.code
		}
		if (timeOfDay != null) {
			val timeOfDayDescr = if (timeOfDay == 120000L) "noon" else String.format("%d:%d", timeOfDay / 10000, timeOfDay / 100 % 100)
			result = if (result != null) String.format("%s at %s", result, timeOfDayDescr) else String.format("at %s", timeOfDayDescr)
		}
		return String.format("%s, %s", administratedQuantity, result)
	}
}
