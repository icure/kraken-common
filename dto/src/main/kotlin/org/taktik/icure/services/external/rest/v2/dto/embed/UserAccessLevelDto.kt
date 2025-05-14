package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Specifies the access level of a user that is not necessarily a data owner on an entity. The interpretation of the
 * entities is the following:
 * - Read: the user only has read permissions on the entity.
 * - Write: the user has read and write permissions on the entity, but they cannot modify the access of users to the entity.
 * - Admin: the user has full control over the entity.
 */
enum class UserAccessLevelDto {
	Admin,
	Read,
	Write
}
