package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.HasExplicitDataOwnerAccess
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.KeypairFingerprintString
import org.taktik.icure.exceptions.MergeConflictException
import org.taktik.icure.security.DataOwnerAuthenticationDetails

/**
 * Data necessary for the secure sharing of entities between data owners.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExchangeData(
    @param:ContentValue(ContentValues.UUID) @JsonProperty("_id")  override val id: String,
    @JsonProperty("_rev") override val rev: String? = null,
    /**
     * ID of the data owner which created this exchange data, in order to share some data with the [delegate].
     */
    val delegator: String,
    /**
     * ID of a data owner which can use this exchange data to access data shared with him by [delegator].
     */
    val delegate: String,
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
     */
    val delegatorSignature: Map<KeypairFingerprintString, Base64String>,
    /**
     * Base 64 signature of the exchange data, to ensure it was not tampered by third parties. This signature validates:
     * - The (decrypted) exchange key
     * - The (decrypted) access control secret
     * - The delegator and delegates being part of the exchange data
     * - The public keys used in the exchange data (allows to consider them as verified in a second moment).
     */
    val sharedSignature: Base64String,
    @JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
    @JsonProperty("deleted") override val deletionDate: Long? = null,
    @JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
    @JsonProperty("_conflicts") override val conflicts: List<String>? = null,
    @JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
) : StoredDocument, HasExplicitDataOwnerAccess {
    init {
        require(delegator.isNotBlank() && delegate.isNotBlank()) {
            "Delegator and delegate ids are required for exchange data"
        }
        require(
            exchangeKey.isNotEmpty()
                && accessControlSecret.isNotEmpty()
                && sharedSignatureKey.isNotEmpty()
                && sharedSignature.isNotEmpty()
        ) {
            "Access control data should specify values for exchangeKey, accessControlKey and shared signature."
        }
    }

    override fun withIdRev(id: String?, rev: String): ExchangeData =
        id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)

    override fun withDeletionDate(deletionDate: Long?): ExchangeData =
        this.copy(deletionDate = deletionDate)

    override val dataOwnersWithExplicitAccess: Map<String, AccessLevel>
        get() = mapOf(this.delegator to AccessLevel.WRITE, this.delegate to AccessLevel.WRITE)

    fun solveConflictsWith(other: ExchangeData): Map<String, Any?> {
        if (this.delegator != other.delegator || this.delegate != other.delegate) throw MergeConflictException(
            "Impossible to merge exchange data referring to different delegator/delegate pairs"
        )
        return super<StoredDocument>.solveConflictsWith(other) + mapOf(
            "delegator" to this.delegator,
            "delegate" to this.delegate,
            /*
             * RSA Encryption of the same value with the same key multiple times will give different result: discordant
             * entries for the same public key fingerprint is normal.
             */
            "exchangeKey" to this.exchangeKey + other.exchangeKey,
            "accessControlSecret" to this.accessControlSecret + other.accessControlSecret,
            // As a result of merging the signature may become invalid -> exchange data is not trusted anymore
            "sharedSignature" to this.sharedSignature,
            "sharedSignatureKey" to this.sharedSignatureKey + other.sharedSignatureKey,
            "delegatorSignature" to this.delegatorSignature + other.delegatorSignature,
        )
    }
}
