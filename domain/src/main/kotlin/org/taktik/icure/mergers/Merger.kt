package org.taktik.icure.mergers

import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.entities.utils.MergeUtil.mergeSets
import org.taktik.icure.entities.utils.MergeUtil.startIndexOfContiguousSublistOrNull
import org.taktik.icure.mergers.annotations.Mergeable

/**
 * Base class for all the auto-generated mergers. A merger will be generated for each entity annotated with [Mergeable].
 * It specifies 2 methods:
 * - [canMerge] to check if 2 mergeable entities can be safely merged. Some entities may not be mergeable because the
 * content of some field is ambiguous and their automatic merging may lead to loss of data or undesired results from the
 * point of view of the user.
 * - [merge] automatically merges two entities, this must be called only after checking that the 2 entities are automatically
 * mergeable through [canMerge].
 * Both merge and canMerge are evaluated on a per-field basis. Each type has its own default strategy. The default
 * strategy can be overridden by setting an annotation on the field, at the level of the interface where the field is
 * defined. Each merge strategy defines the behavior of canMerge and merge.
 * Strategies are defined in the [org.taktik.icure.mergers.annotations] package.
 *
 * Each mergeable class has a set of fields that can be used to identify the objects of that class in collections.
 * Two mergeable objects with the same values in the identifier fields are considered equal.
 *
 * For [canMerge]:
 * * If a merge strategy is defined on the parameter, it follows the rules defined by the merge strategy.
 * * If a field is an identifier field and does not have a merge strategy, then it must be equal on both entities for them to be mergeable.
 * * If is @Mergeable → call inner Merger.canMerge(), if both of them are non-null, else true.
 * * If is non @Mergeable, non-collection: either one of them is null or content equals.
 * * If the field is a List:
 *     * If the inner type is a Mergeable class, then the fields are mergeable if one of the two is a contiguous sublist of the other. Equality is evaluated on the basis of the identifier fields.
 *      Mergeability between elements of the list is delegated to the merger if the entity is not duplicated in the list or with == if there are duplicates of that entity.
 *      * If the inner type is a non-Mergeable, non-collection class, then the fields are mergeable if one of the two is a contiguous sublist of the other, evaluating equality with ==.
 *      * If the inner type is a collection, an error will be thrown at generation time.
 * * If the field is a Set:
 *     * If the inner type is a Mergeable class and there are duplicated entities in the set, based on the identifier fields, then the same duplicated entities must appear in both sets.
 *      Equality is evaluated using ==. If there are no duplicates or the duplicates match exactly, then all the mergeability of all the entities common to both sets is delegated to the merger of the class.
 *      * If the inner type is a  non-Mergeable, non-collection class, then it is always mergeable.
 *      * If the inner type is a collection, an error will be thrown at generation time.
 * * If the field is a Map:
 *     * If value is Mergeable, the fields are mergeable if the value corresponding to the keys appearing in both maps, evaluated using the merger of the class.
 *     * If value is any-non collection, map is mergeable if the value corresponding to the keys appearing in both maps must be equals.
 *     * If value is List or Set, then the mergeability of the values corresponding to the keys appearing in both maps is delegated to the List/Set strategy.
 *
 * For [merge], providing that the 2 entities are mergeable:
 * * id and rev are always from the left entity.
 * * If a merge strategy is defined on the property, follow the behavior defined by the strategy.
 * * If the field is a Mergeable non-collection, delegate to the merger for that type.
 * * If the field is a non-Mergeable, non-collection, take left ?: right (as either one of them is null or they are equals).
 * * If the field is a List:
 *      * If the inner type is a Mergeable class, find the common sublist between the 2 lists. The element in the sublist are merged using the merger for the type. Any prefix / suffix remaining is simply prepended / appended to the result.
 *      * If the inner type is a non-Mergeable class, take the longest between the two (as the shortest is a sublist of the other, verified by [canMerge]).
 * * If the field is a Set:
 *      * If the inner type is a Mergeable class, delegate to [mergeSets] of MergeUtils, comparing each element using the identifier fields if there are no duplicates of it, using == otherwise.
 *      * If the inner type is a non-Mergeable class, add the 2 sets.
 * * If the field is a Map:
 *      * If the value is a Mergeable class, merge all the values corresponding to the keys common to the maps using the merger for the class.
 *      * If the value is a non-Mergeable, non-collection, sum the 2 maps as the [canMerge] already verified that the values of the common keys are equal.
 *      * If the value is a collection, delegateto the appropriate List/set merging method.
 *
 */
abstract class Merger<T : Any> {
	abstract fun canMerge(left: T, right: T): Boolean
	abstract fun merge(left: T, right: T): T

