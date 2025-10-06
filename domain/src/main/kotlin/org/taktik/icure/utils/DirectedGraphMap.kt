package org.taktik.icure.utils

import java.util.ArrayDeque

/**
 * Represent a directed graph as a map.
 * Each key is a vertex, and each value is the vertices that can be reached from the key.
 */
typealias DirectedGraphMap<T> = Map<T, Set<T>>

/**
 * Reverses the direction of a graph map edges.
 */
fun <T> DirectedGraphMap<T>.reversed(): DirectedGraphMap<T> = edgesToGraphMap(this.flatMap { (from, tos) -> tos.map { it to from } })

/**
 * Finds if there is a path that starting from the provided vertex reaches it again.
 * @param vertex a vertex in the graph.
 * @return true if there is a loop from the start vertex.
 */
fun <T> DirectedGraphMap<T>.hasLoopTo(vertex: T): Boolean = vertex in reachSetExcludingZeroLength(vertex)

/**
 * @return if the graph has one or more loops.
 */
fun <T> DirectedGraphMap<T>.hasLoops(): Boolean = this.keys.any { hasLoopTo(it) }

/**
 * Finds all vertices that have at least a path that starts from them and can reach them again.
 * @return the vertices that are part of one or more loops.
 */
fun <T> DirectedGraphMap<T>.findLoopVertices(): Set<T> = this.keys.filter { hasLoopTo(it) }.toSet()

fun <T> DirectedGraphMap<T>.distancesFrom(vertex: T): Map<T, Int> {
	val res = mutableMapOf<T, Int>()
	val visited = mutableSetOf<T>()
	val toVisit = ArrayDeque<Pair<T, Int>>()

	toVisit.add(vertex to 0)
	while (toVisit.isNotEmpty()) {
		val (curr, dist) = toVisit.removeFirst()
		if (curr !in visited) {
			visited += curr
			res[curr] = dist
			this[curr]?.forEach { neighbor ->
				if (neighbor !in visited) {
					toVisit.addLast(neighbor to dist + 1)
				}
			}
		}
	}
	return res
}

/**
 * Get the set of vertices that can be reached with a path that starts from the provided vertex.
 * This does not include the input vertex unless there is a path longer than 0 that can reach it (there is a loop that
 * includes the input vertex).
 * @param vertex a vertex in the graph.
 * @return the vertices that can be reached from the provided vertex.
 */
fun <T> DirectedGraphMap<T>.reachSetExcludingZeroLength(vertex: T): Set<T> {
	val visited = mutableSetOf<T>()
	val toVisit = edgesOf(vertex).toMutableSet()
	while (toVisit.isNotEmpty()) {
		visited += toVisit
		val neighbours = toVisit.flatMap { edgesOf(it) }.toSet()
		toVisit += neighbours
		toVisit -= visited
	}
	return visited
}

/**
 * Get the set of vertices that can be reached with a path that starts from the provided vertex.
 * Always includes the input vertex, since there is always a path of length 0 to it.
 * @param vertex a vertex in the graph.
 * @return the vertices that can be reached from the provided vertex.
 */
fun <T> DirectedGraphMap<T>.reachSet(vertex: T): Set<T> = reachSetExcludingZeroLength(vertex) + vertex

/**
 * @return the set of vertices that can't be reached from any other vertices
 */
fun <T> DirectedGraphMap<T>.roots(): Set<T> = keys - values.flatten().toSet()

/**
 * Get all possible paths starting from the roots of the graph. Can't be used with graph-map that contain loops.
 * @return all pats from all roots.
 * @throws IllegalArgumentException if the path contains loops.
 */
fun <T> DirectedGraphMap<T>.paths(): List<List<T>> {
	require(!hasLoops()) { "Paths is not supported on graphs with loops." }
	fun pathsFrom(vertex: T): Sequence<List<T>> = edgesOf(vertex).let { neighbours ->
		if (neighbours.isEmpty()) {
			sequenceOf(listOf(vertex))
		} else {
			neighbours.asSequence().flatMap { pathsFrom(it) }.map { listOf(vertex) + it }
		}
	}
	return roots().flatMap { pathsFrom(it) }
}

/**
 * Makes a graph map given the edges. Duplicate edges are ignored.
 * @param edges edges of the graph, in the form `source to destination`.
 * @return a graph consisting of all vertex that appears in the given edges, and the given edges.
 */
fun <T> edgesToGraphMap(vararg edges: Pair<T, T>): DirectedGraphMap<T> = edgesToGraphMap(edges.toList())

/**
 * Makes a graph map given the edges. Duplicate edges are ignored.
 * @param edges edges of the graph, in the form `source to destination`.
 * @return a graph consisting of all vertex that appears in the given edges, and the given edges.
 */
fun <T> edgesToGraphMap(edges: Collection<Pair<T, T>>): DirectedGraphMap<T> {
	val allVertices = edges.flatMap { listOf(it.first, it.second) }
	val graphOnlyWithEdges = edges
		.groupBy { it.first }
		.mapValues { it.value.map { (_, d) -> d }.toSet() }
	return allVertices.associateWith { graphOnlyWithEdges[it] ?: emptySet() }
}

private fun <T> DirectedGraphMap<T>.edgesOf(vertex: T): Set<T> = this[vertex] ?: emptySet()
