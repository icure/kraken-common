package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """Required data that link the public keys of a data owner to their exchange data.""")
/**
 * Links the public keys of a data owner to their exchange data. This entity associates secure delegation keys
 * to the encrypted id of the exchange data used for the creation of the secure delegation.
 */
data class ExchangeDataMapDto(
	/** The id of this entity, which is the Secure Delegation Key. */
	override val id: String,
	/** The revision of the exchange data map in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	@param:Schema(
		description = """
        A map where each key is the fingerprint of a public key and the value is an exchange data id, encrypted with the private key corresponding to that public key.
    """,
	)
	/** A map where each key is a public key fingerprint and the value is an encrypted exchange data id. */
	val encryptedExchangeDataIds: Map<KeypairFingerprintV2StringDto, Base64StringDto> = emptyMap(),
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
) : StoredDocumentDto {
	override fun withDeletionDate(deletionDate: Long?): ExchangeDataMapDto = copy(deletionDate = deletionDate)

	override fun withIdRev(
		id: String?,
		rev: String,
	): VersionableDto<String> = id?.let { copy(id = it, rev = rev) } ?: copy(rev = rev)
}
