package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * A group whose hard deletion was interrupted: hard deleting the group again resumes it.
 */
data class InterruptedGroupHardDeletionDto(
	/** The id of the group. */
	@param:Schema(required = true) @ActiveField val groupId: String,
	/** When the group was soft deleted. */
	@ActiveField val deletionDate: Long? = null,
	/** When the hard deletion started, null for a group hard deleted before group tombstones existed. */
	@ActiveField val hardDeletionStartedAt: Long? = null,
)
