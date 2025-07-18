package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.utils.KeypairFingerprintV2String
import org.taktik.icure.entities.utils.Sha256HexString

/**
 * Metadata to support explicit data owners in the decryption of secure delegations explicit->anonymous or
 * anonymous->explicit. Associates secure delegation keys to the encrypted id of the exchange data used for
 * the creation of the secure delegation.
 */
data class ExchangeDataMap(
	/**
	 * The id of this entity is the Secure Delegation Key.
	 */
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,

	/**
	 * A map where each key is the fingerprint of a public key of the explicit data owner in an explicit->anonymous or
	 * anonymous->explicit delegation, and the value is the id of the exchange data used for the creation
	 * of the secure delegation.
	 */
	val encryptedExchangeDataIds: Map<KeypairFingerprintV2String, Sha256HexString> = emptyMap(),
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
) : StoredDocument {
	override fun withIdRev(id: String?, rev: String): ExchangeDataMap = id?.let { this.copy(id = it, rev = rev) } ?: this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?): ExchangeDataMap = this.copy(deletionDate = deletionDate)
}
