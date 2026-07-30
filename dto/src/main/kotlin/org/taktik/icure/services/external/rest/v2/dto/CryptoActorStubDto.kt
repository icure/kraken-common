package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEntryKeyStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

/**
 * Holds only data specific for crypto actors without any additional information (from patient, hcparty, device).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubDto(
	override val id: String,
	@param:Schema(required = true) override val rev: String, // Stubs can't be created, but only updated or retrieved: rev is never null.
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	override val aesExchangeKeys: Map<AesExchangeKeyEntryKeyStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(),
	override val publicKey: SpkiHexStringDto? = null,
	@param:Schema(required = true) override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto>,
	@Deprecated("Use dataOwnerGroups with a DataOwnerGroupLinkTypeDto.parent link instead")
	override val parentId: String? = null,
	@ActiveField
	override val dataOwnerGroups: List<DataOwnerGroupLinkDto> = emptyList(),
	override val cryptoActorProperties: Set<PropertyStubDto>? = null,
) : VersionableDto<String>,
	CryptoActorDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	): CryptoActorStubDto = copy(id = id ?: this.id, rev = rev)
}
