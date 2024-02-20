package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto

/**
 * Holds only data specific for crypto actors without any additional information (from patient, hcparty, device).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CryptoActorStubDto(
    override val id: String,
    override val rev: String, // Stubs can't be created, but only updated or retrieved: rev is never null.
    override val hcPartyKeys: Map<String, List<String>> = emptyMap(),
    override val aesExchangeKeys: Map<String, Map<String, Map<String, String>>> = emptyMap(),
    override val transferKeys: Map<String, Map<String, String>> = emptyMap(),
    override val privateKeyShamirPartitions: Map<String, String> = emptyMap(),
    override val publicKey: String? = null,
    override val publicKeysForOaepWithSha256: Set<String>,
    override val tags: Set<CodeStubDto> = emptySet(),
) : VersionableDto<String>, CryptoActorDto, HasTagsDto {
    override fun withIdRev(id: String?, rev: String): CryptoActorStubDto =
        copy(id = id ?: this.id, rev = rev)
}
