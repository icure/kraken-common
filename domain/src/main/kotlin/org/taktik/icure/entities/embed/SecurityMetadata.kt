package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.utils.Sha256HexString
import org.taktik.icure.exceptions.MergeConflictException
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.utils.DirectedGraphMap
import org.taktik.icure.utils.hasLoops
import java.io.Serializable

/**
 * Holds information for user-based access control and encryption of entities.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecurityMetadata(
    /**
     * This maps the hex-encoded sha256 hash of a key created by the client using a certain [ExchangeData.accessControlSecret] to the
     * [SecureDelegation] for the corresponding delegate-delegator pair. This hash is used by the server to perform access control for
     * anonymous data owners (see [DataOwnerAuthenticationDetails]) and in some cases also by the sdks to quickly find the appropriate
     * exchange key needed for the decryption of the content of the corresponding [SecureDelegation].
     *
     * Note that it is also possible for a secure delegation in this map to have no entry for secretId, encryptionKey or owningEntityId.
     * This could happen in situations where a user should have access only to the unencrypted content of an entity.
     */
    val secureDelegations: Map<Sha256HexString, SecureDelegation>
): Serializable {
    init {
        require(secureDelegations.isNotEmpty()) { "Security metadata should contain at least an entry for delegations" }
        require(!secureDelegations.parentsGraph.hasLoops()) { "Secure delegations graph must not have any loops" }
    }

    /**
     * Merges the security metadata of two versions of the same entity (same id).
     * NOTE: not suitable for the merging of metadata of duplicate entities (e.g. same person existing as two different
     * patient entities): use [mergeForDuplicatedEntityIntoThisFrom] instead.
     * This method is merges equivalent secure delegations into a single one by doing the union of their content.
     * @param other the security metadata of the other version of the entity.
     * @return the merged security metadata.
     */
    fun mergeForDifferentVersionsOfEntity(other: SecurityMetadata): SecurityMetadata = merge(
		other = other,
		mergeVersions = true,
		omitEncryptionKeysOfOther = false
	)

    /**
     * Merges the security metadata of duplicated entities (e.g. different patient entities representing the same
     * person).
     * NOTE: not suitable for the merging of metadata of different versions of the same entity (same id, different
     * revision history): use [mergeForDifferentVersionsOfEntity] instead.
     * The main differences with [mergeForDifferentVersionsOfEntity] are:
     * - This method is not commutative
     * - The encrypted encryption keys from delegations of [other] can be omitted.
     * - The parents of equivalent delegations are merged as follows:
     *   - If a delegation is root in this and/or other it will be a root delegation in the merged metadata as
     *     well (potentially removing links which exist in one of the delegations).
     *   - If a delegation is not a root in neither this/other the new parents will be the union of the parents of the
     *   two delegations (potentially updating the parents to a new canonical delegation key).
     * @param other the security metadata of the duplicated entity.
     * @return a new security metadata being the result of the merging.
     */
    fun mergeForDuplicatedEntityIntoThisFrom(
        other: SecurityMetadata,
        omitEncryptionKeysOfOther: Boolean
    ): SecurityMetadata = merge(
		other = other,
		mergeVersions = false,
		omitEncryptionKeysOfOther = omitEncryptionKeysOfOther
	)

    private fun merge(
        other: SecurityMetadata,
        mergeVersions: Boolean,
        omitEncryptionKeysOfOther: Boolean
    ): SecurityMetadata {
        // Find duplicate delegations and merge
        val mergedDelegations = (this.secureDelegations.keys + other.secureDelegations.keys).associateWith { canonicalKey ->
            val thisDelegation = this.secureDelegations[canonicalKey]
            val otherDelegation = other.secureDelegations[canonicalKey]
            if (thisDelegation != null && otherDelegation != null)
                mergeSecDels(
					thisDelegation = thisDelegation,
					otherDelegation = otherDelegation,
					mergeVersions = mergeVersions,
					omitEncryptionKeysOfOther = omitEncryptionKeysOfOther
				)
            else
                checkNotNull(thisDelegation ?: otherDelegation?.copy(encryptionKeys = emptySet())) {
                    "At least one of the delegations should have been not null"
                }
        }
        return SecurityMetadata(
            secureDelegations = mergedDelegations,
        )
    }

    private fun mergeSecDels(
        thisDelegation: SecureDelegation,
        otherDelegation: SecureDelegation,
        mergeVersions: Boolean,
        omitEncryptionKeysOfOther: Boolean
    ): SecureDelegation {
        if (
            thisDelegation.delegator != otherDelegation.delegator
            || thisDelegation.delegate != otherDelegation.delegate
            || thisDelegation.exchangeDataId != otherDelegation.exchangeDataId
        ) throw MergeConflictException(
            "Can't merge secure delegations referring to different delegator, delegate or exchange data id"
        )
        return SecureDelegation(
            delegator = thisDelegation.delegator,
            delegate = thisDelegation.delegate,
            secretIds = thisDelegation.secretIds + otherDelegation.secretIds,
            encryptionKeys = if (omitEncryptionKeysOfOther) {
                thisDelegation.encryptionKeys
            } else {
                thisDelegation.encryptionKeys + otherDelegation.encryptionKeys
            },
            owningEntityIds = thisDelegation.owningEntityIds + otherDelegation.owningEntityIds,
            parentDelegations = if (mergeVersions) {
                thisDelegation.parentDelegations + otherDelegation.parentDelegations
            } else {
                if (thisDelegation.parentDelegations.isEmpty() || otherDelegation.parentDelegations.isEmpty())
                    emptySet()
                else
                    thisDelegation.parentDelegations + otherDelegation.parentDelegations
            },
            exchangeDataId = thisDelegation.exchangeDataId,
            // Without fine-grained permissions this is fine
            permissions = if (thisDelegation.permissions == AccessLevel.WRITE) AccessLevel.WRITE else otherDelegation.permissions
        )
    }
}

val Map<Sha256HexString, SecureDelegation>.parentsGraph: DirectedGraphMap<String> get() =
    this.mapValues { (_, delegation) -> delegation.parentDelegations }
