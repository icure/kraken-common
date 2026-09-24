package org.taktik.icure.services.external.rest.v1.dto.embed

/**
 * The commercial status of a group. Groups whose status is not explicitly [PAYING] or [FREE] are reported as
 * [INHERITED_PAYING] or [INHERITED_FREE] depending on the first ancestor group that is [PAYING] or [FREE]
 * ([INHERITED_FREE] if there is none).
 */
enum class GroupStatusDto {
	PAYING,
	FREE,
	INHERITED_PAYING,
	INHERITED_FREE,
}
