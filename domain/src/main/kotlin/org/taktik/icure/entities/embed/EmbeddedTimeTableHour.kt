/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.utils.FuzzyDates
import java.io.Serializable
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class  EmbeddedTimeTableHour(
	/**
	 * The start of a time slot, as fuzzy time in 24-hour hhmmss format.
	 */
	val startHour: Int, //hhmmss
	/**
	 * The end of a time slot, as fuzzy time in 24-hour hhmmss format.
	 * The special value [END_OF_DAY] (23_59_60) can be used to indicate the end of the day.
	 */
	val endHour: Int, //hhmmss
) : Serializable {
	companion object {
		const val END_OF_DAY = 23_59_60
	}

	val startHourLocalTime get() = FuzzyDates.getFullLocalTime(startHour)!!
	val endHourLocalTime get() = if (endHour == END_OF_DAY) LocalTime.MIDNIGHT else FuzzyDates.getFullLocalTime(endHour)!!

	init {
		require(FuzzyDates.getFullLocalTime(startHour) != null) { "$startHour is not a valid full fuzzy time" }
		require(endHour == END_OF_DAY || FuzzyDates.getFullLocalTime(endHour) != null) { "$endHour is not a valid full fuzzy time nor the END_OF_DAY marker" }
		require(endHour > startHour) { "Unsatisfied requirement: endHour > startHour" }
	}
}
