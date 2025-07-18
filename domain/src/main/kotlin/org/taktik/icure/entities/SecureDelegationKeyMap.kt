package org.taktik.icure.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Encryptable
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.utils.Base64String

/**
 * An internal top-level entity which allows data owners to identify the anonymous delegator and/or delegate of secure
 * delegation where they are not a participant.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SecureDelegationKeyMap(
	@param:ContentValue(ContentValues.UUID) @JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	/**
	 * The secure delegation key this map refers to.
	 */
	val delegationKey: String,
	/**
	 * The delegator of the secure delegation key this map refers to, if the delegator is anonymous in the delegation,
	 * and if not encrypted.
	 * On the server side this value should always be encrypted.
	 */
	val delegator: String?,
	/**
	 * The delegate of the secure delegation key this map refers to, if the delegate is anonymous in the delegation,
	 * and if not encrypted.
	 * On the server side this value should always be encrypted.
	 */
	val delegate: String?,
	override val encryptedSelf: Base64String?,
	override val securityMetadata: SecurityMetadata?,

	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<Delegation>> = emptyMap(),
	override val delegations: Map<String, Set<Delegation>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<Delegation>> = emptyMap(),
	@param:JsonProperty("rev_history") override val revHistory: Map<String, String>? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = null,
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = null,
	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = null,
) : StoredDocument,
	HasEncryptionMetadata,
	Encryptable {
	init {
		require(secretForeignKeys.isEmpty() && delegations.isEmpty() && encryptionKeys.isEmpty() && cryptedForeignKeys.isEmpty()) {
			"Secure delegation key maps should not contain legacy delegations."
		}
	}

	override fun withDeletionDate(deletionDate: Long?): SecureDelegationKeyMap = this.copy(deletionDate = deletionDate)

	fun solveConflictsWith(other: SecureDelegationKeyMap): Map<String, Any?> {
		require(this.delegationKey == other.delegationKey) {
			"Can't merge automatically secure delegation key maps with different delegation keys."
		}
		return super<StoredDocument>.solveConflictsWith(other) +
			super<HasEncryptionMetadata>.solveConflictsWith(other) +
			super<Encryptable>.solveConflictsWith(other) +
			mapOf(
				"delegator" to (this.delegator ?: other.delegator),
				"delegate" to (this.delegate ?: other.delegate),
				"delegationKey" to this.delegationKey,
			)
	}

	override fun withIdRev(id: String?, rev: String): SecureDelegationKeyMap = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	fun validateForStore() {
		require(delegator == null && delegate == null && encryptedSelf != null) {
			"Delegator and delegate fields should have been encrypted."
		}
	}
}
