/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import org.taktik.couchdb.entity.Versionable
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.utils.MergeUtil.mergeMapsOfListsDistinct

/**
 * @property hcPartyKeys For each couple of HcParties (delegator and delegate), this map contains the exchange AES key. The delegator is always this hcp, the key of the map is the id of the delegate. The AES exchange key is encrypted using RSA twice : once using this hcp public key (index 0 in the Array) and once using the other hcp public key (index 1 in the Array). For a pair of HcParties. Each HcParty always has one AES exchange key for himself.
 * @property privateKeyShamirPartitions The privateKeyShamirPartitions are used to share this hcp's private RSA key with a series of other hcParties using Shamir's algorithm. The key of the map is the hcp Id with whom this partition has been shared. The value is \"threshold|partition in hex\" encrypted using the the partition's holder's public RSA key
 * @property publicKey The public key of this actor
 * @property publicKeysForOaepWithSha256 The public keys of this actor which should be used for RSA-OAEP with sha256 encryption
 */
interface CryptoActor {
	// One AES key per HcParty, encrypted using this hcParty public key and the other hcParty public key
	// For a pair of HcParties, this key is called the AES exchange key
	// Each HcParty always has one AES exchange key for himself
	// The map's keys are the delegate id.
	// In the table, we get at the first position: the key encrypted using owner (this)'s public key and in 2nd pos.
	// the key encrypted using delegate's public key.
	val hcPartyKeys: Map<String, List<String>>

	// Extra AES exchange keys, usually the ones we lost access to at some point
	// The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }
	val aesExchangeKeys: Map<String, Map<String, Map<String, String>>>

	// Our private keys encrypted with our public keys
	// The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }
	val transferKeys: Map<String, Map<String, String>>

	val privateKeyShamirPartitions: Map<String, String> //Format is hcpId of key that has been partitioned : "threshold|partition in hex"
	val publicKey: String?

	// The public keys stored in this set must be used only for RSA-OAEP with Sha-256 encryption. (Instead, the one contained in the publicKey and
	// aesExchangeKey field must be used for RSA-OAEP with Sha-1 and are considered legacy starting from v8 of the SDK).
	val publicKeysForOaepWithSha256: Set<String>

	val parentId: String?

	fun solveConflictsWith(other: CryptoActor): Map<String, Any?> {
		return mapOf(
			"hcPartyKeys" to mergeMapsOfListsDistinct(this.hcPartyKeys, other.hcPartyKeys),
			"privateKeyShamirPartitions" to (other.privateKeyShamirPartitions + this.privateKeyShamirPartitions),
			"publicKey" to (this.publicKey ?: other.publicKey),
			"aesExchangeKeys" to (other.aesExchangeKeys + this.aesExchangeKeys),
			"transferKeys" to (other.transferKeys + this.transferKeys),
		)
	}
}

/**
 * Checks if the [CryptoActor] content of two entities is the same (regardless of any additional content from the
 * concrete entity). Only considers data necessary for cryptographic operations, including:
 * - [CryptoActor.hcPartyKeys]
 * - [CryptoActor.privateKeyShamirPartitions]
 * - [CryptoActor.publicKey]
 * - [CryptoActor.aesExchangeKeys]
 * - [CryptoActor.transferKeys]
 * - [CryptoActor.publicKeysForOaepWithSha256]
 * Excludes any other content.
 * @return if the [CryptoActor] content of this and [other] are the same.
 */
fun CryptoActor.cryptoActorCryptographicDataEquals(other: CryptoActor) =
	this.hcPartyKeys == other.hcPartyKeys &&
	this.privateKeyShamirPartitions == other.privateKeyShamirPartitions &&
	this.publicKey == other.publicKey &&
	this.aesExchangeKeys == other.aesExchangeKeys &&
	this.transferKeys == other.transferKeys &&
	this.publicKeysForOaepWithSha256 == other.publicKeysForOaepWithSha256

/**
 * Converts this [CryptoActor] to a [CryptoActorStub]. If the rev is null, this will return null, since stubs can't be
 * used for non-stored entities.
 * @return a [CryptoActorStub] with the same crypto-actor content as this [CryptoActor].
 */
fun <T> T.asCryptoActorStub(): CryptoActorStub? where T : CryptoActor, T : Versionable<String>, T : HasTags =
	if (this is CryptoActorStub) this else this.rev?.let { rev ->
		CryptoActorStub(
			id = this.id,
			rev = rev,
			hcPartyKeys = this.hcPartyKeys,
			privateKeyShamirPartitions = this.privateKeyShamirPartitions,
			publicKey = this.publicKey,
			aesExchangeKeys = this.aesExchangeKeys,
			transferKeys = this.transferKeys,
			publicKeysForOaepWithSha256 = this.publicKeysForOaepWithSha256,
			tags = this.tags,
			parentId = this.parentId
		)
	}