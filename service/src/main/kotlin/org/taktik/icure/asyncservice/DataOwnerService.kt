package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.entities.requests.DataOwnerPublicKeys
import org.taktik.icure.entities.requests.LinkedDataOwner
import org.taktik.icure.pagination.MultiKeyPaginationElement

interface DataOwnerService {
	/**
	 * Get just the crypto-actor properties of a data owner. Any data owner is allowed to call this method.
	 * Ignores inaccessible missing data owners.
	 * @param dataOwnerId a data owner id
	 * @return the type of the data owner with the provided id and its crypto-actor properties.
	 */
	suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType?

	/**
	 * Bulk version of [getCryptoActorStub]
	 */
	fun getCryptoActorStubs(dataOwnerIds: List<String>): Flow<CryptoActorStubWithType>

	/**
	 * Get the data owner with the provided id. Only data owners with access to the data owner with the provided id are
	 * allowed to call this method (e.g. a data owner without a delegation for a patient data owner can't use this
	 * method).
	 * @param dataOwnerId a data owner id
	 * @return the data owner with the provided id and its type.
	 */
	suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType?

	/**
	 * Bulk version of [getDataOwner]. Ignores inaccessible or missing data owners.
	 */
	fun getDataOwners(dataOwnerIds: List<String>): Flow<DataOwnerWithType>

	/**
	 * Updates only the crypto-actor properties of a data owner. The data owner itself is allowed to modify any of its
	 * crypto-actor properties, and other data owners are allowed to modify only exchange keys towards themselves.
	 * @param modifiedCryptoActor the modified crypto-actor properties of a data owner
	 * @return the updated crypto-actor.
	 */
	suspend fun modifyCryptoActor(modifiedCryptoActor: CryptoActorStubWithType): CryptoActorStubWithType

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchyInfo instead")
	fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType>

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchyInfo instead")
	fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType>

	/**
	 * Get the type and group hierarchies of the data owner with the provided id as a tree of ids rooted at the data
	 * owner itself (see [org.taktik.icure.entities.base.DataOwnerGroupLinkType] for the membership propagation
	 * rules). The parents of each node are the data owners it is directly linked to through the legacy parentId or a
	 * dataOwnerGroups link, together with the type of that link; a data owner reachable through multiple paths
	 * appears once per path.
	 * Any data owner is allowed to call this method.
	 * @param dataOwnerId a data owner id.
	 * @return the id hierarchy tree rooted at the data owner with the provided id.
	 */
	suspend fun getCryptoActorHierarchyInfo(dataOwnerId: String): DataOwnerHierarchyInfo

	/**
	 * Get the data owners that declare a direct link to any of the data owner groups with the provided ids,
	 * together with the group link type of each of them. Any data owner is allowed to call this method.
	 *
	 * See [org.taktik.icure.asynclogic.DataOwnerLogic.findDataOwnersLinkedToGroups] for the semantics: this is
	 * not transitive, a page may hold fewer rows than [limit] because of the deduplication of data owners linked
	 * to several of [dataOwnerGroupIds], and the logic may use a lower limit than requested.
	 */
	fun findDataOwnersLinkedToGroups(
		dataOwnerGroupIds: List<String>,
		dataOwnerType: DataOwnerType,
		startDocumentId: String?,
		limit: Int,
	): Flow<MultiKeyPaginationElement<LinkedDataOwner, String>>

	/**
	 * Get the public keys of the data owners with the provided ids, each with the encryption algorithm it must be
	 * used with. Any data owner is allowed to call this method. Ignores missing data owners, and data owners
	 * without any public key.
	 *
	 * @throws IllegalArgumentException if there are too many [dataOwnerIds].
	 */
	fun getDataOwnersPublicKeys(
		dataOwnerIds: List<String>,
		dataOwnerType: DataOwnerType,
	): Flow<DataOwnerPublicKeys>

	fun addDataOwnersToGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		newMembersIds: List<String>
	): Flow<String>

	fun removeDataOwnersFromGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		membersToRemoveIds: List<String>,
		invalidateSharedExchangeDataIfNeeded: Boolean
	): Flow<String>
}
