package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerWithType

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

	fun getCryptoActorHierarchy(dataOwnerId: String): Flow<DataOwnerWithType>
	fun getCryptoActorHierarchyStub(dataOwnerId: String): Flow<CryptoActorStubWithType>
}
