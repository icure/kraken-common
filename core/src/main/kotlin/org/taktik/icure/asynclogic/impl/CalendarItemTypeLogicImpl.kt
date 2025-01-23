/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncdao.CalendarItemTypeDAO
import org.taktik.icure.asynclogic.CalendarItemTypeLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class CalendarItemTypeLogicImpl(
	private val calendarItemTypeDAO: CalendarItemTypeDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<CalendarItemType, CalendarItemTypeDAO>(fixer, datastoreInstanceProvider, filters), CalendarItemTypeLogic {

	override fun getAllCalendarItemTypes(offset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastore = getInstanceAndGroup()
		emitAll(calendarItemTypeDAO
			.getAllPaginated(datastore, offset.limitIncludingKey(), Nothing::class.java)
			.toPaginatedFlow<CalendarItemType>(offset.limit)
		)
	}

	override suspend fun createCalendarItemType(calendarItemType: CalendarItemType) =
		fix(calendarItemType, isCreate = true) { fixedCalendarItemType ->
			if(fixedCalendarItemType.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val datastoreInformation = getInstanceAndGroup()
			calendarItemTypeDAO.create(datastoreInformation, fixedCalendarItemType)
		}

	override suspend fun getCalendarItemType(calendarItemTypeId: String): CalendarItemType?  {
		val datastoreInformation = getInstanceAndGroup()
		return calendarItemTypeDAO.get(datastoreInformation, calendarItemTypeId)
	}

	override fun getCalendarItemTypes(calendarItemTypeIds: Collection<String>) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(calendarItemTypeDAO.getEntities(datastoreInformation, calendarItemTypeIds))
		}

	override suspend fun modifyCalendarTypeItem(calendarItemType: CalendarItemType) =
		fix(calendarItemType, isCreate = false) { fixedCalendarItemType ->
			val datastoreInformation = getInstanceAndGroup()
			calendarItemTypeDAO.save(datastoreInformation, fixedCalendarItemType)
		}

	override fun getAllEntitiesIncludeDeleted(
		offset: PaginationOffset<String>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(calendarItemTypeDAO
			.getCalendarItemsWithDeleted(datastoreInformation, offset.limitIncludingKey())
			.toPaginatedFlow<CalendarItemType>(offset.limit)
		)
	}

	override fun getAllEntitiesIncludeDeleted() = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(calendarItemTypeDAO.getCalendarItemsWithDeleted(datastoreInformation))
	}

	override fun getGenericDAO(): CalendarItemTypeDAO {
		return calendarItemTypeDAO
	}
}
