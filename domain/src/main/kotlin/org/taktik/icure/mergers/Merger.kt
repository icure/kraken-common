package org.taktik.icure.mergers

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.HasDataAttachments
import org.taktik.icure.entities.embed.DeletedAttachment
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.objectstorage.DataAttachment
import org.taktik.icure.entities.utils.MergeUtil
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct

abstract class Merger<T> {
	abstract fun canMerge(l: T?, r: T?): Boolean
	abstract fun merge(l: T?, r: T?): T?

	// region mergeUtils
	protected fun mergeDeletedAttachments(l: List<DeletedAttachment>, r: List<DeletedAttachment>): List<DeletedAttachment> =
		MergeUtil.mergeListsDistinct(
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

	protected inline fun <F : Identifiable<String>> canMergeCollectionsOfMergeable(
		l: Collection<F>?,
		r: Collection<F>?,
		canMerge: (F?, F?) -> Boolean
	): Boolean {
		val visited = mutableSetOf<String>()
		val visitLeft = l?.all { leftItem ->
			visited.add(leftItem.id)
			canMerge(leftItem, r?.firstOrNull { it.id == leftItem.id })
		} ?: true
		val visitedRemainingRight = r?.filter {
			it.id !in visited
		}?.all { rightItem ->
			canMerge(l?.firstOrNull { it.id == rightItem.id }, rightItem)
		} ?: true
		return visitLeft && visitedRemainingRight
	}

	protected inline fun <K, F> canMergeMapsOfMergeable(
		l: Map<K, F>?,
		r: Map<K, F>?,
		canMerge: (F?, F?) -> Boolean
	): Boolean {
		val visited = mutableSetOf<K>()
		val visitLeft = l?.all { (k, v) ->
			visited.add(k)
			canMerge(v, r?.get(k))
		} ?: true
		val visitedRemainingRight = r?.filterKeys { k ->
			k !in visited
		}?.all { (k, v) ->
			canMerge(l?.get(k), v)
		} ?: true
		return visitLeft && visitedRemainingRight
	}

	protected inline fun <K, F : Identifiable<String>> canMergeMapsOfMergeableCollections(
		l: Map<K, Collection<F>>?,
		r: Map<K, Collection<F>>?,
		canMerge: (F?, F?) -> Boolean
	): Boolean {
		val visited = mutableSetOf<K>()
		val visitLeft = l?.all { (k, v) ->
			visited.add(k)
			canMergeCollectionsOfMergeable(v, r?.get(k), canMerge)
		} ?: true
		val visitedRemainingRight = r?.filterKeys { k ->
			k !in visited
		}?.all { (k, v) ->
			canMergeCollectionsOfMergeable(l?.get(k), v, canMerge)
		} ?: true
		return visitLeft && visitedRemainingRight
	}

	// endregion
}