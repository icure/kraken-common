package org.taktik.icure.asynclogic

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

    /**
     * Get just the crypto-actor properties of a data owner for which the type is known.
     * @param dataOwnerId a data owner id
     * @param dataOwnerType the type of the data owner with the provided id.
     * @return the crypto-actor properties of the data owner with the provided id, or null if the data owner does not
     * exist or is not of the expected type.
     */
    suspend fun getCryptoActorStubWithType(
        dataOwnerId: String,
        dataOwnerType: DataOwnerType
    ): CryptoActorStub?

    /**
     * Get the data owner with the provided id.
     * @param dataOwnerId a data owner id
     * @return the data owner with the provided id and its type.
     */
    suspend fun getDataOwner(dataOwnerId: String): DataOwnerWithType?

    /**
     * Updates only the crypto-actor properties of a data owner.
     * @param modifiedCryptoActor the modified crypto-actor properties of a data owner
     * @return the updated crypto-actor.
     */
    suspend fun modifyCryptoActor(modifiedCryptoActor: CryptoActorStubWithType): CryptoActorStubWithType
}