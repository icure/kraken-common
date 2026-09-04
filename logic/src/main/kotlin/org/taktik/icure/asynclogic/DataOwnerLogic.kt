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

	/**
	 * Add data owners to a data owner group.
	 * The data owners must all be of the same type (as indicated by [dataOwnerType]).
	 * The data owner group (indicated by [dataOwnerGroupId]) must exist, or this method will fail; if any of the data
	 * owners in [newMembersIds] does not exist, or they can't be updated (for example if there are too many concurrent
	 * changes to that data owner) or can't be added to the group (because of restrictions on group link type), they
	 * will be ignored.
	 * The result contains the ids of all the data owners that were successfully added, or that were already members of
	 * the group.
	 * No guarantee on order of returned elements.
	 *
	 * Only the links a data owner declares itself are considered: a data owner that is already a member of
	 * [dataOwnerGroupId] transitively, through another group it is linked to, has no link of its own to it and is
	 * therefore linked to it directly, exactly like any other new member.
	 *
	 * Soft-deleted data owners are not ignored: they are updated and returned like any other member. A soft-deleted
	 * data owner can be undeleted at any moment, and it should then have the group membership it was given in the
	 * meantime.
	 *
	 * # Interruption and retry
	 *
	 * This method is idempotent: adding a data owner that is already a member is a no-op that still reports the data
	 * owner as added. If a call terminates early — the caller cancels it, or the process dies part-way through the
	 * updates — some of the members may have been added and the result gives no indication of which: calling the
	 * method again with the same input picks up exactly the work that is left, and repeating it eventually adds every
	 * member that can be added.
	 */
	fun addDataOwnersToGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		newMembersIds: List<String>
	): Flow<String>

	/**
	 * Remove data owners from a data owner group.
	 * The data owners must all be of the same type (as indicated by [dataOwnerType]).
	 * The data owner group (indicated by [dataOwnerGroupId]) must exist, or this method will fail; if any of the data
	 * owners in [membersToRemoveIds] does not exist, or they can't be updated (for example if there are too many
	 * concurrent changes to that data owner), they will be ignored.
	 * The result contains the ids of all the data owners that were successfully removed, or that were already not
	 * members of the group.
	 *
	 * If [invalidateSharedExchangeDataIfNeeded] is true (recommended), before returning this method will check if there
	 * is exchange data that should be invalidated for encryption as a result of the removal of [membersToRemoveIds]
	 * from [dataOwnerGroupId]:
	 * 1. Invalidation is performed only if [dataOwnerGroupId] represents a simple-type group. If [dataOwnerGroupId]
	 *    represents a parent-type group then no invalidation will be performed (invalidation must be performed at the
	 *    level of the group keypair, not at the level of the exchange data).
	 * 2. The exchange data considered for invalidation is all exchange data where [dataOwnerGroupId] is one of the
	 *    members. If the data owner group itself has links to other groups, then also exchange data where one of those
	 *    groups is a member will be considered, transitively (following only simple-type links, the only kind a
	 *    simple-type group may declare).
	 * 3. Of the removed members, only the ones this removal actually cut off from the group whose exchange data is
	 *    being considered are taken into account, and that is decided for each of the groups of step 2 separately. A
	 *    data owner that is still a member of that group once the links it declared itself are gone — because it
	 *    belongs to another group that is itself a member of it — keeps its access to that exchange data
	 *    legitimately, so nothing of it is invalidated on its behalf: that would only force the exchange data of
	 *    everyone else to be recreated for nothing. This covers a member that declared several paths to a group and
	 *    lost only one, a member that was only ever linked to it transitively and therefore had nothing to lose, and
	 *    a member that lost [dataOwnerGroupId] but belongs to one of the groups above it in its own right. If no
	 *    member is left for any of the groups of step 2, no exchange data is invalidated at all.
	 * 4. If any of the members cut off from one of the groups of step 2 is itself a simple-type data owner-group then
	 *    all the exchange data of that group is invalidated; otherwise only its exchange data where there is at least
	 *    a piece for a recipient that is one of the members cut off from it
	 *
	 * Only the links a data owner declares itself are considered and removed: a data owner that is a member of
	 * [dataOwnerGroupId] transitively, through another group it is linked to, has no link of its own to remove, so it
	 * is treated — and reported — as removed while it keeps that transitive membership. A transitive membership can
	 * only be changed where the link that creates it is declared: by removing the data owner from the intermediate
	 * group, or by removing the intermediate group from [dataOwnerGroupId]. Such a data owner is reported as removed,
	 * but the invalidation above does not treat it as one (step 3): it is still a member, so the exchange data it can
	 * decrypt is left usable for encryption.
	 *
	 * Soft-deleted data owners are not ignored: they are updated and returned like any other member, and their
	 * exchange data pieces are considered for invalidation. A soft-deleted data owner can be undeleted at any moment,
	 * so leaving it in a group it was removed from, or leaving the exchange data it can decrypt usable for
	 * encryption, would be unsafe.
	 *
	 * # Interruption and retry
	 *
	 * This method is idempotent: removing a data owner that is not a member is a no-op that still reports the data
	 * owner as removed, and invalidating exchange data that is already invalidated is a no-op as well.
	 *
	 * The members are updated first and the exchange data is invalidated after, so a call that terminates early —
	 * the caller cancels it, some members could not be updated, or the invalidation itself fails, which is reported
	 * as an error after the members have already been removed — can leave members removed while the exchange data
	 * they can still decrypt is usable for encryption. Calling the method again with the same input picks up exactly
	 * the work that is left, and repeating it eventually removes every member that can be removed and invalidates
	 * everything that has to be invalidated; a caller that gets an error, or that does not receive every id it asked
	 * for, should retry until it does.
	 *
	 * The retry must use the same input, and not only the members missing from the result: a member that an
	 * interrupted call already removed is no longer a member of the group, so it still counts as cut off for the
	 * invalidation (which is why it is part of the result), and dropping it from the input would drop the exchange
	 * data that has a piece for it from the invalidation, leaving that exchange data usable for encryption.
	 */
	fun removeDataOwnersFromGroup(
		dataOwnerType: DataOwnerType,
		dataOwnerGroupId: String,
		membersToRemoveIds: List<String>,
		invalidateSharedExchangeDataIfNeeded: Boolean
	): Flow<String>
}
