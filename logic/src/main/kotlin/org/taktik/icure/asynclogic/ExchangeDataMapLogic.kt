package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.entities.utils.Base64String
import org.taktik.icure.entities.utils.HexString
import org.taktik.icure.entities.utils.KeypairFingerprintV2String

interface ExchangeDataMapLogic {

	/**
	 * Updates an existing [ExchangeDataMap].
	 * @param exchangeDataMap the updated [ExchangeDataMap].
	 * @return the updated [ExchangeDataMap] or null if the operation was not successful.
	 */
	suspend fun modifyExchangeDataMap(exchangeDataMap: ExchangeDataMap): ExchangeDataMap?

	/**
	 * Gets a batch of [ExchangeDataMap] by their ids. All data owners can access this method
	 * @param ids the id of the [ExchangeDataMap]s to retrieve.
	 * @return a [Flow] of all the [ExchangeDataMap] that were found.
	 */
	fun getExchangeDataMapBatch(ids: Collection<String>): Flow<ExchangeDataMap>

	/**
	 * Creates an [ExchangeDataMap] for each element of the collection passed as parameter. If a batch already exists,
	 * then it is updated by appending its encryptedExchangeKeys to the existing ones.
	 * @param batch the encrypted exchange data id to create or update in the [ExchangeDataMap]s. Each key is the hex-encoded access control
	 * key while the value is another map that associated the encrypted ExchangeData id to the fingerprint
	 * of the public key used to encrypt it.
	 * @return a [Flow] of all the [ExchangeDataMap] that were successfully created or updated.
	 */
	fun createOrUpdateExchangeDataMapBatchByAccessControlKey(batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>): Flow<ExchangeDataMap>

	/**
	 * Creates an [ExchangeDataMap] for each element of the collection passed as parameter. If a batch already exists,
	 * then it is updated by appending its encryptedExchangeKeys to the existing ones.
	 * @param batch the encrypted exchange data id to create or update in the [ExchangeDataMap]s. Each key is the hex-encoded hash of the
	 * access control key while the value is another map that associated the encrypted ExchangeData id to the
	 * fingerprint of the public key used to encrypt it.
	 * @return a [Flow] of all the [ExchangeDataMap] that were successfully created or updated.
	 */
	fun createOrUpdateExchangeDataMapBatchById(batch: Map<HexString, Map<KeypairFingerprintV2String, Base64String>>): Flow<ExchangeDataMap>
}
