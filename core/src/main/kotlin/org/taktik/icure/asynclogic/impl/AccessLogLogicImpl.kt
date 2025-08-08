/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.AccessLogDAO
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.AccessLogLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.result.AggregatedAccessLogs
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.pagination.toPaginatedList
import org.taktik.icure.validation.aspect.Fixer
import java.time.Instant

@Service
@Profile("app")
class AccessLogLogicImpl(
	private val accessLogDAO: AccessLogDAO,
	private val patientDAO: PatientDAO,
	private val objectMapper: ObjectMapper,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	private val sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : EntityWithEncryptionMetadataLogic<AccessLog, AccessLogDAO>(
	fixer,
	sessionLogic,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters,
),
	AccessLogLogic {
	override suspend fun createAccessLog(accessLog: AccessLog) = fix(accessLog, isCreate = true) { fixedAccessLog ->
		if (fixedAccessLog.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		accessLogDAO.create(
			datastoreInformation,
			if (fixedAccessLog.date == null) {
				fixedAccessLog.copy(user = sessionLogic.getCurrentUserId(), date = Instant.now())
			} else {
				fixedAccessLog.copy(user = sessionLogic.getCurrentUserId())
			},
		)
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listAccessLogIdsByDataOwnerPatientDate instead")
	override fun listAccessLogsByHCPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretForeignKeys: List<String>,
	): Flow<AccessLog> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			accessLogDAO.findAccessLogsByHCPartyAndSecretPatientKeys(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretForeignKeys,
			),
		)
	}

	override fun listAccessLogIdsByDataOwnerPatientDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			accessLogDAO.listAccessLogIdsByDataOwnerPatientDate(
				datastoreInformation = datastoreInformation,
				searchKeys = getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys = secretForeignKeys,
				startDate = startDate,
				endDate = endDate,
				descending = descending,
			),
		)
	}

	override suspend fun getAccessLog(accessLogId: String): AccessLog? = getEntity(accessLogId)

	override fun getAccessLogs(ids: List<String>): Flow<AccessLog> = getEntities(ids)

	override fun listAccessLogsBy(
		fromEpoch: Long,
		toEpoch: Long,
		paginationOffset: PaginationOffset<Long>,
		descending: Boolean,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			accessLogDAO
				.listAccessLogsByDate(datastoreInformation, fromEpoch, toEpoch, paginationOffset.limitIncludingKey(), descending)
				.toPaginatedFlow<AccessLog>(paginationOffset.limit),
		)
	}

	private fun decomposeStartKey(startKeyString: String?): Long? = startKeyString?.let { objectMapper.readValue<Long>(it) }

	private suspend fun doAggregatePatientByAccessLogs(
		userId: String,
		accessType: String?,
		startDate: Long?,
		startKey: String?,
		startDocumentId: String?,
		limit: Int,
		paginationOffset: PaginationOffset<ComplexKey>,
		patientIds: List<String> = emptyList(),
		patients: List<Patient> = emptyList(),
		totalCount: Int = 0,
	): AggregatedAccessLogs {
		val datastoreInformation = getInstanceAndGroup()
		return findAccessLogsByUserAfterDate(
			userId,
			accessType,
			decomposeStartKey(startKey) ?: startDate,
			paginationOffset,
			true,
		).toPaginatedList<AccessLog, ComplexKey>()
			.let { accessLogPaginatedList ->
				val count = accessLogPaginatedList.rows.count()
				val previousPatientIds = patientIds.toSet()
				val newPatientIds =
					accessLogPaginatedList.rows
						.let { accessLogs ->
							if (decomposeStartKey(startKey) != null && startDocumentId != null && patientIds.isEmpty()) {
								@Suppress("DEPRECATION")
								accessLogs.dropWhile { it.patientId != startDocumentId }
							} else {
								accessLogs
							}
						}.mapNotNull {
							@Suppress("DEPRECATION")
							it.patientId
						}.filter { !previousPatientIds.contains(it) }
						.distinct()

				val newPatients = patientDAO.getPatients(datastoreInformation, newPatientIds).filter { it.deletionDate == null }.toList()
				((patientIds + newPatientIds) to (patients + newPatients)).let { (updatedPatientIds, updatedPatients) ->
					if (updatedPatients.size <= limit && accessLogPaginatedList.nextKeyPair != null) {
						doAggregatePatientByAccessLogs(
							userId,
							accessType,
							startDate,
							startKey,
							startDocumentId,
							limit,
							PaginationOffset(
								accessLogPaginatedList.nextKeyPair?.startKey as? ComplexKey,
								accessLogPaginatedList.nextKeyPair?.startKeyDocId,
								null,
								limit * 2 + 1,
							),
							updatedPatientIds,
							updatedPatients,
							totalCount + count,
						)
					} else if (updatedPatients.size > limit) {
						updatedPatients.take(limit + 1).let { patientsPlusNextKey ->
							val lastKeyMillis =
								accessLogPaginatedList.rows
									.firstOrNull {
										@Suppress("DEPRECATION")
										it.patientId == patientsPlusNextKey.last().id
									}?.date
									?.toEpochMilli()
							AggregatedAccessLogs(
								accessLogPaginatedList.totalSize,
								totalCount + count,
								patientsPlusNextKey.subList(0, limit),
								lastKeyMillis,
								patientsPlusNextKey.last().id,
							)
						}
					} else {
						AggregatedAccessLogs(accessLogPaginatedList.totalSize, totalCount + count, updatedPatients, null, null)
					}
				}
			}
	}

	override suspend fun aggregatePatientByAccessLogs(
		userId: String,
		accessType: String?,
		startDate: Long?,
		startKey: String?,
		startDocumentId: String?,
		limit: Int,
	) = doAggregatePatientByAccessLogs(
		userId,
		accessType,
		startDate,
		startKey,
		startDocumentId,
		limit,
		PaginationOffset(
			null,
			null,
			null,
			limit * 2 + 1,
		),
	)

	override fun findAccessLogsByUserAfterDate(
		userId: String,
		accessType: String?,
		startDate: Long?,
		pagination: PaginationOffset<ComplexKey>,
		descending: Boolean,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			accessLogDAO
				.findAccessLogsByUserAfterDate(datastoreInformation, userId, accessType, startDate, pagination.limitIncludingKey(), descending)
				.toPaginatedFlow<AccessLog>(pagination.limit),
		)
	}

	override fun entityWithUpdatedSecurityMetadata(
		entity: AccessLog,
		updatedMetadata: SecurityMetadata,
	): AccessLog = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO() = accessLogDAO
}
