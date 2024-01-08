package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.KeypairFingerprintV2String
import org.taktik.icure.entities.utils.Sha256HexString
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.VersionableDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RecoveryDataDto(
    override val id: String,
    override val rev: String? = null,
    val recipient: String,
    val encryptedSelf: String,
    val type: Type,
    val expirationInstant: Long? = null,
    override val deletionDate: Long? = null
): StoredDocumentDto {
    enum class Type {
        KEYPAIR_RECOVERY,
        EXCHANGE_KEY_RECOVERY
    }

    override fun withDeletionDate(deletionDate: Long?): RecoveryDataDto =
        copy(deletionDate = deletionDate)

    override fun withIdRev(id: String?, rev: String): VersionableDto<String> =
        id?.let { copy(id = it, rev = rev) } ?: copy(rev = rev)
}