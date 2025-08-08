package org.taktik.icure.utils

/**
 * This class represents the root of a pattern matcher based on a tree structure.
 * It matches the path passed as parameter, removes extra slashed and, if part of the path was not found, it appends it
 * to the matched part (to support searching for wrong urls).
 */
class TreePathMatcher : TreePath("", Regex("."), false) {
	override fun match(segments: List<String>): String = matchPath(MatchResult(notMatched = segments))?.let { match ->
		buildString {
			append(match.path.let { it.substring(1, it.length) })
			match.notMatched.takeIf { it.isNotEmpty() }?.also { append("/") }
			append(match.notMatched.joinToString("/"))
		}
	} ?: segments.joinToString("/")
}

/**
 * This class represents a path and its subpaths in a tree structure.
 * @param segment is the value of the segment as specified in the SpringBoot controller
 * @param matcher a Regex used to match the current segment against the one provided in the matching phase.
 * @param isParameter whether this segment represents a path parameter.
 * @param children a List containing all the children segments. (eg. in the path "/rest/v1/icure", icure is a child of v1, that is a child of rest).
 * The children are ordered lexicographically, so the path parameters always come last.
 * @param isComplete whether this is the terminal node of a valid path or just an intermediate node. (eg. considering the paths "/rest/v1/icure"
 * and "/rest/v1/icure/test", icure and test are both terminal nodes, while rest and v1 are not).
 */
open class TreePath(
	val segment: String,
	val matcher: Regex,
	private val isParameter: Boolean,
	val children: MutableList<TreePath> = mutableListOf(),
	private var isComplete: Boolean = false,
) {

	/**
	 * Adds a new path to the tree recursively and in place.
	 * @param segments a List of all the segments that compose the path to add.
	 */
	fun addChildren(segments: List<String>) {
		if (segments.isNotEmpty()) {
			val segment = segments.first()
			val existingNode = children.firstOrNull { it.segment == segment }
				?: TreePath(
					segment,
					Regex("[^/]+".takeIf { segment.startsWith("{") } ?: "^$segment$"),
					segment.startsWith("{"),
				).also { children.add(it) }
			existingNode.addChildren(segments.drop(1))
			children.sortBy { it.segment }
		} else {
			isComplete = true
		}
	}

	/**
	 * Matches a path against the whole tree, returning the longest match. The longest match is defined as the one that
	 * leaves out the least number of segments (see [MatchResult]). If two matches leaves out the same number of segments, then the one with the
	 * least path parameters in it is preferred, as path parameters have the least strict match. If two paths also have the same number of path
	 * parameters, then the one that comes first in a lexicographical order is returned.
	 * @param matchResult a [MatchResult]
	 * @return a copy of the [MatchResult] or null if no match was possible.
	 */
	protected fun matchPath(matchResult: MatchResult): MatchResult? = when {
		matchResult.notMatched.isEmpty() && isComplete -> {
			MatchResult(
				"${matchResult.path}/$segment",
				matchResult.notMatched,
				matchResult.parameters.takeIf { !isParameter } ?: (matchResult.parameters + 1),
			)
		}
		matchResult.notMatched.isEmpty() && !isComplete -> null
		else -> {
			children
				.filter { matchResult.notMatched.first().matches(it.matcher) }
				.mapNotNull {
					it.matchPath(
						MatchResult(
							"${matchResult.path}/$segment",
							matchResult.notMatched.drop(1),
							matchResult.parameters.takeIf { !isParameter } ?: (matchResult.parameters + 1),
						),
					)
				}.sortedWith(compareBy({ it.notMatched.size }, { it.parameters }))
				.firstOrNull() ?: MatchResult(
				"${matchResult.path}/$segment",
				matchResult.notMatched,
				matchResult.parameters.takeIf { !isParameter } ?: (matchResult.parameters + 1),
			)
		}
	}

	/**
	 * Matches a path against the whole tree, returning the longest match. The longest match is defined as the one that
	 * leaves out the least number of segments (see [MatchResult]). If two matches leaves out the same number of segments, then the one with the
	 * least path parameters in it is preferred, as path parameters have the least strict match. If two paths also have the same number of path
	 * parameters, then the one that comes first in a lexicographical order is returned.
	 * @param segments a list containing all the segments of a path.
	 * @return the longest match plus the part that was not matched.
	 */
	open fun match(segments: List<String>): String = matchPath(MatchResult(notMatched = segments))?.path ?: segments.joinToString("/")
}

/**
 * This data class represents the state of a matching operation.
 * @param path the longest path matched up to now.
 * @param notMatched the segments that were not matched.
 * @param parameters the number of path parameters present in the current match.
 */
data class MatchResult(
	val path: String = "",
	val notMatched: List<String> = emptyList(),
	val parameters: Int = 0,
)
