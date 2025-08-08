package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CalendarItemV2Mapper

// TODO tmp no support yet for generics

interface CalendarItemBulkShareResultV2Mapper {

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["calendarItemToDto"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<CalendarItemDto>): EntityBulkShareResult<CalendarItem>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToCalendarItem"])
	fun map(bulkShareResult: EntityBulkShareResult<CalendarItem>): EntityBulkShareResultDto<CalendarItemDto>

	@Named("calendarItemToDto")
	fun calendarItemToDto(calendarItem: CalendarItem?): CalendarItemDto?

	@Named("dtoToCalendarItem")
	fun dtoToCalendarItem(calendarItemDto: CalendarItemDto?): CalendarItem?
}

@Service
class CalendarItemBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val calendarItemMapper: CalendarItemV2Mapper,
) : CalendarItemBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<CalendarItemDto>): EntityBulkShareResult<CalendarItem> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { calendarItemMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<CalendarItem>): EntityBulkShareResultDto<CalendarItemDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { calendarItemMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)
	override fun calendarItemToDto(calendarItem: CalendarItem?): CalendarItemDto? = calendarItem?.let { calendarItemMapper.map(it) }

	override fun dtoToCalendarItem(calendarItemDto: CalendarItemDto?): CalendarItem? = calendarItemDto?.let { calendarItemMapper.map(it) }
}
