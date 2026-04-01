package org.taktik.icure.services.external.rest.v2.mapper.conflicts

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.conflicts.MergeResult
import org.taktik.icure.services.external.rest.v2.dto.conflicts.MergeResultDto


@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class MergeResultV2Mapper {

	fun map(mergeResultDto: MergeResultDto): MergeResult = when(mergeResultDto) {
		is MergeResultDto.Failure -> map(mergeResultDto)
		is MergeResultDto.PartialSuccess -> map(mergeResultDto)
		is MergeResultDto.Success -> map(mergeResultDto)
	}

	fun map(mergeResult: MergeResult): MergeResultDto = when(mergeResult) {
		is MergeResult.Failure -> map(mergeResult)
		is MergeResult.PartialSuccess -> map(mergeResult)
		is MergeResult.Success -> map(mergeResult)
	}


	abstract fun map(dto: MergeResultDto.Failure): MergeResult.Failure
	abstract fun map(dto: MergeResultDto.PartialSuccess): MergeResult.PartialSuccess
	abstract fun map(dto: MergeResultDto.Success): MergeResult.Success

	abstract fun map(entity: MergeResult.Failure): MergeResultDto.Failure
	abstract fun map(entity: MergeResult.PartialSuccess): MergeResultDto.PartialSuccess
	abstract fun map(entity: MergeResult.Success): MergeResultDto.Success
}