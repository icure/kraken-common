package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.AesExchangeKeyEncryptionKeypairIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.HexStringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SpkiHexStringDto

/**
 * Holds only data specific for crypto actors without any additional information (from patient, hcparty, device).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubDto(
	override val id: String,
	override val rev: String, // Stubs can't be created, but only updated or retrieved: rev is never null.
	override val hcPartyKeys: Map<String, List<HexStringDto>> = emptyMap(),
	override val aesExchangeKeys: Map<SpkiHexStringDto, Map<String, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>>> = emptyMap(),
	override val transferKeys: Map<AesExchangeKeyEncryptionKeypairIdentifierDto, Map<AesExchangeKeyEncryptionKeypairIdentifierDto, HexStringDto>> = emptyMap(),
	override val privateKeyShamirPartitions: Map<String, HexStringDto> = emptyMap(),
	override val publicKey: SpkiHexStringDto? = null,
	override val publicKeysForOaepWithSha256: Set<SpkiHexStringDto>,
	override val tags: Set<CodeStubDto> = emptySet(),
) : VersionableDto<String>, CryptoActorDto, HasTagsDto {
    override fun withIdRev(id: String?, rev: String): CryptoActorStubDto =
        copy(id = id ?: this.id, rev = rev)
}
