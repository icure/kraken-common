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
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.dto.annotations.filtering.HandledByMapper
import org.taktik.icure.services.external.rest.v2.dto.PropertyStubDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

/**
 * Interface for entities that participate in the iCure end-to-end encryption system.
 * A CryptoActor holds the cryptographic keys and key exchange material needed for secure data sharing.
 */
interface CryptoActorDto : VersionableDto<String> {
	@get:Schema(
		description =
		"For each couple of HcParties (delegator and delegate), this map contains the exchange AES key. The delegator is always this hcp, the key of the map is the id of the delegate. " +
			"The AES exchange key is encrypted using RSA twice : once using this hcp public key (index 0 in the Array) and once using the other hcp public key (index 1 in the Array). For a pair of HcParties. Each HcParty always has one AES exchange key for himself.",
	)
	@ActiveField val hcPartyKeys: Map<String, List<HexStringDto>>

	@get:Schema(
		description = "Extra AES exchange keys, usually the ones we lost access to at some point. The structure is { publicKey: { delegateId: { myPubKey1: aesExKey_for_this, delegatePubKey1: aesExKey_for_delegate } } }",
	)
	@ActiveField val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>>

	@get:Schema(
		description = "Our private keys encrypted with our public keys. The structure is { publicKey1: { publicKey2: privateKey2_encrypted_with_publicKey1, publicKey3: privateKey3_encrypted_with_publicKey1 } }",
	)
	@ActiveField val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>

	@get:Schema(
		description = "The privateKeyShamirPartitions are used to share this hcp's private RSA key with a series of other hcParties using Shamir's algorithm. The key of the map is the hcp Id with whom this partition has been shared. The value is \"threshold⎮partition in hex\" encrypted using the the partition's holder's public RSA key",
	)
	@ActiveField val privateKeyShamirPartitions: Map<String, HexStringDto>

	@get:Schema(description = "The public key of this hcp")
	@ActiveField val publicKey: SpkiHexStringDto?

	@get:Schema(description = "The public keys of this actor that are generates using the OAEP Sha-256 standard")
	@ActiveField val publicKeysForOaepWithSha256: Set<SpkiHexStringDto>

	@get:Schema(
		description = "The id of the parent data owner. When using hierarchical data owners permissions a data owner is allowed to access data shared with their parent",
	)
	@Deprecated("Use dataOwnerGroups with a DataOwnerGroupLinkTypeDto.parent link instead")
	@HandledByMapper
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
	@ActiveField val dataOwnerGroups: List<DataOwnerGroupLinkDto>

	@get:Schema(
		description = "The type any incoming link pointing at this data owner must have. Always null unless explicitly " +
			"set at creation; once set (or once relied upon while null) it can never be changed.",
	)
	@ActiveField val groupLinkType: DataOwnerGroupLinkTypeDto?

	@get:Schema(
		description = "A set of PropertyStub associated to this CryptoActor, that you can use to support the implementation of custom crypto strategies. Note that this properties are publicly visible to all users and must not contain any sensitive data.",
	)
	@AlwaysDecrypted
	@ActiveField val cryptoActorProperties: Set<PropertyStubDto>?
}
