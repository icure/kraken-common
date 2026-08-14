package org.taktik.icure.spring.encoder

import com.fasterxml.jackson.databind.ObjectMapper
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.util.MimeType
import org.taktik.icure.pagination.AbortedPageElement
import org.taktik.icure.pagination.MultiKeyPaginatedFlux
import org.taktik.icure.pagination.MultiKeyPaginationElement
import org.taktik.icure.pagination.NextPageElement
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.PaginationError
import org.taktik.icure.pagination.PaginationRowElement
import org.taktik.icure.services.external.rest.v2.dto.MultiKeyPaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PaginationErrorDto
import reactor.core.publisher.Flux
import java.io.Serializable

/**
 * This class extends the behaviour of the [Jackson2JsonEncoder] handling different the encoding of a [PaginatedFlux]
 * or of a [MultiKeyPaginatedFlux].
 * In this simple implementation, it collects the [Flux] and handles it as a mono of [PaginatedList] or
 * [MultiKeyPaginatedList] respectively.
 */
class PaginatedCollectingJackson2JsonEncoder(
	private val mapper: ObjectMapper,
	vararg mimeTypes: MimeType,
) : Jackson2JsonEncoder(mapper, *mimeTypes) {
	override fun encode(
		inputStream: Publisher<*>,
		bufferFactory: DataBufferFactory,
		elementType: ResolvableType,
		mimeType: MimeType?,
		hints: MutableMap<String, Any>?,
	): Flux<DataBuffer> = when (inputStream) {
		is PaginatedFlux<*> -> {
			var nextPageElement: NextPageElement<*>? = null
			var error: PaginationError? = null
			inputStream
				// A cursor and an aborted page are both terminal: whatever the flux still has to emit is ignored, the
				// same way the streaming encoder can't write anything once it closed the rows array.
				.takeUntil { it is NextPageElement<*> || it is AbortedPageElement }
				.mapNotNull {
					when (it) {
						is NextPageElement<*> -> {
							nextPageElement = it
							null
						}
						is AbortedPageElement -> {
							error = it.error
							null
						}
						is PaginationRowElement<*, *> -> it.element as Serializable
					}
				}.collectList()
				.map { rows ->
					PaginatedList(
						rows = rows,
						nextKeyPair =
						nextPageElement?.let {
							PaginatedDocumentKeyIdPair(
								startKey = it.startKey?.let { sk -> mapper.valueToTree(sk) },
								startKeyDocId = it.startKeyDocId,
							)
						},
						error = error?.asDto(),
					)
				}.let {
					super.encode(it, bufferFactory, elementType, mimeType, hints)
				}
		}
		is MultiKeyPaginatedFlux<*, *> -> {
			var nextPageElement: MultiKeyPaginationElement.NextPage<*>? = null
			var error: PaginationError? = null
			inputStream
				// A cursor and an aborted page are both terminal: whatever the flux still has to emit is ignored, the
				// same way the streaming encoder can't write anything once it closed the rows array.
				.takeUntil { it is MultiKeyPaginationElement.NextPage<*> || it is MultiKeyPaginationElement.Aborted }
				.mapNotNull {
					when (it) {
						is MultiKeyPaginationElement.NextPage<*> -> {
							nextPageElement = it
							null
						}
						is MultiKeyPaginationElement.Aborted -> {
							error = it.error
							null
						}
						is MultiKeyPaginationElement.Row<*> -> it.element as Serializable
					}
				}.collectList()
				.map { rows ->
					MultiKeyPaginatedList(
						rows = rows,
						nextPage =
						nextPageElement?.let {
							MultiKeyPaginatedList.NextPage(
								nextDocId = it.nextDocId,
								nextKeys = it.nextKeys,
							)
						},
						error = error?.asDto(),
					)
				}.let {
					super.encode(it, bufferFactory, elementType, mimeType, hints)
				}
		}
		else -> super.encode(inputStream, bufferFactory, elementType, mimeType, hints)
	}

	private fun PaginationError.asDto() = PaginationErrorDto(
		statusCode = statusCode,
		message = message,
		exceptionDetail = exceptionDetail,
	)
}
