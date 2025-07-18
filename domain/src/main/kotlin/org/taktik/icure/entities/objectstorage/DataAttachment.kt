package org.taktik.icure.entities.objectstorage

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.flow.Flow
import org.taktik.commons.uti.UTI

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Represent an attachment holding some additional data for an entity.
 * At least one of [couchDbAttachmentId] or [objectStoreAttachmentId] is always not null.
 * @property couchDbAttachmentId if the attachment is stored as a couchdb attachment this holds the id of the attachment, else null.
 * @property objectStoreAttachmentId if the attachment is stored with the object storage service this holds the id of the attachment, else null.
 * @property utis [Uniform Type Identifiers](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/understanding_utis/understand_utis_conc/understand_utis_conc.html#//apple_ref/doc/uid/TP40001319-CH202-CHDHIJDE) for the data attachment.
 * This is a list in order to allow specifying a priority, but each uti must be unique.
 */
data class DataAttachment(val couchDbAttachmentId: String? = null, val objectStoreAttachmentId: String? = null, val utis: List<String> = emptyList()) {
	init {
		require(couchDbAttachmentId != null || objectStoreAttachmentId != null) {
			"Must specify the id of at least one storage place for the attachment"
		}
		require(utis.distinct().size == utis.size) {
			val duplicates = utis.groupingBy { it }.eachCount().filter { it.value > 1 }.toList()
			"There are duplicate utis: $duplicates"
		}
	}

	companion object {
		/**
		 * Default mime type for data attachments, if no specific uti is provided.
		 */
		const val DEFAULT_MIME_TYPE = "application/octet-stream"
	}

	@JsonIgnore
	private var cachedBytes: ByteArray? = null

	@get:JsonIgnore
	val ids: Pair<String?, String?> get() = couchDbAttachmentId to objectStoreAttachmentId

	/**
	 * Get the mime type string for this attachment. If the attachment does not specify a UTI with a valid mime type returns null.
	 */
	@get:JsonIgnore
	val mimeType: String? get() =
		utis.asSequence().mapNotNull(UTI::get).flatMap { it.mimeTypes ?: emptyList() }.firstOrNull()

	/**
	 * [mimeType] or [DEFAULT_MIME_TYPE].
	 */
	@get:JsonIgnore
	val mimeTypeOrDefault: String get() =
		mimeType ?: DEFAULT_MIME_TYPE

	/**
	 * @return if this and other attachment have the same ids (the attachment is the same and is stored in the same place)
	 */
	infix fun hasSameIdsAs(other: DataAttachment) = this.ids == other.ids

	/**
	 * @return a copy of this data attachment where the ids are replaced with those of another data attachment
	 */
	fun withIdsOf(other: DataAttachment) = copy(
		couchDbAttachmentId = other.couchDbAttachmentId,
		objectStoreAttachmentId = other.objectStoreAttachmentId,
	)

	/**
	 * Get the attachment content as bytes. If the content has been cached immediately returns it, otherwise loads the content from
	 * the dataProvider and caches it. This method is not intended to be used directly, as the appropriate implementation of flow
	 * provider is not trivial, you should instead use DataAttachmentLoader.contentBytesOf.
	 */
	suspend fun contentBytesFromCacheOrLoadAndStore(dataProvider: suspend () -> ByteArray): ByteArray = cachedBytes ?: dataProvider().also { cachedBytes = it }

	/**
	 * Get the attachment content as a flow. If the content has been cached immediately returns a flow which wraps its content,
	 * otherwise loads the flow directly from the dataProvider. This method is not intended to be used directly, as the appropriate
	 * implementation of flow provider is not trivial, you should instead use DataAttachmentLoader.contentFlowOf.
	 */
	fun <T> contentFlowFromCacheOrLoad(dataProvider: () -> Flow<T>, unmarshaller: (data: ByteArray) -> Flow<T>): Flow<T> = cachedBytes?.let { unmarshaller(it) } ?: dataProvider()
}
