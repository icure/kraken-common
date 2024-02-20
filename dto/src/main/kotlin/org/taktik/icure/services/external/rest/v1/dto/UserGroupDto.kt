/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto

data class UserGroupDto(
	val groupId: String? = null,
	val groupName: String? = null,
	val groupsHierarchy: List<GroupDto> = emptyList(),
	val userId: String? = null,
	val login: String? = null,
	val name: String? = null,
	val email: String? = null,
	val phone: String? = null,
	val patientId: String? = null,
	val healthcarePartyId: String? = null,
	val deviceId: String? = null,
	val nameOfParentOfTopmostGroupInHierarchy: String? = null,
	)
