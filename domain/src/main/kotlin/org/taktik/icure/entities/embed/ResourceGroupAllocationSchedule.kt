package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.utils.FuzzyDates
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceGroupAllocationSchedule(
	/**
	 * When representing different resource groups in a single agenda you need to provide a (custom) code stub to
	 * distinguish them.
	 * Different ResourceGroupAllocationSchedule in a single agenda for the same resourceGroup can't be active at
	 * overlapping times.
	 * Note that if any of the items are public this will also be public.
	 */
	val resourceGroup: CodeStub? = null,
	val tags: Set<CodeStub> = emptySet(),
	val codes: Set<CodeStub> = emptySet(),
	// TODO place or medical location?
	/**
	 * Can be used for human-readable name to help identify the schedule.
	 * Note that if any of the items are public this will also be public.
	 */
	val name: String? = null,
	/**
	 * The date and time when the schedule starts being valid / available, as a fuzzy date time.
	 * If null the schedule is going to be considered always valid until [endDateTime].
	 * If both are null the schedule is considered always valid
	 */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val startDateTime: Long? = null,
	/**
	 * The date and time when the schedule ends being valid / available, as a fuzzy date time.
	 * If null the schedule is going to be considered always valid since [startDateTime].
	 * If both are null the schedule is considered always valid
	 */
	@field:NotNull(autoFix = AutoFix.FUZZYNOW) val endDateTime: Long? = null,
	/**
	 * Details of the availabilities for this schedule.
	 * The availabilities may be truncated by the startTime or endTime.
	 */
	val items: List<EmbeddedTimeTableItem>,
// TODO later val slottingAlgorithm: TimeTableSlottingAlgorithm
) : Serializable {
	init {
		require(startDateTime == null || FuzzyDates.getFullLocalDateTime(startDateTime, false) != null) { "startDateTime must be null or a valid fuzzyDateTime ($startDateTime)" }
		require(endDateTime == null || FuzzyDates.getFullLocalDateTime(endDateTime, false) != null) { "endDateTime must be null or a valid fuzzyDateTime ($endDateTime)" }
		require(startDateTime == null || endDateTime == null || startDateTime < endDateTime) { "If both startTime and endTime are specified startTime must be <= endTime ($startDateTime > $endDateTime)" }
		resourceGroup?.apply {
			requireNormalized()
			require(context == null && contextLabel == null) { "Context is not allowed for resourceGroup code stub $resourceGroup" }
		}
	}

	fun checkPublishedRequirements() {
		require(items.isNotEmpty()) { "At least one item per schedule is required in published agenda" }
		items.forEach { it.checkPublishedRequirements() }
	}
}
