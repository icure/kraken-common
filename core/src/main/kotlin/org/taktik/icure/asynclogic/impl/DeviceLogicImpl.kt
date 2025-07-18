package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import org.slf4j.LoggerFactory
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asynclogic.DeviceLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Device
import org.taktik.icure.validation.aspect.Fixer

open class DeviceLogicImpl(
	datastoreInstanceProvider: DatastoreInstanceProvider,
	private val deviceDAO: DeviceDAO,
	filters: Filters,
	fixer: Fixer,
) : GenericLogicImpl<Device, DeviceDAO>(fixer, datastoreInstanceProvider, filters),
	DeviceLogic {
	companion object {
		private val log = LoggerFactory.getLogger(DeviceLogicImpl::class.java)
	}

	override suspend fun createDevice(device: Device): Device? = fix(device, isCreate = true) { fixedDevice ->
		if (fixedDevice.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		createEntities(listOf(fixedDevice)).firstOrNull()
	}

	override fun createDevices(devices: List<Device>): Flow<Device> = flow {
		try {
			emitAll(
				createEntities(devices.map { device -> fix(device, isCreate = true) }),
			)
		} catch (e: Exception) {
			log.error("createDevices: " + e.message)
			throw IllegalArgumentException("Invalid Devices problem", e)
		}
	}

	override suspend fun modifyDevice(device: Device): Device? = fix(device, isCreate = false) {
		modifyEntities(setOf(it)).singleOrNull()
	}

	override fun modifyDevices(devices: List<Device>): Flow<Device> = flow {
		try {
			emitAll(
				modifyEntities(devices.map { device -> fix(device, isCreate = false) }),
			)
		} catch (e: Exception) {
			log.error("modifyDevices: " + e.message)
			throw IllegalArgumentException("Invalid Devices problem", e)
		}
	}

	override suspend fun getDevice(deviceId: String) = getEntity(deviceId)

	override fun getDevices(deviceIds: Collection<String>): Flow<Device> = getEntities(deviceIds)

	@Deprecated("A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	override suspend fun getHcPartyKeysForDelegate(deviceId: String): Map<String, String> {
		val datastoreInformation = getInstanceAndGroup()
		return deviceDAO.getHcPartyKeysForDelegate(datastoreInformation, deviceId)
	}

	override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
		val datastoreInformation = getInstanceAndGroup()
		return deviceDAO.getAesExchangeKeysForDelegate(datastoreInformation, healthcarePartyId)
	}

	override fun filterDevices(
		filter: FilterChain<Device>,
		limit: Int,
		startDocumentId: String?,
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation)

		val sortedIds =
			if (startDocumentId != null) { // Sub-set starting from startDocId to the end (including last element)
				ids.dropWhile { it != startDocumentId }
			} else {
				ids
			}
		val selectedIds = sortedIds.take(limit + 1) // Fetching one more device for the start key of the next page
		emitAll(
			deviceDAO.findDevicesByIds(datastoreInformation, selectedIds),
		)
	}

	override fun getGenericDAO() = deviceDAO

	override fun getEntityIds() = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(deviceDAO.getEntityIds(datastoreInformation))
	}
}