	// region mergeUtils
	protected fun mergeDeletedAttachments(l: List<DeletedAttachment>, r: List<DeletedAttachment>): List<DeletedAttachment> =
		mergeListsDistinct(
			l,
			r,
			comparator = { a, b -> a.key == b.key && a.objectStoreAttachmentId == b.objectStoreAttachmentId && a.couchDbAttachmentId == b.couchDbAttachmentId },
		)

	protected fun mergeSecurityMetadata(l: SecurityMetadata?, r: SecurityMetadata?): SecurityMetadata? =
		l?.let { thisSecurityMetadata ->
			r?.let { otherSecurityMetadata ->
				thisSecurityMetadata.mergeForDifferentVersionsOfEntity(otherSecurityMetadata)
			} ?: thisSecurityMetadata
		} ?: r

	// TODO the current implementation considers the data attachments non mergeable. If in future we add a consistent way of getting the digest for each attachment, we can actually merge them. (See also canMergeDataAttachments)
	protected fun <D : HasDataAttachments<D>> solveDataAttachmentsConflicts(
		l: HasDataAttachments<D>?,
		r: HasDataAttachments<D>?
	): Map<String, DataAttachment> =
		if (l != null && r != null) {
			l.dataAttachments + r.dataAttachments
		}
		else {
			l?.dataAttachments ?: r?.dataAttachments ?: emptyMap()
		}

	protected fun mergeListOfStringsIgnoringCase(l: List<String>, r: List<String>): List<String> =
		mergeListsDistinct(l, r, { a, b -> a.equals(b, true) }, { a, _ -> a })

	protected inline fun <K, F> mergeMapsOfMergeable(
		l: Map<K, F>,
		r: Map<K, F>,
		merge: (F, F) -> F
	): Map<K, F> {
		val merged = mutableMapOf<K, F>()
		l.forEach { (k, leftValue) ->
			if (!r.containsKey(k)) {
				merged[k] = leftValue
			} else {
				val rightValue = r.getValue(k)
				merged[k] = merge(leftValue, rightValue)
			}
		}
		r.filterKeys { k ->
			!merged.containsKey(k)
		}.forEach { (k, v) ->
			merged[k] = v
		}
		return merged
	}

	protected inline fun <M, ID> mergeListsOfMergeable(
		l: List<M>,
		r: List<M>,
		merge: (M, M) -> M,
		canMerge: (M, M) -> Boolean,
		idEquals: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): List<M> {
		val (base, sublist) = if (l.size >= r.size) l to r else r to l
		val startIndex = indexOfMergeableSublistWithDuplicates(
			l = base,
			r = sublist,
			canMerge = canMerge,
			idEquals = idEquals,
			idGetter = idGetter,
		) ?: throw IllegalStateException("One list is not a subset of the other")
		return base.subList(0, startIndex) +
			base.subList(startIndex, startIndex + sublist.size).zip(sublist).map { (a, b) -> merge(a, b) } +
			base.subList(startIndex + sublist.size, base.size)
	}

