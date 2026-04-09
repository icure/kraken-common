package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents metadata which allows a data owner to recover cryptographic secrets meant for them.
 * The id of recovery data should be derived from the encryption key the data was encrypted with,
 * so that only the encryption key is needed to find and use the recovery data.
 */
data class RecoveryDataDto(
	/** The unique identifier of the recovery data, derived from the encryption key. */
	override val id: String,
	/** The revision of the recovery data in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Id of the data owner that this recovery data is meant for */
	@param:Schema(required = true) val recipient: String,
	/** Encrypted recovery data. The structure of the decrypted data depends on the [type] of the recovery data. */
	@param:Schema(required = true) val encryptedSelf: Base64StringDto,
	/** Type of the recovery data. */
	@param:Schema(required = true) val type: Type,
	/** Timestamp (unix epoch in ms) at which this recovery data will expire. If null, this recovery data will never expire. Negative values or zero mean the data is already expired. */
	val expirationInstant: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
) : StoredDocumentDto {
	enum class Type {
		KEYPAIR_RECOVERY,
		EXCHANGE_KEY_RECOVERY,
	}

	override fun withDeletionDate(deletionDate: Long?): RecoveryDataDto = copy(deletionDate = deletionDate)

	override fun withIdRev(
		id: String?,
		rev: String,
	): VersionableDto<String> = id?.let { copy(id = it, rev = rev) } ?: copy(rev = rev)
}
