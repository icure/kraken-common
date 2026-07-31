/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.mergers.annotations.MergeStrategyUse

/**
 * @property hcPartyKeys For each couple of HcParties (delegator and delegate), this map contains the exchange AES key. The delegator is always this hcp, the key of the map is the id of the delegate. The AES exchange key is encrypted using RSA twice : once using this hcp public key (index 0 in the Array) and once using the other hcp public key (index 1 in the Array). For a pair of HcParties. Each HcParty always has one AES exchange key for himself.
 * @property privateKeyShamirPartitions The privateKeyShamirPartitions are used to share this hcp's private RSA key with a series of other hcParties using Shamir's algorithm. The key of the map is the hcp Id with whom this partition has been shared. The value is \"threshold|partition in hex\" encrypted using the the partition's holder's public RSA key
 * @property publicKey The public key of this actor
 * @property publicKeysForOaepWithSha256 The public keys of this actor which should be used for RSA-OAEP with sha256 encryption
 * @property dataOwnerGroups The links to the HealthcareParties that are used to represent organizations, administrative units or loose groups of hcps that need to easily share information with each others. Those HealthcareParties usually have public keys and associated private keys as they are legitimate targets for SecureDelegations.
 * Group membership is transitive, whatever the link type: if this actor is linked to a group A and A is itself linked
 * to a group B, then this actor also belongs to B. Resolving the full set of groups of an actor therefore requires
 * following those links recursively.
 * @property cryptoActorProperties a set of [PropertyStub] associated to this [CryptoActor]. They are not supposed to be encrypted if
 * the concrete implementation of this interface is Encryptable and so they must not contain any sensitive information.
 */
interface CryptoActor {
	// One AES key per HcParty, encrypted using this hcParty public key and the other hcParty public key
	// For a pair of HcParties, this key is called the AES exchange key
	// Each HcParty always has one AES exchange key for himself
	// The map's keys are the delegate id.
	// In the table, we get at the first position: the key encrypted using owner (this)'s public key and in 2nd pos.
	// the key encrypted using delegate's public key.
	@MergeStrategyUse(
		canMerge = "canMergeMap({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		merge = "{{LEFT}}.{{PROP}} + {{RIGHT}}.{{PROP}}",
	)
	val hcPartyKeys: Map<String, List<String>>

