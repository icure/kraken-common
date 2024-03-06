package org.taktik.icure.spring.encoder

import com.fasterxml.jackson.databind.ObjectMapper
import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.util.MimeType
import org.taktik.icure.pagination.NextPageElement
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.PaginationRowElement
import org.taktik.icure.services.external.rest.v1.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v1.dto.PaginatedList
import reactor.core.publisher.Flux
import java.io.Serializable

/**
 * This class extends the behaviour of the [Jackson2JsonEncoder] handling different the encoding of a [PaginatedFlux].
 * In this simple implementation, it collects the [Flux] and handles it as a mono of [PaginatedList]
 */
class PaginatedCollectingJackson2JsonEncoder(
	mapper: ObjectMapper,
	vararg mimeTypes: MimeType
) : Jackson2JsonEncoder(mapper, *mimeTypes) {

	override fun encode(
		inputStream: Publisher<*>,
		bufferFactory: DataBufferFactory,
		elementType: ResolvableType,
		mimeType: MimeType?,
		hints: MutableMap<String, Any>?
	): Flux<DataBuffer> = if(inputStream is PaginatedFlux) {
		var nextPageElement: NextPageElement<*>? = null
		inputStream.mapNotNull {
			when(it) {
				is NextPageElement<*> -> {
					nextPageElement = it
					null
				}
				is PaginationRowElement<*, *> -> it.element as Serializable
			}
		}.collectList().map { rows ->
			PaginatedList(
				rows = rows,
				nextKeyPair = nextPageElement?.let {
					PaginatedDocumentKeyIdPair(
						startKey = it.startKey,
						startKeyDocId = it.startKeyDocId
					)
				}
			)
		}.let {
			super.encode(it, bufferFactory, elementType, mimeType, hints)
		}
	} else super.encode(inputStream, bufferFactory, elementType, mimeType, hints)

}