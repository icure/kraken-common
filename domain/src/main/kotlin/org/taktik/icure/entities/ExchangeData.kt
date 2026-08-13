package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.HasExplicitDataOwnerAccess
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.KeypairFingerprintString
import org.taktik.icure.security.DataOwnerAuthenticationDetails

/**
 * Data necessary for the secure sharing of entities between data owners.
 *
 * # Exchange data for data owner groups
 *
 * ## Parent-type groups
 *
 * Each parent-type group must have a one or more keypairs that are known by all members of the group.
 *
 * When creating exchange data to a parent-type data owner group the delegator creates the exchange data with both
 * his own keys and with the group keypairs.
 *
 * ## Simple-type groups
 *
 * Simple-type data owner groups do not use a shared group keypair; instead, the complete exchange data for a group will
 * consist of many pieces of [ExchangeData] that share the same [delegator], [delegate], and [exchangeDataGroupId].
 *
 * Each piece will contain [exchangeKey], [accessControlSecret], and [sharedSignatureKey] that are only for the keypairs
 * of a specific member of the [delegate] group, as specified by [recipient].
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExchangeData(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	/**
	 * ID of the data owner which created this exchange data, in order to share some data with the [delegate].
	 * This can be a either a data owner or a parent-type data owner group.
	 * Simple-type data owner groups can never be delegators.
	 *
	 * For exchange data dedicated to inter-group sharing this might be a data owner reference (group/dataOwnerId)
	 * rather than a simple id.
	 */
	val delegator: String,
	/**
	 * ID of a data owner or data owner group which can use this exchange data to access encrypted data shared with him
	 * by [delegator].
	 *
	 * For exchange data dedicated to inter-group sharing this might be a data owner reference (group/dataOwnerId)
	 * rather than a simple id.
	 */
	val delegate: String,
	/**
	 * If the [delegate] is a data owner group, this is the id of a member in the group that is the recipient of this
	 * piece of the exchange data.
	 * In this piece the [exchangeKey], [accessControlSecret], and [sharedSignatureKey] have entries for, and only for,
	 * the keypairs of this recipient.
	 *
	 * For exchange data dedicated to inter-group sharing this might be a data owner reference (group/dataOwnerId)
	 * rather than a simple id.
	 *
	 * This is null exactly when [exchangeDataGroupId] is null: a piece of exchange data has a [recipient] if and only
	 * if it belongs to a simple-type group. Plain exchange data and the exchange data for parent-type groups always
	 * have both null.
	 */
	val recipient: String? = null,
	/**
	 * If this is a piece of exchange data for a simple-type group then this is a uuid shared between all the
	 * [ExchangeData] pieces for that group; null otherwise.
	 *
	 * This is null exactly when [recipient] is null: see [recipient] for the exchange data that has neither.
	 */
	val exchangeDataGroupId: String? = null,
	/**
	 * Aes key to use for sharing data from the delegator to the delegate, encrypted with the public keys of both
	 * delegate and delegator. This key should never be sent decrypted to the server, as it allows to read medical data.
	 */
	val exchangeKey: Map<KeypairFingerprintString, Base64String>,
	/**
	 * Key used for access control to data shared from the delegator to the delegate, encrypted with the public keys of both
	 * delegate and delegator.
	 *
	 * This key will be used by the client to calculate the keys of [SecurityMetadata.secureDelegations] in
	 * [HasSecureDelegationsAccessControl.securityMetadata] which allows to implement a form of access control where the
	 * identity of data owners with access to a specific entity can't be deduced from the database alone. This is useful
	 * for example to allow patients to access their medical data without creating a deducible link between the patient
	 * and the medical data in the database.
	 *
	 * There are no strict requirements on how the client should use this secret to create the security metadata key,
	 * but for authentication the client must be able to provide a 128 bit long access control key (see
	 * [DataOwnerAuthenticationDetails.accessControlKeys]) which once hashed using sha256 will give the key of the
	 * security metadata.
	 * However, in order to avoid introducing undesired links between entities which could be detrimental to the
	 * patients privacy the access control keys should be created also using information on the entity class and secret
	 * foreign keys of the entity holding the delegation, in order to ensure that in case of different confidentiality
	 * settings for the entity the security metadata key will also be different and won't leak information on links
	 * between data.
	 * ```
	 * accessControlKey = sha256Bytes(accessControlSecret + entityClass + sfk[0]).take(16)
	 * securityMetadataKey = sha256Hex(accessControlKey)
	 * ```
	 */
	val accessControlSecret: Map<KeypairFingerprintString, Base64String>,
	/**
	 * Encrypted signature key (hmac-sha256) shared between delegate and delegator, to allow either of them to modify
	 * the exchange data, without voiding the authenticity guarantee.
	 */
	val sharedSignatureKey: Map<KeypairFingerprintString, Base64String>,
	/**
	 * Signature to ensure the key data has not been tampered with by third parties (any actor without access to the
	 * keypair of the delegator/delegate): when creating new exchange data the delegator will create a new hmac key and
	 * sign it with his own private key.
	 * This field will contain the signature by fingerprint of the public key to use for verification.
	 *
	 * Note that in case of exchange data to a simple-type group this value is non-empty only for the piece where the
	 * [recipient] is the [delegator].
	 */
	val delegatorSignature: Map<KeypairFingerprintString, Base64String> = emptyMap(),
	/**
	 * Base 64 signature of the exchange data, to ensure it was not tampered by third parties. This signature validates:
	 * - The (decrypted) exchange key
	 * - The (decrypted) access control secret
	 * - The delegator and delegates being part of the exchange data
	 * - The public keys used in the exchange data (allows to consider them as verified in a second moment).
	 * - The recipient and exchangeDataGroupId (included only if not null)
	 *
	 * Note that in case of exchange data to a simple-type group this value only includes the signature for this piece.
	 */
	val sharedSignature: Base64String,
	/**
	 * If true this exchange data has been invalidated for encryption of new data; it can still be used to decrypt
	 * and modify existing data, but when creating new data the SDK will not use this exchange data.
	 *
	 * For exchange data to a simple-type group this value is relevant only on the piece where the [recipient] is
	 * the [delegator].
	 */
	val invalidated: Boolean = false,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
) : StoredDocument,
	HasExplicitDataOwnerAccess {
	init {
		require(delegator.isNotBlank() && delegate.isNotBlank()) {
			"Delegator and delegate ids are required for exchange data"
		}
		require(
			exchangeKey.isNotEmpty() &&
				accessControlSecret.isNotEmpty() &&
				sharedSignatureKey.isNotEmpty() &&
				sharedSignature.isNotEmpty(),
		) {
			"Access control data should specify values for exchangeKey, accessControlKey and shared signature."
		}
	}

	override fun withIdRev(id: String?, rev: String): ExchangeData = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?): ExchangeData = this.copy(deletionDate = deletionDate)

	override val dataOwnersWithExplicitAccess: Map<String, AccessLevel>
		get() = mapOf(this.delegator to AccessLevel.WRITE, this.delegate to AccessLevel.WRITE)
}
