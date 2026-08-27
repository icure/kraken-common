package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.DeviceLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.mergers.Merger
import org.taktik.icure.validation.aspect.Fixer

open class DeviceLogicImpl(
	datastoreInstanceProvider: DatastoreInstanceProvider,
	private val deviceDAO: DeviceDAO,
	filters: Filters,
	fixer: Fixer,
	merger: Merger<Device>
) : GenericLogicImpl<Device, DeviceDAO>(fixer, datastoreInstanceProvider, filters),
	ConflictResolutionLogic<Device> by ConflictResolutionLogicImpl(deviceDAO, merger, datastoreInstanceProvider),
	DeviceLogic {
	/**
	 * Reused by group-explicit write paths such as `DeviceCloudLogicImpl`, which extends this class.
	 */
	protected val groupLinksHelper = object : CryptoActorLogicHelper<Device, DeviceDAO>(deviceDAO) {
		override val dataOwnerType = DataOwnerType.DEVICE
		override fun Device.copyWithLinks(parentId: String?, dataOwnerGroups: List<DataOwnerGroupLink>): Device =
			copy(parentId = parentId, dataOwnerGroups = dataOwnerGroups)
	}

	override suspend fun createDevice(device: Device) = fix(device, isCreate = true) { fixedDevice ->
		val datastoreInformation = getInstanceAndGroup()
		createEntity(groupLinksHelper.validateAndNormalizeOwnGroupLinks(fixedDevice, null, datastoreInformation))
	}

	override fun createDevices(devices: List<Device>): Flow<Device> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			createEntities(
				devices.map { device -> fix(device, isCreate = true) }
					.map { groupLinksHelper.validateAndNormalizeOwnGroupLinks(it, null, datastoreInformation) },
			),
		)
	}

	override suspend fun modifyDevice(device: Device) = fix(device, isCreate = false) {
		val datastoreInformation = getInstanceAndGroup()
		val original = deviceDAO.get(datastoreInformation, it.id)
		modifyEntity(groupLinksHelper.validateAndNormalizeOwnGroupLinks(it, original, datastoreInformation))
	}

	override fun modifyDevices(devices: List<Device>): Flow<Device> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			modifyEntities(
				devices.map { device -> fix(device, isCreate = false) }.map {
					val original = deviceDAO.get(datastoreInformation, it.id)
					groupLinksHelper.validateAndNormalizeOwnGroupLinks(it, original, datastoreInformation)
				},
			),
		)
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
