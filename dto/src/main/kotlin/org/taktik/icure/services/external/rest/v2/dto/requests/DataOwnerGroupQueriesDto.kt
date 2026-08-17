package org.taktik.icure.services.external.rest.v2.dto.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkTypeDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

/**
 * A data owner that declares a direct link to one of the queried data owner groups, through the legacy
 * `parentId` or a `dataOwnerGroups` entry.
 *
 * Group membership is transitive, but this is **not**: only the data owners declaring a link to a queried group
 * are returned, never the data owners linked to *them*. Following the chain is up to the client, which has to
 * decide whether it wants to, based on [groupLinkType].
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class LinkedDataOwnerDto(
	@param:Schema(required = true)
	@ActiveField val dataOwnerId: String,
	/**
	 * The group link type of this data owner, that is the type any link pointing at *it* has. Omitted when it is
	 * the default for the type of data owner that was queried (`parent` for healthcare parties, `notAllowed` for
	 * patients and devices), which is the common case: an absent value is not a fourth type.
	 */
	@ActiveField val groupLinkType: DataOwnerGroupLinkTypeDto? = null,
)

/**
 * The public keys of a data owner, each with the encryption algorithm it must be used with.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class DataOwnerPublicKeysDto(
	@param:Schema(required = true)
	@ActiveField val dataOwnerId: String,
	@param:Schema(required = true)
	@ActiveField val publicKeys: List<PublicKeyInfoDto>,
)

/**
 * A public key of a data owner and the encryption algorithm it must be used with. A key appears at most once in a
 * [DataOwnerPublicKeysDto]: a keypair is generated for one scheme, so a key that a data owner happens to have
 * declared for both is reported as the sha256 one.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = false)
data class PublicKeyInfoDto(
	@param:Schema(required = true)
	@ActiveField val publicKey: SpkiHexStringDto,
	@param:Schema(required = true)
	@ActiveField val algorithm: RsaEncryptionAlgorithmDto,
)

/**
 * An algorithm a public key of a data owner may be used with.
 *
 * This is an enum rather than a "uses sha256" boolean so that a third scheme can be introduced without a
 * breaking change to the wire format.
 */
enum class RsaEncryptionAlgorithmDto {
	/**
	 * RSA-OAEP with sha1. The keys of `aesExchangeKeys` and `publicKey` use this algorithm; they are considered
	 * legacy starting from v8 of the SDK.
	 */
	OaepWithSha1,

	/**
	 * RSA-OAEP with sha256. The keys in `publicKeysForOaepWithSha256` use this algorithm.
	 */
	OaepWithSha256,
}
