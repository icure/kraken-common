/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.utils.FuzzyValues
import java.io.Serializable

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

	init {
		require(FuzzyValues.strictIsFuzzyTime(startHour) ) { "$startHour is not HHMMSSFuzzyTime" }
		require(endHour == END_OF_DAY || FuzzyValues.strictIsFuzzyTime(endHour)) { "$endHour is not HHMMSSFuzzyTime" }
		require(endHour > startHour) { "Unsatisfied requirement: endHour > startHour" }
		// The following requirements are implied by the previous requirements
//		require(startHour >= 0 && startHour < 23_59_60) { "Unsatisfied requirement: startHour >= 0 && startHour < 23_59_60" }
//		require(endHour > 0 && endHour <= 23_59_60) { "Unsatisfied requirement: endHour > 0 && endHour <= 23_59_60" }
	}
}
