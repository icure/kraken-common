package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.KeypairFingerprintV2StringDto

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """Data necessary for the secure sharing of entities between data owners.""")
/**
 * Holds the cryptographic data necessary for the secure sharing of entities between data owners.
 * Exchange data enables a delegator to share encrypted medical data with a delegate.
 */
data class ExchangeDataDto(
	/** The Id of the exchange data. */
	override val id: String,
	/** The revision of the exchange data in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	@param:Schema(
		description = """ID of the data owner which created this exchange data, in order to share some data with the [delegate].""",
		required = true,
	)
	/** The id of the data owner who created this exchange data to share data with the delegate. */
	val delegator: String,
	@param:Schema(
		description = """ID of a data owner which can use this exchange data to access data shared with him by [delegator].""",
		required = true,
	)
	/** The id of the data owner who can use this exchange data to access shared data. */
	val delegate: String,
	@param:Schema(
		description = """Aes key to use for sharing data from the delegator to the delegate, encrypted with the public keys of both
delegate and delegator. This key should never be sent decrypted to the server, as it allows to read medical data.""",
		required = true,
	)
	/** AES key for sharing data, encrypted with the public keys of both delegate and delegator. */
	val exchangeKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(
		description = """Key used for access control to data shared from the delegator to the delegate, encrypted with the public keys of both
delegate and delegator.
This key will be used by the client to calculate the keys of [SecurityMetadata.secureDelegations] in
[HasSecureDelegationsAccessControl.securityMetadata] which allows to implement a form of access control where the
identity of data owners with access to a specific entity can't be deduced from the database alone. This is useful
for example to allow patients to access their medical data without creating a deducible link between the patient
and the medical data in the database.
There are no strict requirements on how the client should use this secret to create the security metadata key,
but for authentication the client must be able to provide a 128 bit long access control key (see
[DataOwnerAuthenticationDetails.accessControlKeys]) which once hashed using sha256 will give the key of the
security metadata.
However, in order to avoid introducing undesired links between entities which could be detrimental to the
patients privacy the access control keys should be created also using information on the entity class and secret
foreign keys of the entity holding the delegation, in order to ensure that in case of different confidentiality
settings for the entity the security metadata key will also be different and won't leak information on links
between data.
```
accessControlKey = sha256Bytes(accessControlSecret + entityClass + sfk[0]).take(16)
securityMetadataKey = sha256Hex(accessControlKey)
```""",
		required = true,
	)
	/** Key used for access control, encrypted with the public keys of both delegate and delegator. */
	val accessControlSecret: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(
		description = """Signature to ensure the key data has not been tampered with by third parties (any actor without access to the
keypair of the delegator/delegate): when creating new exchange data the delegator will create a new hmac key and
sign it with his own private key.
This field will contain the signature by fingerprint of the public key to use for verification.""",
		required = true,
	)
	/** Signature by the delegator to ensure key data has not been tampered with by third parties. */
	val delegatorSignature: Map<KeypairFingerprintV2StringDto, Base64StringDto> = emptyMap(),
	@param:Schema(
		description = """Encrypted signature key (hmac-sha256) shared between delegate and delegator, to allow either of them to modify
the exchange data, without voiding the authenticity guarantee.""",
		required = true,
	)
	/** Encrypted HMAC-SHA256 signature key shared between delegate and delegator. */
	val sharedSignatureKey: Map<KeypairFingerprintV2StringDto, Base64StringDto>,
	@param:Schema(
		description = """Base 64 signature of the exchange data, to ensure it was not tampered by third parties. This signature validates:
- The (decrypted) exchange key
- The (decrypted) access control secret
- The delegator and delegates being part of the exchange data
- The public keys used in the exchange data (allows to consider them as verified in a second moment).""",
		required = true,
	)
	/** Base64 signature of the exchange data to ensure it was not tampered by third parties. */
	val sharedSignature: Base64StringDto,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
) : StoredDocumentDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	): ExchangeDataDto = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?): ExchangeDataDto = this.copy(deletionDate = deletionDate)
}
