package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.base.DataOwnerIdWithHierarchy

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

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchiesIds instead")
	fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType>

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchiesIds instead")
	fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType>

	/**
	 * Get the group hierarchies of the data owner with the provided id as a tree of ids rooted at the data owner
	 * itself (see [org.taktik.icure.entities.base.DataOwnerGroupLinkType] for the membership propagation rules).
	 * The parents of each node are the data owners it is directly linked to through the legacy parentId or a
	 * dataOwnerGroups link; a data owner reachable through multiple paths appears once per path.
	 * @param dataOwnerId a data owner id.
	 * @return the id hierarchy tree rooted at the data owner with the provided id.
	 */
	suspend fun getCryptoActorHierarchiesIds(dataOwnerId: String): DataOwnerIdWithHierarchy
}
