package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Device

interface DeviceService {
	suspend fun createDevice(device: Device): Device?
	fun createDevices(devices: List<Device>): Flow<Device>
	suspend fun modifyDevice(device: Device): Device?
	fun modifyDevices(devices: List<Device>): Flow<Device>
	suspend fun getDevice(deviceId: String): Device?
	fun getDevices(deviceIds: List<String>): Flow<Device>
	@Deprecated(message = "A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(deviceId: String): Map<String, String>
	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>
	suspend fun deleteDevice(id: String): DocIdentifier?
	fun deleteDevices(ids: Collection<String>): Flow<DocIdentifier>
	fun filterDevices(filter: FilterChain<Device>, limit: Int, startDocumentId: String?): Flow<ViewQueryResultEvent>
	fun getEntityIds(): Flow<String>

	/**
	 * Retrieves the ids of the [Device]s matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [Device].
	 * @return a [Flow] of the ids matching the filter.
	 * @throws AccessDeniedException if the current user does not have the Permission to search devices with a filter.
	 */
	fun matchDevicesBy(filter: AbstractFilter<Device>): Flow<String>
}
