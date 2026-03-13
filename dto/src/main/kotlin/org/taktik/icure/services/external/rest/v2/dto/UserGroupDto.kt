/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.dto

/**
 * DTO representing a user within the context of a specific group, including the group hierarchy
 * and the user's associated data owner identifiers.
 */
data class UserGroupDto(
	/** The identifier of the group this user belongs to. */
	val groupId: String? = null,
	/** The name of the group this user belongs to. */
	val groupName: String? = null,
	/** The list of groups forming the hierarchy from the topmost group down to this group. */
	val groupsHierarchy: List<GroupDto> = emptyList(),
	/** The identifier of the user. */
	val userId: String? = null,
	/** The login identifier of the user. */
	val login: String? = null,
	/** The display name of the user. */
	val name: String? = null,
	/** The email address of the user. */
	val email: String? = null,
	/** The phone number of the user. */
	val phone: String? = null,
	/** The identifier of the patient linked to this user, if any. */
	val patientId: String? = null,
	/** The identifier of the healthcare party linked to this user, if any. */
	val healthcarePartyId: String? = null,
	/** The identifier of the device linked to this user, if any. */
	val deviceId: String? = null,
	/** The name of the parent group of the topmost group in the hierarchy. */
	val nameOfParentOfTopmostGroupInHierarchy: String? = null,
)
