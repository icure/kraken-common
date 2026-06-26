package org.taktik.icure.entities.conflicts

/**
 * Describes the strategy that should be used when automatically resolving the conflicting revisions of an entity.
 *
 * Each strategy is backed by a `ConflictSolver` implementation that decides which revision wins and how the
 * conflicting revisions are merged or purged.
 */
enum class ConflictResolutionStrategy {
	/**
	 * The default strategy. It attempts to merge all the conflicting revisions of an entity into a single one,
	 * leveraging the entity-specific merger. A revision that cannot be merged with the others is left untouched:
	 * depending on how many revisions could be merged the result will be a success, a partial success or a failure.
	 */
	FullMergeability,

	/**
	 * A strategy that does not attempt any merge. Among all the conflicting revisions it keeps the one with the
	 * greatest revision ordinal (i.e. the most recent revision) and purges all the others. If more than one revision
	 * shares the greatest ordinal the strategy cannot pick a single winner and the resolution fails.
	 */
	LatestRevision
}
