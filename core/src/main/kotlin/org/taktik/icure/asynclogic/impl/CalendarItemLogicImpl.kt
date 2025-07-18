/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.CalendarItemDAO
import org.taktik.icure.asyncdao.UserDAO
import org.taktik.icure.asynclogic.AgendaLogic
import org.taktik.icure.asynclogic.CalendarItemLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.mergeUniqueValuesForSearchKeys
import org.taktik.icure.utils.toComplexKeyPaginationOffset
import org.taktik.icure.validation.aspect.Fixer

open class CalendarItemLogicImpl(
	private val calendarItemDAO: CalendarItemDAO,
	private val agendaLogic: AgendaLogic,
	protected val userDAO: UserDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : EntityWithEncryptionMetadataLogic<CalendarItem, CalendarItemDAO>(
	fixer,
	sessionLogic,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters,
),
	CalendarItemLogic {
	override suspend fun createCalendarItem(calendarItem: CalendarItem) = fix(calendarItem, isCreate = true) { fixedCalendarItem ->
		if (fixedCalendarItem.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		calendarItemDAO.create(
			datastoreInformation,
			fixedCalendarItem.takeIf { it.hcpId != null } ?: fixedCalendarItem.copy(
				hcpId =
				calendarItem.agendaId?.let {
					agendaLogic.getAgenda(it)?.userId?.let { uId ->
						userDAO.getUserOnUserDb(datastoreInformation, uId, false).healthcarePartyId
					}
				},
			),
		)
	}

	override fun getAllCalendarItems(offset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			calendarItemDAO
				.getAllPaginated(datastoreInformation, offset.limitIncludingKey(), Nothing::class.java)
				.toPaginatedFlow<CalendarItem>(offset.limit),
		)
	}

	override suspend fun getCalendarItem(calendarItemId: String) = getEntity(calendarItemId)

	override fun getCalendarItemByPeriodAndHcPartyId(
		startDate: Long,
		endDate: Long,
		hcPartyId: String,
	): Flow<CalendarItem> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueValuesForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				calendarItemDAO.listCalendarItemByPeriodAndHcPartyId(datastoreInformation, startDate, endDate, key)
			},
		)
	}

	override fun getCalendarItemByPeriodAndAgendaId(
		startDate: Long,
		endDate: Long,
		agendaId: String,
		descending: Boolean,
	): Flow<CalendarItem> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(calendarItemDAO.listCalendarItemByPeriodAndAgendaId(datastoreInformation, startDate, endDate, agendaId, descending))
	}

	override fun getCalendarItemsByRecurrenceId(
		recurrenceId: String,
		paginationOffset: PaginationOffset<String>,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			calendarItemDAO
				.listCalendarItemsByRecurrenceId(datastoreInformation, recurrenceId, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<CalendarItem>(paginationOffset.limit),
		)
	}

	override fun getCalendarItemsByRecurrenceId(recurrenceId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(calendarItemDAO.listCalendarItemsByRecurrenceId(datastoreInformation, recurrenceId))
	}

	override fun listCalendarItemsByHCPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			calendarItemDAO.listCalendarItemsByHcPartyAndPatient(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretPatientKeys,
			),
		)
	}

	override fun findCalendarItemsByHCPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
		paginationOffset: PaginationOffset<List<Any>>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		if (secretPatientKeys.size == 1) {
			emitAll(
				calendarItemDAO.findCalendarItemsByHcPartyAndPatient(
					datastoreInformation,
					hcPartyId,
					secretPatientKeys.first(),
					paginationOffset.toComplexKeyPaginationOffset(),
				),
			)
		} else {
			emitAll(
				calendarItemDAO.findCalendarItemsByHcPartyAndPatient(
					datastoreInformation,
					hcPartyId,
					secretPatientKeys.sorted(),
					paginationOffset.toComplexKeyPaginationOffset(),
				),
			)
		}
	}

	override fun getCalendarItems(ids: List<String>): Flow<CalendarItem> = getEntities(ids)

	override fun entityWithUpdatedSecurityMetadata(
		entity: CalendarItem,
		updatedMetadata: SecurityMetadata,
	): CalendarItem = entity.copy(securityMetadata = updatedMetadata)

	override fun findCalendarItemIdsByDataOwnerPatientStartTime(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			calendarItemDAO.listCalendarItemIdsByDataOwnerPatientStartTime(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending,
			),
		)
	}

	override fun getGenericDAO(): CalendarItemDAO = calendarItemDAO
}
