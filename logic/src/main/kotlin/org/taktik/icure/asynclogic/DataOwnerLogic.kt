package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.DataOwnerWithType

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

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchies instead")
	fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType>

	@Deprecated("Only follows the legacy linear parentId chain, use getCryptoActorHierarchiesStub instead")
	fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType>

	/**
	 * Get the data owner with the provided id followed by all the distinct data owners in its group hierarchies
	 * (see [org.taktik.icure.entities.base.DataOwnerGroupLinkType] for the membership propagation rules).
	 * Each data owner is emitted only once even if it is reachable through multiple paths; the tree structure can be
	 * rebuilt from the parentId and dataOwnerGroups of the returned data owners.
	 * @param dataOwnerId a data owner id.
	 * @return the data owner with the provided id and all the data owners of its group hierarchies.
	 */
	fun getCryptoActorHierarchies(dataOwnerId: String): Flow<DataOwnerWithType>

	/**
	 * Same as [getCryptoActorHierarchies] but limited to the crypto-actor properties of the data owners.
	 * @param dataOwnerId a data owner id.
	 * @return the crypto-actor stubs of the data owner with the provided id and all the data owners of its group
	 * hierarchies.
	 */
	fun getCryptoActorHierarchiesStub(dataOwnerId: String): Flow<CryptoActorStubWithType>
}
