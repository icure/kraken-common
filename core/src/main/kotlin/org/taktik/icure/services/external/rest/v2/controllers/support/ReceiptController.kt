/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.ReceiptService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.entities.embed.ReceiptBlobType
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.ReceiptDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.ReceiptV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ReceiptBulkShareResultV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.writeTo
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream

@RestController("receiptControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/receipt")
@Tag(name = "receipt")
class ReceiptController(
	private val receiptService: ReceiptService,
	private val receiptV2Mapper: ReceiptV2Mapper,
	private val bulkShareResultV2Mapper: ReceiptBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Creates a receipt")
	@PostMapping
	fun createReceipt(
		@RequestBody receiptDto: ReceiptDto,
	): Mono<ReceiptDto> = mono {
		receiptV2Mapper.map(receiptService.createReceipt(receiptV2Mapper.map(receiptDto)))
	}

	@Operation(summary = "Deletes multiple Receipts")
	@PostMapping("/delete/batch")
	fun deleteReceipts(
		@RequestBody receiptIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = receiptService
		.deleteReceipts(
			receiptIds.ids.map { IdAndRev(it, null) },
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes a multiple Receipts if they match the provided revs")
	@PostMapping("/delete/batch/withrev")
	fun deleteReceiptsWithRev(
		@RequestBody receiptIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = receiptService
		.deleteReceipts(
			receiptIds.ids.map(idWithRevV2Mapper::map),
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
		.injectReactorContext()

	@Operation(summary = "Deletes an Receipt")
	@DeleteMapping("/{receiptId}")
	fun deleteReceipt(
		@PathVariable receiptId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = mono {
		receiptService.deleteReceipt(receiptId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{receiptId}")
	fun undeleteReceipt(
		@PathVariable receiptId: String,
		@RequestParam(required = true) rev: String,
	): Mono<ReceiptDto> = mono {
		receiptV2Mapper.map(receiptService.undeleteReceipt(receiptId, rev))
	}

	@DeleteMapping("/purge/{receiptId}")
	fun purgeReceipt(
		@PathVariable receiptId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		receiptService.purgeReceipt(receiptId, rev).let(docIdentifierV2Mapper::map)
	}

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
	): Mono<ByteArray> = mono {
		val attachment =
			ByteArrayOutputStream().use {
				receiptService.getAttachment(receiptId, attachmentId).writeTo(it)
				it.toByteArray()
			}
		if (attachment.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found")
				.also { logger.error(it.message) }
		}
		attachment
	}

	@Operation(summary = "Creates a receipt's attachment")
	@PutMapping("/{receiptId}/attachment/{blobType}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setReceiptAttachment(
		@PathVariable receiptId: String,
		@PathVariable blobType: String,
		@Parameter(
			description = "Revision of the latest known version of the receipt. If it doesn't match the current revision the method will fail with CONFLICT.",
		)
		@RequestParam(required = true)
		rev: String,
		@Schema(type = "string", format = "binary") @RequestBody payload: ByteArray,
	): Mono<ReceiptDto> = mono {
		val receipt = receiptService.getReceipt(receiptId)
		if (receipt != null) {
			if (receipt.rev != rev) throw ResponseStatusException(HttpStatus.CONFLICT, "Current receipt revision does not match provided revision.")
			receiptV2Mapper.map(receiptService.addReceiptAttachment(receipt, ReceiptBlobType.valueOf(blobType), payload))
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Receipt modification failed")
		}
	}

	@Operation(summary = "Gets a receipt")
	@GetMapping("/{receiptId}")
	fun getReceipt(
		@PathVariable receiptId: String,
	): Mono<ReceiptDto> = mono {
		receiptService.getReceipt(receiptId)?.let { receiptV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found")
	}

	@Operation(summary = "Gets a receipt")
	@GetMapping("/byRef/{ref}")
	fun listByReference(
		@PathVariable ref: String,
	): Flux<ReceiptDto> = receiptService.listReceiptsByReference(ref).map { receiptV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "Updates a receipt")
	@PutMapping
	fun modifyReceipt(
		@RequestBody receiptDto: ReceiptDto,
	): Mono<ReceiptDto> = mono {
		receiptService
			.modifyReceipt(receiptV2Mapper.map(receiptDto))
			.let { receiptV2Mapper.map(it) }
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<ReceiptDto>> = flow {
		emitAll(
			receiptService
				.bulkShareOrUpdateMetadata(
					entityShareOrMetadataUpdateRequestV2Mapper.map(request),
				).map { bulkShareResultV2Mapper.map(it) },
		)
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
