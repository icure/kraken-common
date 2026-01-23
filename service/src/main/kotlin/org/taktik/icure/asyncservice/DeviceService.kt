package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Device
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException

interface DeviceService {
	suspend fun createDevice(device: Device): Device
	fun createDevices(devices: List<Device>): Flow<Device>
	suspend fun modifyDevice(device: Device): Device
	fun modifyDevices(devices: List<Device>): Flow<Device>
	suspend fun getDevice(deviceId: String): Device?
	fun getDevices(deviceIds: List<String>): Flow<Device>

	@Deprecated(message = "A DataOwner may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
	suspend fun getHcPartyKeysForDelegate(deviceId: String): Map<String, String>
	suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>>

	/**
	 * Marks a batch of entities as deleted.
	 * The data of the entities is preserved, but they won't appear in most queries.
	 * Ignores entities that:
	 * - don't exist
	 * - the user can't delete due to limited lack of write access
	 * - don't match the provided revision (if provided)
	 *
	 * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
	 * @return a [Flow] containing the deleted [Device].
	 */
	fun deleteDevices(ids: List<IdAndRev>): Flow<Device>

	/**
	 * Marks an entity as deleted.
	 * The data of the entity is preserved, but the entity won't appear in most queries.
	 *
	 * @param id the id of the entity to delete.
	 * @param rev the latest rev of the entity to delete.
	 * @return the deleted [Device]
	 * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun deleteDevice(id: String, rev: String?): Device

	/**
	 * Deletes an entity.
	 * An entity deleted this way can't be restored.
	 * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
	 *
	 * @param id the id of the entity
	 * @param rev the latest known revision of the entity.
	 * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
	 * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
	 * @throws ConflictRequestException if the entity rev doesn't match.
	 */
	suspend fun purgeDevice(id: String, rev: String): DocIdentifier
	fun purgeDevices(deviceIds: List<IdAndRev>): Flow<DocIdentifier>

	/**
	 * Restores an entity marked as deleted.
	 * The user needs to have write access to the entity
	 * @param id the id of the entity marked to restore
	 * @param rev the revision of the entity after it was marked as deleted
	 * @return the restored entity
	 */
	suspend fun undeleteDevice(id: String, rev: String): Device
	fun undeleteDevices(deviceIds: List<IdAndRev>): Flow<Device>

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
