package org.taktik.icure.services.external.rest.v2.dto.conflicts

/**
 * Describes the strategy that should be used when automatically resolving the conflicting revisions of an entity.
 *
 * Maps to `org.taktik.icure.entities.conflicts.ConflictResolutionStrategy`.
 */
enum class ConflictResolutionStrategyDto {
	/**
	 * Attempts to merge all the conflicting revisions of an entity into a single one. Revisions that cannot be merged
	 * with the others are left untouched. This is the default strategy.
	 */
	FullMergeability,

	/**
	 * Does not attempt any merge: it keeps the most recent revision (the one with the greatest revision ordinal) and
	 * purges all the others. If multiple revisions share the greatest ordinal the resolution fails.
	 */
	LatestRevision
}
