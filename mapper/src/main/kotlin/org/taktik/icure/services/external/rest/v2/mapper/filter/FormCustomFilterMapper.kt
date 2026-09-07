package org.taktik.icure.services.external.rest.v2.mapper.filter

import org.springframework.stereotype.Service
import org.taktik.icure.entities.filters.AbstractCustomFilter
import org.taktik.icure.entities.filters.FormByKeysCustomFilter
import org.taktik.icure.entities.filters.FormByRangeCustomFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.CustomFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.GenericByKeysCustomFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.GenericByRangeCustomFilterDto
import org.taktik.icure.services.external.rest.v2.mapper.dao.KeyComponentV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.dao.RangeQueryParametersV2Mapper

/**
 * Maps the generic [CustomFilterDto] payload received by the Form custom-filtering
 * endpoint to the concrete [FormByKeysCustomFilter]/[FormByRangeCustomFilter] domain filter.
 */
@Service
class FormCustomFilterV2Mapper(
	private val keyComponentMapper: KeyComponentV2Mapper,
	private val rangeQueryParametersMapper: RangeQueryParametersV2Mapper,
) {
	fun map(customFilterDto: CustomFilterDto): AbstractCustomFilter = when (customFilterDto) {
		is GenericByKeysCustomFilterDto -> FormByKeysCustomFilter(
			viewName = customFilterDto.viewName,
			startKey = keyComponentMapper.mapStartKey(customFilterDto.startKey),
			startDocumentId = customFilterDto.startDocumentId,
			limit = customFilterDto.limit,
			keyComponents = keyComponentMapper.mapKeyComponents(customFilterDto.keyComponents),
		)
		is GenericByRangeCustomFilterDto -> FormByRangeCustomFilter(
			viewName = customFilterDto.viewName,
			startKey = keyComponentMapper.mapStartKey(customFilterDto.startKey),
			startDocumentId = customFilterDto.startDocumentId,
			limit = customFilterDto.limit,
			nonRangeKeyComponents = keyComponentMapper.mapKeyComponents(customFilterDto.nonRangeKeyComponents),
			range = rangeQueryParametersMapper.map(customFilterDto.range),
		)
	}
}
