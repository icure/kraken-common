package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnore
import org.taktik.icure.entities.embed.AccessLevel

/**
 * Represents an entity where access control is granted to specific users instead of or in addition to all users with a specific role.
 * The id of users with access to the entity is explicitly indicated in the entity itself, but it is possible that some additional users
 * have access to the entity without being explicitly indicated in the entity (see [HasSecureDelegationsAccessControl]).
 * Note that user-based access control may not apply to all methods for an entity, and in some cases a user role may provide a higher
 * access level compared to the user-specific level, but in all cases only users which were given explicit access to the encryption key of
 * an entity will be able to read the encrypted data.
 */
interface HasExplicitDataOwnerAccess {
	/**
	 * Data owners with explicit access to the entity, and corresponding access level.
	 */
	// Currently fine-grained permissions are not yet supported, but for new entity types with explicit data owner access which may need to
	// support fine-grained access control in future you should consider representing it as a map { "*": AccessLevel }, as done in
	// SecureDelegation.permissions
	@get:JsonIgnore val dataOwnersWithExplicitAccess: Map<String, AccessLevel>
}

fun HasExplicitDataOwnerAccess.dataOwnerIdsWithExplicitAccessOfAtLeast(
	accessLevel: AccessLevel,
) = if (accessLevel == AccessLevel.READ) {
	dataOwnersWithExplicitAccess.keys
} else {
	dataOwnersWithExplicitAccess.filterValues { it == AccessLevel.WRITE }.keys
}