	protected fun <M, ID> mergeSetsOfMergeable(
		l: Set<M>,
		r: Set<M>,
		merge: (M, M) -> M,
		idEquals: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Set<M> {
		val duplicates = idsOfDuplicatesInCollections(l = l, r = r, idGetter = idGetter)
		return mergeSets(
			a = l,
			b = r,
			comparator = { a, b ->
				when {
					!idEquals(a, b) -> false
					idGetter(a) in duplicates -> a == b
					else -> true
				}
			},
			merger = merge,
		)
	}

	protected fun mergeAesExchangeKeys(
		l: Map<String, Map<String, Map<String, String>>>,
		r: Map<String, Map<String, Map<String, String>>>,
	): Map<String, Map<String, Map<String, String>>> = mergeMapsOfMergeable(l, r) { leftDelegates, rightDelegates ->
		leftDelegates + rightDelegates
	}
	// endregion

	// region canMergeUtils
	protected fun canMergeSecurityMetadata(l: SecurityMetadata?, r: SecurityMetadata?): Boolean =
		l?.let { thisSecurityMetadata ->
			r?.let { otherSecurityMetadata ->
				thisSecurityMetadata.canMergeForDifferentVersionsOfEntity(otherSecurityMetadata)
			} ?: true
		} ?: true

	protected inline fun <M, ID> canMergeListsOfMergeable(
		l: List<M>?,
		r: List<M>?,
		canMerge: (M, M) -> Boolean,
		idEquals: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Boolean {
		if (l == null || r == null) {
			return true
		}

		return indexOfMergeableSublistWithDuplicates(
			l = l,
			r = r,
			canMerge = canMerge,
			idEquals = idEquals,
			idGetter = idGetter,
		) != null
	}

	protected inline fun <M, ID> indexOfMergeableSublistWithDuplicates(
		l: List<M>,
		r: List<M>,
		canMerge: (M, M) -> Boolean,
		idEquals: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Int? {
		val duplicates = idsOfDuplicatesInCollections(l = l, r = r, idGetter = idGetter)

		return startIndexOfContiguousSublistOrNull(
			left = l,
			right = r,
			equals = { a, b ->
				when {
					!idEquals(a, b) -> false
					idGetter(a) in duplicates -> a == b
					else -> canMerge(a, b)
				}
			}
		)
	}

	protected inline fun <M, ID> idsOfDuplicatesInCollections(
		l: Collection<M>,
		r: Collection<M>,
		idGetter: (M) -> ID,
	): Set<ID> = l.groupBy { idGetter(it) }.filterValues { it.size > 1 }.mapTo(mutableSetOf()) { it.key } +
		r.groupBy { idGetter(it) }.filterValues { it.size > 1 }.mapTo(mutableSetOf()) { it.key }

	protected inline fun <M, ID> canMergeSetsOfMergeable(
		l: Set<M>?,
		r: Set<M>?,
		canMerge: (M, M) -> Boolean,
		idComparator: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Boolean {
		// If either are null, I can always merge setting the other
		if (l == null || r == null) {
			return true
		}
		val leftDuplicates = l.groupBy { idGetter(it) }.filterValues { it.size > 1 }
		val rightDuplicates = r.groupBy { idGetter(it) }.filterValues { it.size > 1 }

		if (leftDuplicates.keys != rightDuplicates.keys &&
			leftDuplicates.any { (k, v) -> rightDuplicates.getValue(k).toSet() != v.toSet() }) {
			return false
		}

		return l.filter { !leftDuplicates.containsKey(idGetter(it)) }.all { leftItem ->
			val rightItem = r.firstOrNull {
				rightDuplicates.containsKey(idGetter(it)) && idComparator(it, leftItem)
			}
			rightItem == null || canMerge(leftItem, rightItem)
		}
	}

	protected inline fun <K, F> canMergeMapsOfMergeable(
		l: Map<K, F>?,
		r: Map<K, F>?,
		canMerge: (F, F) -> Boolean
	): Boolean {
		// If either are null, I can always merge setting the other
		if (l == null || r == null) {
			return true
		}

		// There is no need to visit right: if a key is only present on r and not l, then it will always be mergeable
		return l.all { (k, v) ->
			r[k] == null || canMerge(v, r.getValue(k))
		}
	}

	protected inline fun <K, M, ID> canMergeMapsOfMergeableLists(
		l: Map<K, List<M>>?,
		r: Map<K, List<M>>?,
		canMerge: (M, M) -> Boolean,
		comparator: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Boolean {
		// If either are null it is always possible to merge
		if (l == null || r == null) {
			return true
		}

		// There is no need to visit right: if a key is only present on r and not l, then it will always be mergeable
		return l.all { (k, v) ->
			canMergeListsOfMergeable(v, r[k], canMerge, comparator, idGetter)
		}
	}

	protected inline fun <K, M, ID> canMergeMapsOfMergeableSets(
		l: Map<K, Set<M>>?,
		r: Map<K, Set<M>>?,
		canMerge: (M, M) -> Boolean,
		comparator: (M, M) -> Boolean,
		idGetter: (M) -> ID
	): Boolean {
		// If either are null it is always possible to merge
		if (l == null || r == null) {
			return true
		}

		// There is no need to visit right: if a key is only present on r and not l, then it will always be mergeable
		return l.all { (k, v) ->
			canMergeSetsOfMergeable(v, r[k], canMerge, comparator, idGetter)
		}
	}

	protected fun <K, V> canMergeMap(
		l: Map<K, V>?,
		r: Map<K, V>?,
	): Boolean = canMergeMapsOfMergeable(l, r) { a, b -> a == b }

	// TODO can be improved like solve
	protected fun canMergeDataAttachments(
		l: Map<String, DataAttachment>,
		r: Map<String, DataAttachment>,
	): Boolean = canMergeMap(l, r)

	protected fun canMergeAesExchangeKeys(
		l: Map<String, Map<String, Map<String, String>>>,
		r: Map<String, Map<String, Map<String, String>>>,
	): Boolean = canMergeMapsOfMergeable(l, r) { leftDelegates, rightDelegates ->
		canMergeMapsOfMergeable(leftDelegates, rightDelegates) { a, b -> a == b }
	}

	// endregion
}