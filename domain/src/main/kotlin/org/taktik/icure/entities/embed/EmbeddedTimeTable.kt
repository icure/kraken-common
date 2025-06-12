package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EmbeddedTimeTable(
	val tags: Set<CodeStub> = emptySet(),
	val codes: Set<CodeStub> = emptySet(),
	val medicalLocationId: String? = null,
	/**
	 * Can be used for human-readable name to help identify the timetable.
	 * Note that if the agenda is public this name will also be public.
	 */
	val name: String? = null,
	/**
	 * The date and time when the timetable starts being valid / available, as a fuzzy date time.
	 * If null the timetable is going to be considered always valid until [endDateTime].
	 * If both are null the timetable is considered always valid
	 */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val startDateTime: Long? = null,
	/**
	 * The date and time when the timetable ends being valid / available, as a fuzzy date time.
	 * If null the timetable is going to be considered always valid since [startDateTime].
	 * If both are null the timetable is considered always valid
	 */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val endDateTime: Long? = null,
	/**
	 * Details of the availabilities for this timetable.
	 * The availabilities may be truncated by the startTime or endTime.
	 */
	val items: List<EmbeddedTimeTableItem>,
// TODO later val slottingAlgorithm: TimeTableSlottingAlgorithm
) : Serializable {
	init {
		require(startDateTime == null || FuzzyValues.strictTryParseFuzzyDateTime(startDateTime) != null ) { "startDateTime must be null or a valid fuzzyDateTime ($startDateTime)" }
		require(endDateTime == null || FuzzyValues.strictTryParseFuzzyDateTime(endDateTime) != null ) { "endDateTime must be null or a valid fuzzyDateTime ($endDateTime)" }
		require(startDateTime == null || endDateTime == null || startDateTime < endDateTime) { "If both startTime and endTime are specified startTime must be <= endTime ($startDateTime > $endDateTime)" }
		require(items.isNotEmpty()) { "At least one item is required" }
	}
}