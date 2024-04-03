package org.taktik.icure.entities.base

import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.SecurityMetadata

/**
 * Represents a special case of entities with user-based access control where the users with access to the entity may not be explicitly
 * indicated, and may instead be provided through secure delegations.
 * In these entities only the data owner which created the entity will be able to access it initially, but he/she can share the entity
 * with other data owners, so they will also gain access to the entity. These data owners in turn can share the entity with more data
 * owners, but they can never give an access level higher than their own.
 */
interface HasSecureDelegationsAccessControl : HasExplicitDataOwnerAccess {
    /**
     * Security metadata for the entity, contains metadata necessary for access control.
     * In [HasEncryptionMetadata] entities this is also used to store additional encrypted metadata on the entity, including encryption keys for the
     * [HasEncryptionMetadata.encryptedSelf] (replacing [HasEncryptionMetadata.encryptionKeys]), owning entity id (replacing [HasEncryptionMetadata.cryptedForeignKeys]),
     * and secret id (replacing the keys of [HasEncryptionMetadata.delegations]).
     */
    val securityMetadata: SecurityMetadata?

    override val dataOwnersWithExplicitAccess: Map<String, AccessLevel> get() =
        securityMetadata?.secureDelegations?.values
            ?.flatMap { delegation ->
                listOfNotNull(
                    delegation.delegate?.let { it to delegation.permissions },
                    delegation.delegator?.let { it to delegation.permissions }
                )
            }
            ?.groupBy { it.first }
            ?.mapValues { (_, v) -> if (v.any { it.second == AccessLevel.WRITE }) AccessLevel.WRITE else AccessLevel.READ }
            ?: emptyMap()
}
