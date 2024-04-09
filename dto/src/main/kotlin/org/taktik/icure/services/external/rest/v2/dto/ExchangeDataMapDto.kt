package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.SecureDelegationKeyStringDto

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """Required data that link the public keys of a data owner to their exchange data.""")
data class ExchangeDataMapDto(
    override val id: String,
    override val rev: String? = null,
    @get:Schema(description = """
        A map where each key is the fingerprint of a public key and the value is an exchange data id, encrypted with the private key corresponding to that public key.
    """)
    val encryptedExchangeDataIds: Map<KeypairFingerprintV2StringDto, Base64StringDto> = emptyMap(),
    override val deletionDate: Long? = null,
): StoredDocumentDto {
    override fun withDeletionDate(deletionDate: Long?): ExchangeDataMapDto = copy(deletionDate = deletionDate)

    override fun withIdRev(id: String?, rev: String): VersionableDto<String> =
        id?.let { copy(id = it, rev = rev) } ?: copy(rev = rev)
}
