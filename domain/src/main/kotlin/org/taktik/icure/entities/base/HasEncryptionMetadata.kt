/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets
import org.taktik.icure.mergers.annotations.MergeStrategyUseReference

interface HasEncryptionMetadata : HasSecureDelegationsAccessControl {
	/**
	 * The secretForeignKeys are filled at the to-many end of a one-to-many relationship (for example inside Contact for the Patient -> Contacts relationship).
	 * Used when we want to find all contacts for a specific patient.
	 * These keys are in clear. You can have several to partition the medical document space.
	 */
	val secretForeignKeys: Set<String>

	// Used when we want to find the patient for this contact
	// These keys are the public patient ids encrypted using the hcParty keys.

	/**
	 * The secretForeignKeys are filled at the to many end of a one to many relationship (for example inside Contact for the Patient -> Contacts relationship).
	 * Used when we want to find the patient for a specific contact. These keys are the encrypted id (using the hcParty key for the delegate) that can be found in clear inside the patient
	 * ids encrypted using the hcParty keys.
	 * With the introduction of [securityMetadata] sdks will stop adding new data to this field, and instead use the [securityMetadata], but the field may still be read
	 * and/or some of his content may be deleted.
	 */
	@MergeStrategyUseReference("org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets")
	val cryptedForeignKeys: Map<String, Set<Delegation>>

	// When a document is created, the responsible generates a cryptographically random master key (never to be used for something else than referencing from other entities)
	// He/she encrypts it using his own AES exchange key and stores it as a delegation
	// The responsible is thus always in the delegations as well

	/**
	 * When a document is created, the responsible generates a cryptographically random master key (never to be used for something else than referencing from other entities).
	 * He/she encrypts it using his own AES exchange key and stores it as a delegation. The responsible is thus always in the delegations as well.
	 * With the introduction of [securityMetadata] sdks will stop adding new data to this field, and instead use the [securityMetadata], but the field may still be read
	 * and/or some of his content may be deleted.
	 */
	@MergeStrategyUseReference("org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets")
	val delegations: Map<String, Set<Delegation>>

	// When a document needs to be encrypted, the responsible generates a cryptographically random master key (different from the delegation key, never to appear in clear anywhere in the db)
	// He/she encrypts it using his own AES exchange key and stores it as a delegation

	/**
	 * When a document needs to be encrypted, the responsible generates a cryptographically random master key (different from the delegation key, never to appear
	 * in clear anywhere in the db). He/she encrypts it using his own AES exchange key and stores it as a delegation.
	 * With the introduction of [securityMetadata] sdks will stop adding new data to this field, and instead use the [securityMetadata], but the field may still be read
	 * and/or some of his content may be deleted.
	 */
	@MergeStrategyUseReference("org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfSets")
	val encryptionKeys: Map<String, Set<Delegation>>

	fun solveConflictsWith(other: HasEncryptionMetadata): Map<String, Any?> = mapOf(
		"secretForeignKeys" to this.secretForeignKeys + other.secretForeignKeys,
		"cryptedForeignKeys" to mergeMapsOfSets(this.cryptedForeignKeys, other.cryptedForeignKeys),
		"delegations" to mergeMapsOfSets(this.delegations, other.delegations),
		"encryptionKeys" to mergeMapsOfSets(this.encryptionKeys, other.encryptionKeys),
		"securityMetadata" to (
			this.securityMetadata?.let { thisSecurityMetadata ->
				other.securityMetadata?.let { otherSecurityMetadata ->
					thisSecurityMetadata.mergeForDifferentVersionsOfEntity(otherSecurityMetadata)
				} ?: thisSecurityMetadata
			} ?: other.securityMetadata
		),
	)

	override val dataOwnersWithExplicitAccess: Map<String, AccessLevel>
		get() = super.dataOwnersWithExplicitAccess +
			(delegations.keys + delegations.values.flatMap { delegationsForDelegate -> delegationsForDelegate.mapNotNull { it.owner } })
				.associateWith { AccessLevel.WRITE }
}

/**
 * Specifies if a data owner can access this entity directly. This allows to verify both for explicit access or
 * 'anonymous' access in case the provided parameter is a delegation key. In this method the hierarchy of the data owner
 * (if any) is ignored: even if any of the data owner parents would have access to the entity this method still returns
 * false.
 * @param dataOwnerIdOrDelegationKey the id of the data owner or a secure delegation key
 * @return if the provided data owner or delegation key is in this entity.
 */
fun HasEncryptionMetadata.hasDataOwnerOrDelegationKey(dataOwnerIdOrDelegationKey: String): Boolean = delegations.containsKey(dataOwnerIdOrDelegationKey) ||
	securityMetadata?.let { metadata ->
		metadata.secureDelegations.containsKey(dataOwnerIdOrDelegationKey) ||
			metadata.secureDelegations.any { (_, delegation) ->
				delegation.delegator == dataOwnerIdOrDelegationKey || delegation.delegate == dataOwnerIdOrDelegationKey
			}
	} == true

/**
 * Checks if the metadata of two [HasEncryptionMetadata] entities is the same:
 * - [HasEncryptionMetadata.securityMetadata]
 * - [HasEncryptionMetadata.secretForeignKeys]
 * - [HasEncryptionMetadata.cryptedForeignKeys]
 * - [HasEncryptionMetadata.delegations]
 * - [HasEncryptionMetadata.encryptionKeys]
 * This comparison ignores the value of [HasEncryptionMetadata.encryptedSelf], since it is not metadata but actual entity content.
 * @return if the metadata of this and [other] are the same.
 */
fun HasEncryptionMetadata.encryptableMetadataEquals(other: HasEncryptionMetadata): Boolean {
	return this.securityMetadata == other.securityMetadata &&
		this.secretForeignKeys == other.secretForeignKeys &&
		this.cryptedForeignKeys == other.cryptedForeignKeys &&
		this.delegations == other.delegations &&
		this.encryptionKeys == other.encryptionKeys
	// encryptedSelf is not metadata
}
