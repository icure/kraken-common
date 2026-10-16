package org.taktik.icure.asynclogic.impl.customentities

import org.taktik.icure.asyncdao.CustomEntityDAO
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogicHelper
import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentModificationLogic
import org.taktik.icure.entities.CustomEntityBase

object CustomEntityLogicContextHelper {
	interface Key<T : Any> {
		/**
		 * A error message used when attempting to get this helper from a context that does not have it
		 */
		val errorMessageInvalid: String
	}

	data object EncryptionMetadata : Key<EntityWithEncryptionMetadataLogicHelper<CustomEntityBase, CustomEntityDAO>> {
		override val errorMessageInvalid get() =
			"This method can only be used with delegation-based custom entities"
	}

	data object Attachment : Key<CustomEntityDataAttachmentModificationLogic> {
		override val errorMessageInvalid get() =
			"This entity does not support attachments"
	}

	@JvmInline
	value class Builder @PublishedApi internal constructor(
		@PublishedApi internal val map: MutableMap<Key<*>, Any>,
	) {
		fun <T : Any> put(key: Key<T>, value: T) {
			check(map.putIfAbsent(key, value) == null) {
				"Duplicate helper for key $key"
			}
		}
	}

	inline fun build(builderBlock: Builder.() -> Unit): Map<Key<*>, Any> {
		val map = mutableMapOf<Key<*>, Any>()
		Builder(map).builderBlock()
		return map
	}
}