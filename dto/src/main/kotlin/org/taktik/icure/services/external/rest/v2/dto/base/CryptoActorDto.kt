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

package org.taktik.icure.services.external.rest.v2.dto.base

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.AlwaysDecrypted
import org.taktik.icure.services.external.rest.v2.dto.PropertyStubDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

interface CryptoActorDto : VersionableDto<String> {
	@param:Schema(
		description =
		"For each couple of HcParties (delegator and delegate), this map contains the exchange AES key. The delegator is always this hcp, the key of the map is the id of the delegate. " +
			"The AES exchange key is encrypted using RSA twice : once using this hcp public key (index 0 in the Array) and once using the other hcp public key (index 1 in the Array). For a pair of HcParties. Each HcParty always has one AES exchange key for himself.",
	)
	val hcPartyKeys: Map<String, List<HexStringDto>>

	@param:Schema(
		description = "Extra AES exchange keys, usually the ones we lost access to at some point. The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }",
	)
	val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>>

	@param:Schema(
		description = "Our private keys encrypted with our public keys. The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }",
	)
	val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>

	@param:Schema(
		description = "The privateKeyShamirPartitions are used to share this hcp's private RSA key with a series of other hcParties using Shamir's algorithm. The key of the map is the hcp Id with whom this partition has been shared. The value is \"threshold⎮partition in hex\" encrypted using the the partition's holder's public RSA key",
	)
	val privateKeyShamirPartitions: Map<String, HexStringDto>

	@param:Schema(description = "The public key of this hcp")
	val publicKey: SpkiHexStringDto?

	@param:Schema(description = "The public keys of this actor that are generates using the OAEP Sha-256 standard")
	val publicKeysForOaepWithSha256: Set<SpkiHexStringDto>

	@param:Schema(
		description = "The id of the parent data owner. When using hierarchical data owners permissions a data owner is allowed to access data shared with their parent",
	)
	val parentId: String?

	@param:Schema(
		description = "A set of PropertyStub associated to this CryptoActor, that you can use to support the implementation of custom crypto strategies. Note that this properties are publicly visible to all users and must not contain any sensitive data.",
	)
	@AlwaysDecrypted
	val cryptoActorProperties: Set<PropertyStubDto>?
}
