/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.ReceiptService
import org.taktik.icure.entities.embed.ReceiptBlobType
import org.taktik.icure.security.CryptoUtils
import org.taktik.icure.services.external.rest.v1.dto.ReceiptDto
import org.taktik.icure.services.external.rest.v1.mapper.ReceiptMapper
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.writeTo
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream

@RestController
@Profile("app")
@RequestMapping("/rest/v1/receipt")
@Tag(name = "receipt")
class ReceiptController(
	private val receiptService: ReceiptService,
	private val receiptMapper: ReceiptMapper,
) {
	@Operation(summary = "Creates a receipt")
	@PostMapping
	fun createReceipt(
		@RequestBody receiptDto: ReceiptDto,
	) = mono {
		receiptService
			.createReceipt(receiptMapper.map(receiptDto))
			?.let { receiptMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Receipt creation failed.")
	}

	@Operation(summary = "Deletes a receipt")
	@DeleteMapping("/{receiptIds}")
	fun deleteReceipt(
		@PathVariable receiptIds: String,
	) = receiptService.deleteReceipts(receiptIds.split(',').map { IdAndRev(it, null) }).injectReactorContext()

	@Operation(
		summary = "Get an attachment",
		responses = [
			ApiResponse(
				responseCode = "200",
				content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))],
			),
		],
	)
	@GetMapping("/{receiptId}/attachment/{attachmentId}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun getReceiptAttachment(
		@PathVariable receiptId: String,
		@PathVariable attachmentId: String,
		@RequestParam(required = false) enckeys: String?,
	) = mono {
		val attachment =
			ByteArrayOutputStream().use {
				receiptService.getAttachment(receiptId, attachmentId).writeTo(it)
				it.toByteArray()
			}
		if (attachment.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found")
				.also { logger.error(it.message) }
		}
		if (enckeys !== null && enckeys.isNotBlank()) {
			CryptoUtils.decryptAESWithAnyKey(attachment, enckeys.split('.'))
		} else {
			attachment
		}
	}

	@Operation(summary = "Creates a receipt's attachment")
	@PutMapping("/{receiptId}/attachment/{blobType}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setReceiptAttachment(
		@PathVariable receiptId: String,
		@PathVariable blobType: String,
		@RequestParam(required = false) enckeys: String?,
		@get:Schema(type = "string", format = "binary") @RequestBody payload: ByteArray,
	) = mono {
		var encryptedPayload = payload
		if (enckeys?.isNotEmpty() == true) {
			CryptoUtils.encryptAESWithAnyKey(encryptedPayload, enckeys.split(',')[0])?.let { encryptedPayload = it }
		}

		val receipt = receiptService.getReceipt(receiptId)
		if (receipt != null) {
			receiptMapper.map(receiptService.addReceiptAttachment(receipt, ReceiptBlobType.valueOf(blobType), encryptedPayload))
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Receipt modification failed")
		}
	}

	@Operation(summary = "Gets a receipt")
	@GetMapping("/{receiptId}")
	fun getReceipt(
		@PathVariable receiptId: String,
	) = mono {
		receiptService.getReceipt(receiptId)?.let { receiptMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found")
	}

	@Operation(summary = "Gets a receipt by reference")
	@GetMapping("/byref/{ref}")
	fun listByReference(
		@PathVariable ref: String,
	): Flux<ReceiptDto> = receiptService.listReceiptsByReference(ref).map { receiptMapper.map(it) }.injectReactorContext()

	@Operation(summary = "Updates a receipt")
	@PutMapping
	fun modifyReceipt(
		@RequestBody receiptDto: ReceiptDto,
	) = mono {
		receiptService
			.modifyReceipt(receiptMapper.map(receiptDto))
			.let { receiptMapper.map(it) }
	}

	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}
}
