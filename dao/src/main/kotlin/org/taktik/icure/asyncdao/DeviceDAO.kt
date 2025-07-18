package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.Device

interface DeviceDAO : GenericDAO<Device> {

	fun findDevicesByIds(datastoreInformation: IDatastoreInformation, deviceIds: Flow<String>): Flow<ViewQueryResultEvent>

	fun listDeviceIdsByResponsible(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Flow<String>

	suspend fun getDevice(datastoreInformation: IDatastoreInformation, deviceId: String): Device?
	fun getDevices(datastoreInformation: IDatastoreInformation, deviceIds: Collection<String>): Flow<Device>
	suspend fun getHcPartyKeysForDelegate(datastoreInformation: IDatastoreInformation, deviceId: String): Map<String, String>

	suspend fun getAesExchangeKeysForDelegate(datastoreInformation: IDatastoreInformation, healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>
}
