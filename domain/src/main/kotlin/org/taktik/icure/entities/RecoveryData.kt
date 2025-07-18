package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.HasExplicitDataOwnerAccess
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.AccessLevel
import org.taktik.icure.entities.embed.RevisionInfo
import java.lang.IllegalStateException

/**
 * Metadata which allows a data owner to recover cryptographic secrets meant for him.
 * In order to simplify the recovery process, the id of recovery data should be (safely) derived from the encryption key
 * the data was encrypted with, this way only the encryption key is needed in order to find and use the recovery data.
 * In the official iCure SDKs this is derived as sha256Hex(encryptionKeyHex)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RecoveryData(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	/**
	 * Id of the data owner that this recovery data is meant for
	 */
	val recipient: String,
	/**
	 * Encrypted recovery data. The structure of the decrypted data depends on the [type] of the recovery data.
	 */
	val encryptedSelf: String,
	/**
	 * Type of the recovery data.
	 */
	val type: Type,
	/**
	 * Timestamp (unix epoch in ms) at which this recovery data will expire. If null, this recovery data will never
	 * expire. Negative values or zero mean the data is already expired.
	 */
	val expirationInstant: Long? = null,
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
) : StoredDocument,
	HasExplicitDataOwnerAccess {
	/**
	 * Represents possible types of recovery data.
	 */
	enum class Type {
		/**
		 * This recovery data is meant to be used to recover a keypair of the recipient. This could be for making a key
		 * available on another device, or for recovering a keypair that has been fully lost.
		 */
		KEYPAIR_RECOVERY,

		/**
		 * This recovery data is meant to be used to recover an exchange key of the recipient. The main purpose of this
		 * is to allow data owners to share data with other data owners that do not have created a keypair yet, but it
		 * can also be used as part of the give-access-back recovery mechanism.
		 */
		EXCHANGE_KEY_RECOVERY,
	}

	override val dataOwnersWithExplicitAccess: Map<String, AccessLevel> get() = mapOf(recipient to AccessLevel.WRITE)
	override fun withDeletionDate(deletionDate: Long?) = throw IllegalStateException("Recovery data cannot be deleted normally: it must be purged instead.")

	override fun withIdRev(id: String?, rev: String) = copy(id = id ?: this.id, rev = rev)
}
