package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.entities.requests.DataOwnerPublicKeys
import org.taktik.icure.entities.requests.LinkedDataOwner
import org.taktik.icure.pagination.MultiKeyPaginationElement

interface DataOwnerLogic {
	/**
	 * Get just the crypto-actor properties of a data owner.
	 * @param dataOwnerId a data owner id
	 * @return the type of the data owner with the provided id and its crypto-actor properties.
	 */
	suspend fun getCryptoActorStub(dataOwnerId: String): CryptoActorStubWithType?

	fun getCryptoActorStubs(dataOwnerIds: List<String>): Flow<CryptoActorStubWithType>

	/**
	 * Get just the crypto-actor properties of a data owner for which the type is known.
	 * @param dataOwnerId a data owner id
	 * @param dataOwnerType the type of the data owner with the provided id.
	 * @return the crypto-actor properties of the data owner with the provided id, or null if the data owner does not
	 * exist or is not of the expected type.
	 */
	suspend fun getCryptoActorStubWithType(
		dataOwnerId: String,
		dataOwnerType: DataOwnerType,
	): CryptoActorStub?

	/**
	 * Get just the crypto-actor properties of multiple data owners for which the type is known, in bulk.
	 * @param dataOwnerIds the ids of the data owners to retrieve.
	 * @param dataOwnerType the type of the data owners with the provided ids.
	 * @return the crypto-actor properties of the data owners with the provided ids, omitting the ids that don't
	 * match any existing data owner of the expected type.
	 */
	fun getCryptoActorStubsWithType(
		dataOwnerIds: Collection<String>,
		dataOwnerType: DataOwnerType,
	): Flow<CryptoActorStub>

	/**
	 * Get the data owner with the provided id.
	 * @param dataOwnerId a data owner id
	 * @return the data owner with the provided id and its type.
	 */
	suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType?

	/**
	 * Get the data owner with the provided id.
	 * @param dataOwnerIds the ids of the data owner to retrieve.
	 * @return the data owner with the provided id and its type.
	 */
	fun getDataOwners(dataOwnerIds: List<String>): Flow<DataOwnerWithType>

	/**
	 * Updates only the crypto-actor properties of a data owner.
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
	 * @param dataOwnerId a data owner id.
	 * @return the id hierarchy tree rooted at the data owner with the provided id.
	 */
	suspend fun getCryptoActorHierarchyInfo(dataOwnerId: String): DataOwnerHierarchyInfo

	/**
	 * Get the data owners that declare a direct link to any of the data owner groups with the provided ids,
	 * through the legacy parentId or a dataOwnerGroups link, together with the group link type of each of them.
	 *
	 * This does **not** follow the links transitively: only the data owners directly linked to one of
	 * [dataOwnerGroupIds] are returned, never the data owners linked to *them*. A caller that wants the full
	 * membership walks the tree itself, deciding at each step from
	 * [org.taktik.icure.entities.requests.LinkedDataOwner.groupLinkType] whether it needs to.
	 *
	 * Only healthcare parties may currently be the target of a group link, so this returns nothing without any
	 * database access for [DataOwnerType.PATIENT] and [DataOwnerType.DEVICE].
	 *
	 * A data owner linked to several of [dataOwnerGroupIds] is returned only once per page, but a page may
	 * therefore hold **fewer** rows than [limit] even when there is a next page: one call does exactly one
	 * database query, and no attempt is made to refill a page after deduplication. Deduplication is also
	 * per-page: the same data owner may be returned again by a later page.
	 *
	 * If the flow emits a [MultiKeyPaginationElement.NextPage] instead of a [MultiKeyPaginationElement.Row], ask
	 * for the next page by calling again with [MultiKeyPaginationElement.NextPage.nextKeys] as
	 * [dataOwnerGroupIds] and [MultiKeyPaginationElement.NextPage.nextDocId] as [startDocumentId]. All results
	 * have been returned once the flow completes without ever emitting a [MultiKeyPaginationElement.NextPage].
	 *
	 * @param dataOwnerGroupIds the ids of the data owners representing the groups, without duplicates.
	 * @param dataOwnerType the type of [dataOwnerGroupIds] and of the returned data owners: a group link may only
	 * connect data owners of the same type.
	 * @param startDocumentId the cursor of a previous page, applying only to the first of [dataOwnerGroupIds].
	 * @param limit the maximum number of rows to return. The logic may use a lower limit than requested.
	 * @throws IllegalArgumentException if [dataOwnerGroupIds] is empty or has duplicates.
	 */
	fun findDataOwnersLinkedToGroups(
		dataOwnerGroupIds: List<String>,
		dataOwnerType: DataOwnerType,
		startDocumentId: String?,
		limit: Int,
	): Flow<MultiKeyPaginationElement<LinkedDataOwner, String>>

	/**
	 * Get the public keys of the data owners with the provided ids, each with the encryption algorithm it must be
	 * used with. Ids that don't match an existing data owner of [dataOwnerType], and data owners without any
	 * public key, produce no result.
	 *
	 * @param dataOwnerIds the ids of the data owners.
	 * @param dataOwnerType the type of the data owners with the provided ids.
	 * @throws IllegalArgumentException if there are too many [dataOwnerIds]: unlike the paginated searches this
	 * has no cursor to resume from, so an oversized request fails instead of being silently truncated.
	 */
	fun getDataOwnersPublicKeys(
		dataOwnerIds: List<String>,
		dataOwnerType: DataOwnerType,
	): Flow<DataOwnerPublicKeys>
}
