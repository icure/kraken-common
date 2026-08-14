package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.requests.ExchangeDataPieceCreationRequest
import org.taktik.icure.services.external.rest.v2.dto.requests.ExchangeDataPieceCreationRequestDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface ExchangeDataPieceCreationRequestV2Mapper {
	fun map(requestDto: ExchangeDataPieceCreationRequestDto): ExchangeDataPieceCreationRequest
	fun map(request: ExchangeDataPieceCreationRequest): ExchangeDataPieceCreationRequestDto
}