	// Extra AES exchange keys, usually the ones we lost access to at some point
	// The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }
	@MergeStrategyUse(
		canMerge = "canMergeAesExchangeKeys({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		merge = "mergeAesExchangeKeys({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
	)
	val aesExchangeKeys: Map<String, Map<String, Map<String, String>>>

	// Our private keys encrypted with our public keys
	// The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeMapsOfMergeable({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}}) { leftKeys, rightKeys -> rightKeys + leftKeys }",
	)
	val transferKeys: Map<String, Map<String, String>>

	@MergeStrategyUse(
		canMerge = "({{LEFT}}.{{PROP}}.keys.containsAll({{RIGHT}}.{{PROP}}.keys) || {{RIGHT}}.{{PROP}}.keys.containsAll({{LEFT}}.{{PROP}}.keys))",
		merge = "if({{LEFT}}.{{PROP}}.keys.size >= {{RIGHT}}.{{PROP}}.keys.size) {{LEFT}}.{{PROP}} else {{RIGHT}}.{{PROP}}",
	)
	val privateKeyShamirPartitions: Map<String, String> // Format is hcpId of key that has been partitioned : "threshold|partition in hex"
	val publicKey: String?

	// The public keys stored in this set must be used only for RSA-OAEP with Sha-256 encryption. (Instead, the one contained in the publicKey and
	// aesExchangeKey field must be used for RSA-OAEP with Sha-1 and are considered legacy starting from v8 of the SDK).
	val publicKeysForOaepWithSha256: Set<String>

	@Deprecated("Use dataOwnerGroups with a DataOwnerGroupLinkType.parent link instead")
	val parentId: String?

	/**
	 * The links to the data owners representing the organizations, administrative units or other loose groups of
	 * healthcare parties this crypto actor belongs to.
	 * There are different types of links, which have different implication on access control and requirements for
	 * sharing data among all members of the group.
	 *
	 * This list should be considered as unordered, and it may not contain two links pointing to the same data owner,
	 * regardless of type.
	 *
	 * # Membership propagation
	 *
	 * All links are transitive, whatever their type: every directly linked group is a group of the actor, and the groups
	 * of a group are also groups of the actor (applied recursively). An actor is therefore a member of every group
	 * reachable through a path of links.
	 *
	 * For example with `hcp -parent-> department -simple-> building -simple-> campus`: the groups of `hcp` are
	 * `department`, `building` and `campus`.
	 *
	 * There may however be restrictions in place on how propagation when the link type changes: propagation from a
	 * parent link to a simple link is allowed, but the opposite is not.
	 * A group membership such as `hcp -parent-> department -simple-> building -parent-> campus` is not allowed: while
	 * the relationships of `building` are technically valid the full membership for `department` or `hcp` is invalid,
	 * and a user associated with a data owner that has invalid membership is not allowed to login.
	 *
	 * # Why groups instead of direct sharing with members
	 *
	 * By using groups of data owners instead of directly sharing data with each data owner in a group you gain two
	 * major advantages:
	 * - Reduced size of metadata on entities
	 * - Possibility of dynamically adding peoples to a group without having to update all the entities that they should
	 *   be able to access
	 *
	 * In a full-scale system where data is massively shared between groups of users using data owner groups is the
	 * only realistic choice available.
	 */
	val dataOwnerGroups: List<DataOwnerGroupLink>

	val cryptoActorProperties: Set<PropertyStub>?

	companion object {
		/**
		 * Validates the [dataOwnerGroups] links of a crypto actor together with its legacy [parentId]:
		 * - A data owner id must appear at most once in [dataOwnerGroups], regardless of link type: linking to the
		 *   same group both as a `parent` and as an `other` (or twice with the same type) is never meaningful, since
		 *   membership/rights are granted per target, not per (target, type) pair.
		 * - If [parentId] is not null and [dataOwnerGroups] also has an entry for that same data owner id (which is
		 *   legal, see [org.taktik.icure.security.resolveHcpAncestors]), that entry must be a
		 *   [DataOwnerGroupLinkType.parent] link: a [parentId] paired with a differently-typed link to the same
		 *   target would be a contradiction (is it a parent or not?).
		 * @throws IllegalArgumentException if [dataOwnerGroups] contains more than one link with the same
		 * [DataOwnerGroupLink.dataOwnerId], or if it links the [parentId] data owner with a non-parent link type.
		 */
		fun validateDataOwnerGroupLinks(dataOwnerGroups: List<DataOwnerGroupLink>, parentId: String?) {
			val duplicateIds = dataOwnerGroups
				.groupingBy { it.dataOwnerId }
				.eachCount()
				.filterValues { it > 1 }
				.keys
			require(duplicateIds.isEmpty()) {
				"Duplicate dataOwnerGroups link(s) for data owner id(s): ${duplicateIds.joinToString()}"
			}
			if (parentId != null) {
				val linkToParent = dataOwnerGroups.firstOrNull { it.dataOwnerId == parentId }
				require(linkToParent == null || linkToParent.linkType == DataOwnerGroupLinkType.parent) {
					"dataOwnerGroups has a link of type ${linkToParent?.linkType} to the legacy parentId $parentId, expected a link of type ${DataOwnerGroupLinkType.parent} or none"
				}
			}
		}
	}
}

/**
 * Converts this [CryptoActor] to a [CryptoActorStub]. If the rev is null, this will return null, since stubs can't be
 * used for non-stored entities.
 * @return a [CryptoActorStub] with the same crypto-actor content as this [CryptoActor].
 */
fun <T> T.asCryptoActorStub(): CryptoActorStub? where T : CryptoActor, T : Versionable<String> = if (this is CryptoActorStub) {
	this
} else {
	this.rev?.let { rev ->
		CryptoActorStub(
			id = this.id,
			rev = rev,
			hcPartyKeys = this.hcPartyKeys,
			privateKeyShamirPartitions = this.privateKeyShamirPartitions,
			publicKey = this.publicKey,
			aesExchangeKeys = this.aesExchangeKeys,
			transferKeys = this.transferKeys,
			publicKeysForOaepWithSha256 = this.publicKeysForOaepWithSha256,
			parentId = this.parentId,
			dataOwnerGroups = this.dataOwnerGroups,
			cryptoActorProperties = this.cryptoActorProperties,
		)
	}
}
