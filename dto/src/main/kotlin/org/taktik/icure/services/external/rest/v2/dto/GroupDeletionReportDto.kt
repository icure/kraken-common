package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO representing one item handled by a group hard deletion.
 */
data class GroupDeletionReportDto(
	/** The type of the deleted resource. */
	@param:Schema(required = true) @ActiveField val type: String,
	/** The unique identifier of the deleted resource. */
	@ActiveField val id: String,
	/** The server on which the resource was deleted. */
	@param:Schema(required = true) @ActiveField val server: String,
	/** What happened to the resource. */
	@param:Schema(defaultValue = "DELETED") @ActiveField val status: Status = Status.DELETED,
	/** Details on a [Status.FAILED] resource. */
	@ActiveField val message: String? = null,
) {
	enum class Status {
		/** The resource existed and was purged by this run. */
		DELETED,
		/** The resource did not exist (e.g. already purged by a previous run): counts as a success. */
		ABSENT,
		/** The resource could not be purged: the hard deletion must be run again. */
		FAILED,
	}
}
