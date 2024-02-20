package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.HexString
import org.taktik.icure.entities.utils.KeypairFingerprintString

/**
 * Holds parameters necessary to share an entity.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class EntityShareRequestDto(
    /**
     * Id of the data owner which is sharing the entity (delegator), if it should be explicitly indicated or null if the
     * delegator requires anonymous delegations. If not null this must match the logged data owner id.
     */
    // We can easily infer the delegator id server-side, but the decision of whether to include it or not in the secure
    // delegation is done on client-side.
    val explicitDelegator: String? = null,
    /**
     * Id of the data owner which will gain access to the entity (delegate), if it should be explicitly indicated or
     * null if the delegate requires anonymous delegations.
     */
    val explicitDelegate: String? = null,
    /**
     * Values generated using the access control secret of the exchange data used for the encryption of the ids and keys
     * to share. Once hashed they are used as secure delegation keys.
     */
    val accessControlKeys: Set<HexString>,
    /**
     * Encrypted secret ids to share with the delegate.
     */
    val secretIds: Set<Base64String> = emptySet(),
    /**
     * Encrypted encryption keys to share with the delegate.
     */
    val encryptionKeys: Set<Base64String> = emptySet(),
    /**
     * Encrypted owning entity ids to share with the delegate.
     */
    val owningEntityIds: Set<Base64String> = emptySet(),
    /**
     * Id of the exchange data used for the encryption of the ids and keys to share. Must be null at least one of
     * delegator or delegate is not explicit.
     */
    val exchangeDataId: String? = null,
    /**
     * Must be non-empty if exactly one of delegator or delegate is explicit and the other is not, empty in all other
     * cases.
     */
    val encryptedExchangeDataId: Map<KeypairFingerprintString, Base64String> = emptyMap(),
    /**
     * Permissions for the delegate.
     */
    val requestedPermissions: RequestedPermissionDto = RequestedPermissionDto.MAX_WRITE
) {
    init {
        require(accessControlKeys.isNotEmpty()) {
            "`accessControlKeys` can't be empty"
        }
    }

    /**
     * Strategy to use for the calculation of permissions for the new [SecureDelegation.permissions]
     */
    enum class RequestedPermissionDto {
        /**
         * The new secure delegation will give maximum access to the delegate, depending on the rights of the delegator.
         * This is currently equivalent to [FULL_READ], but with the introduction of fine-grained access control this
         * would behave more similar to [FULL_WRITE].
         */
        MAX_READ,

        /**
         * The new secure delegation will give full-read access to the delegate.
         *
         * With the introduction of fine-grained access control a user may have limited read access to an entity, and
         * in such case the request would fail (similarly to [FULL_WRITE]).
         */
        FULL_READ,

        /**
         * The new secure delegation will give maximum access to the delegate, depending on the rights of the delegator.
         * If the delegator has full-write access the delegate will also have full-write access else the delegate will
         * have full-read access.
         */
        MAX_WRITE,

        /**
         * The new secure delegation will give full-write access to the delegate. If the delegator does not have
         * full-write access to the entity the request will fail.
         */
        FULL_WRITE,

        /**
         * Request to create a root delegation on the entity. Usually new entities are created with a root delegation
         * for the creator data owner and no other data owners will be able to obtain root permissions, but there are
         * some situations where other data owners can create root delegations on existing entities:
         * - If a data owner has a legacy delegation on an entity he can create a root delegation. This is necessary in
         * cases where the data owner wants to share an entity with another data owner using the new delegation format
         * but does not have a delegation in the new format yet (the data owner creates a new root delegation self->self
         * and then creates a delegation self->other).
         * - A patient data owner is always allowed to create a root delegation for himself
         *
         * A root delegation gives full write permissions to the data owners which can access it (usually a root
         * delegation should be accessible only for one data owner, it should be a delegation a->a) and does not depend
         * on any other delegation: this means that no data owners except for the data owners with the root permission
         * can revoke it.
         */
        ROOT
    }
}
