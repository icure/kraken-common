/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.PropertyStubDto

interface CryptoActorDto : VersionableDto<String> {
	@get:Schema(
		description =
		"For each couple of HcParties (delegator and delegate), this map contains the exchange AES key. The delegator is always this hcp, the key of the map is the id of the delegate." +
			"The AES exchange key is encrypted using RSA twice : once using this hcp public key (index 0 in the Array) and once using the other hcp public key (index 1 in the Array). For a pair of HcParties. Each HcParty always has one AES exchange key for himself.",
	)
	val hcPartyKeys: Map<String, List<String>>

	@get:Schema(
		description = "Extra AES exchange keys, usually the ones we lost access to at some point. The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }",
	)
	val aesExchangeKeys: Map<String, Map<String, Map<String, String>>>

	@get:Schema(
		description = "Our private keys encrypted with our public keys. The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }",
	)
	val transferKeys: Map<String, Map<String, String>>

	@get:Schema(
		description = "The privateKeyShamirPartitions are used to share this hcp's private RSA key with a series of other hcParties using Shamir's algorithm. The key of the map is the hcp Id with whom this partition has been shared. The value is \"threshold⎮partition in hex\" encrypted using the the partition's holder's public RSA key",
	)
	val privateKeyShamirPartitions: Map<String, String>

	@get:Schema(description = "The public key of this hcp")
	val publicKey: String?

	@get:Schema(description = "The public keys of this actor that are generates using the OAEP Sha-256 standard")
	val publicKeysForOaepWithSha256: Set<String>

	@get:Schema(
		description = "The id of the parent data owner. When using hierarchical data owners permissions a data owner is allowed to access data shared with their parent",
	)
	val parentId: String?

	@get:Schema(
		description =
		"A set of PropertyStub associated to this CryptoActor. They are not supposed to be encrypted if" +
			"the concrete implementation of this interface is Encryptable and so they must not contain any sensitive information.",
	)
	val cryptoActorProperties: Set<PropertyStubDto>?
}
