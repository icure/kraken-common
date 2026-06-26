package org.taktik.icure.services.external.rest.v2.mapper.conflicts

import org.springframework.stereotype.Service
import org.taktik.icure.entities.conflicts.ConflictResolutionStrategy
import org.taktik.icure.services.external.rest.v2.dto.conflicts.ConflictResolutionStrategyDto

@Service
class ConflictResolutionStrategyV2Mapper {

	fun map(conflictResolutionStrategyDto: ConflictResolutionStrategyDto): ConflictResolutionStrategy =
		when (conflictResolutionStrategyDto) {
			ConflictResolutionStrategyDto.FullMergeability -> ConflictResolutionStrategy.FullMergeability
			ConflictResolutionStrategyDto.LatestRevision -> ConflictResolutionStrategy.LatestRevision
		}

	fun map(conflictResolutionStrategy: ConflictResolutionStrategy): ConflictResolutionStrategyDto =
		when (conflictResolutionStrategy) {
			ConflictResolutionStrategy.FullMergeability -> ConflictResolutionStrategyDto.FullMergeability
			ConflictResolutionStrategy.LatestRevision -> ConflictResolutionStrategyDto.LatestRevision
		}
}
