package org.taktik.icure.mergers

import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct

abstract class Merger<T> {
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

	protected fun <D : HasDataAttachments<D>> solveDataAttachmentsConflicts(
		l: HasDataAttachments<D>?,
		r: HasDataAttachments<D>?
	): Map<String, DataAttachment> =
		if (l != null && r != null) l.solveDataAttachmentsConflicts(r)
		else emptyMap()

	protected fun mergeListOfStringsIgnoringCase(l: List<String>, r: List<String>): List<String> =
		mergeListsDistinct(l, r, { a, b -> a.equals(b, true) }, { a, _ -> a })
	// endregion

	// region canMergeUtils
	protected fun <F> canMergeNonMergeableField(l: F?, r: F?): Boolean = (l == null || r == null || l == r)

	protected inline fun <F> canMergeCollectionsOfMergeable(
		l: Collection<F>?,
		r: Collection<F>?,
		canMerge: (F, F) -> Boolean,
		comparator: (F, F) -> Boolean
	): Boolean {
		// If either are null, I can always merge setting the other
		if (l == null || r == null) {
			return true
		}

		val mutableRight = r.toMutableList()
		val visitLeft = l.all { leftItem ->
			val rightItem = mutableRight.firstOrNull {
				comparator(it, leftItem)
			}?.also {
				mutableRight.remove(it)
			}
			rightItem == null || canMerge(leftItem, rightItem)
		}
		val visitedRemainingRight = mutableRight.all { rightItem ->
			val leftItem = l.firstOrNull { comparator(it, rightItem) }
			leftItem == null || canMerge(leftItem, rightItem)
		}
		return visitLeft && visitedRemainingRight
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

		val visited = mutableSetOf<K>()
		val visitLeft = l.all { (k, v) ->
			visited.add(k)
			r[k] == null || canMerge(v, r.getValue(k))
		}
		val visitedRemainingRight = r.filterKeys { k ->
			k !in visited
		}.all { (k, v) ->
			l[k] == null || canMerge(l.getValue(k), v)
		}
		return visitLeft && visitedRemainingRight
	}

	protected inline fun <K, F> canMergeMapsOfMergeableCollections(
		l: Map<K, Collection<F>>?,
		r: Map<K, Collection<F>>?,
		canMerge: (F, F) -> Boolean,
		comparator: (F, F) -> Boolean,
	): Boolean {
		// If either are null, I can always merge setting the other
		if (l == null || r == null) {
			return true
		}

		val visited = mutableSetOf<K>()
		val visitLeft = l.all { (k, v) ->
			visited.add(k)
			canMergeCollectionsOfMergeable(v, r[k], canMerge, comparator)
		}
		val visitedRemainingRight = r.filterKeys { k ->
			k !in visited
		}.all { (k, v) ->
			canMergeCollectionsOfMergeable(l[k], v, canMerge, comparator)
		}
		return visitLeft && visitedRemainingRight
	}

	// endregion
}