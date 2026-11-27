package org.taktik.icure.services.external.rest.v2.dto

import org.taktik.icure.dto.annotations.filtering.ActiveField

data class GroupsMatchResultDto(
	/**
	 * All matches of users
	 */
	@ActiveField val matches: List<Match>,
	/**
	 * Details for all groups in [matches].
	 */
	@ActiveField val matchedGroups: Map<String, GroupDto>,
) {
	data class Match(
		/**
		 * Local id of the matched user
		 */
		@ActiveField val userId: String,
		/**
		 * If the matched user is a data owner the type of data owner, null otherwise.
		 */
		@ActiveField val dataOwnerType: DataOwnerTypeDto?,
		/**
		 * The matched group id
		 */
		@ActiveField val groupId: String,
	)
}