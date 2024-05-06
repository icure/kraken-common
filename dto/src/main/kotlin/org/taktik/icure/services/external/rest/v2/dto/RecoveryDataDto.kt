package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RecoveryDataDto(
    override val id: String,
    override val rev: String? = null,
    val recipient: String,
    val encryptedSelf: Base64StringDto,
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
