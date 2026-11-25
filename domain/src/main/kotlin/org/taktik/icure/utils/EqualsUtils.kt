package org.taktik.icure.utils

import java.time.Duration
import java.time.Instant
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

data class Diff(val propertyName: String, val diffs: List<Diff> = listOf()) {
	fun toString(prefix: String = ""): String = "$prefix$propertyName, diffs=\n${diffs.joinToString("\n") { prefix + it.toString("  $prefix") }})"
}

fun <K : Any> K.differences(o: K?): List<Diff> {
	val self: K = this
	return o?.let { other ->
		if (self is List<*>) {
			(this as List<*>).mapIndexedNotNull { idx, v ->
				when {
					idx > (other as List<*>).size -> Diff("$idx <-> missing", listOf())
					v != other[idx] -> Diff("$idx <-> $idx", v?.differences(other[idx]) ?: listOf())
					else -> null
				}
			} +
				if ((other as List<*>).size > (this as List<*>).size) {
					other.takeLast(other.size - this.size).mapIndexed { idx, _ ->
						Diff("missing <-> ${idx + this.size}", listOf())
					}
				} else {
					listOf()
				}
		} else if (self is Set<*>) {
			val notInSelf = (other as Set<*>) - self
			val notInOther = self - (other as Set<*>)

			val othersTreated = mutableSetOf<Any>()

			notInSelf.map { s ->
				Diff(
					"<-",
					notInOther.map { o -> o to (s?.differences(o) ?: listOf()) }.minByOrNull { it.second.size }?.let {
						it.first?.let { it1 -> othersTreated.add(it1) }
						it.second.toList()
					} ?: listOf(),
				)
			}.toList() +
				(notInOther - othersTreated).map { o ->
					Diff(
						"->",
						notInSelf.map { s -> s?.differences(o) ?: listOf() }.minByOrNull { it.size }?.toList()
							?: listOf(),
					)
				}.toList()
		} else {
			val props: Collection<KProperty1<Any, *>> = try {
				(this::class).memberProperties as Collection<KProperty1<Any, *>>
			} catch (e: Exception) {
				listOf()
			}
			props.filter {
				try {
					it.get(self) != it.get(other)
				} catch (e: Exception) {
					false
				}
			}.map { kp ->
				Diff(kp.name, kp.get(self)?.let { s -> (s as Any?)?.differences((kp.get(other))) } ?: listOf())
			}
		}
	} ?: listOf()
}

fun Instant.between(start: Instant, end: Instant): Boolean = this.isAfter(start) && this.isBefore(end)

/**
 * Whether this instant is strictly after [start] and less than [seconds] seconds after it: the same as
 * `between(start, start.plusSeconds(seconds))`, but without computing the end instant, which overflows (and throws)
 * when [seconds] is large.
 */
fun Instant.isWithinSecondsAfter(start: Instant, seconds: Long): Boolean =
	this.isAfter(start) && Duration.between(start, this) < Duration.ofSeconds(seconds)
