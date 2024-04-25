package org.taktik.icure.spring.encoder

import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.core.codec.AbstractEncoder
import org.springframework.core.codec.Hints
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.MediaType
import org.springframework.util.MimeType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException

class FluxStringJsonEncoder : AbstractEncoder<String>(MediaType.APPLICATION_JSON) {

	companion object {
		private class StringArrayJoinHelper {
			private var firstEmitted = false

			fun getPrefix(): String = if(!firstEmitted) {
				firstEmitted = true
				FLUX_PREFIX
			} else FLUX_DELIMITER
		}

		const val FLUX_DELIMITER = ","
		const val FLUX_PREFIX = "["
		const val FLUX_SUFFIX = "]"
	}

	override fun canEncode(elementType: ResolvableType, mimeType: MimeType?): Boolean =
		MimeType.valueOf("application/json") == mimeType
			&& elementType.resolve() == String::class.java
			&& super.canEncode(elementType, mimeType)


	override fun encode(
		inputStream: Publisher<out String>,
		bufferFactory: DataBufferFactory,
		elementType: ResolvableType,
		mimeType: MimeType?,
		hints: MutableMap<String, Any>?
	): Flux<DataBuffer> = Flux.deferContextual { contextView ->
		val hintsToUse = if(contextView.isEmpty) hints else Hints.merge(hints, contextView.javaClass.name, contextView)
		try {
			val helper = StringArrayJoinHelper()

			if(inputStream is Flux<*>) {
				Flux.from(inputStream).map {
					bufferFactory.wrap("${helper.getPrefix()}\"$it\"".toByteArray())
				}.switchIfEmpty(Mono.fromCallable { bufferFactory.wrap(FLUX_PREFIX.toByteArray()) })
				.concatWith(Mono.fromCallable { bufferFactory.wrap(FLUX_SUFFIX.toByteArray()) })
				.doOnNext { dataBuffer ->
					Hints.touchDataBuffer(dataBuffer, hintsToUse, logger)
				}
			} else {
				Flux.from(inputStream).map {
					bufferFactory.wrap(it.toByteArray())
				}.doOnNext { dataBuffer ->
					Hints.touchDataBuffer(dataBuffer, hintsToUse, logger)
				}
			}
		} catch (e: IOException) {
			Flux.error(e)
		}
	}
}