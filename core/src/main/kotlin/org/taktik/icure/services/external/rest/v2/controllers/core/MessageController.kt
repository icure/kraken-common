/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Splitter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asyncservice.MessageService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.MessagesReadStatusUpdate
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.MessageV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.utils.orThrow
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.MessageBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.injectCachedReactorContext
import reactor.core.publisher.Flux

@RestController("messageControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/message")
@Tag(name = "message")
class MessageController(
	private val messageService: MessageService,
	private val sessionLogic: SessionInformationProvider,
	private val messageV2Mapper: MessageV2Mapper,
	private val objectMapper: ObjectMapper,
	private val bulkShareResultV2Mapper: MessageBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val filters: Filters,
	private val reactorCacheInjector: ReactorCacheInjector
) {
	companion object {
		private const val DEFAULT_LIMIT = 1000
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Creates a message")
	@PostMapping
	fun createMessage(@RequestBody messageDto: MessageDto) = mono {
		messageService.createMessage(messageV2Mapper.map(messageDto))?.let { messageV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Message creation failed")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Deletes multiple messages")
	@PostMapping("/delete/batch")
	fun deleteMessages(@RequestBody messageIds: ListOfIdsDto): Flux<DocIdentifier> =
		messageService.deleteMessages(messageIds.ids).injectReactorContext()

	@Operation(summary = "Deletes a message")
	@DeleteMapping("/{messageId}")
	fun deleteMessage(@PathVariable messageId: String) = mono {
		messageService.deleteMessage(messageId)
	}

	@Operation(summary = "Gets a message")
	@GetMapping("/{messageId}")
	fun getMessage(@PathVariable messageId: String) = mono {
		messageService.getMessage(messageId)?.let { messageV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Get all messages for current HC Party and provided transportGuids")
	@PostMapping("/byTransportGuid/list")
	fun listMessagesByTransportGuids(@RequestParam("hcpId") hcpId: String, @RequestBody transportGuids: ListOfIdsDto) =
		messageService.getMessagesByTransportGuids(hcpId, transportGuids.ids.toSet()).map { messageV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "List messages found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by coma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findMessagesByHCPartyPatientForeignKeys(@RequestParam secretFKeys: String): Flux<MessageDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return messageService.listMessagesByCurrentHCPartySecretPatientKeys(secretPatientKeys)
			.map { contact -> messageV2Mapper.map(contact) }
			.injectReactorContext()
	}

	@Operation(summary = "List messages found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findMessagesByHCPartyPatientForeignKeys(@RequestBody secretPatientKeys: List<String>): Flux<MessageDto> {
		return messageService.listMessagesByCurrentHCPartySecretPatientKeys(secretPatientKeys)
			.map { contact -> messageV2Mapper.map(contact) }
			.injectReactorContext()
	}

	@Operation(summary = "Get all messages (paginated) for current HC Party")
	@GetMapping
	fun findMessages(
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.takeIf { it.isNotEmpty() }?.let {
			objectMapper.readValue<ComplexKey>(
				startKey)
		}
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)

		messageService.findForCurrentHcPartySortedByReceived(paginationOffset).paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Get children messages of provided message")
	@GetMapping("/{messageId}/children")
	fun getChildrenMessages(@PathVariable messageId: String) =
		messageService.getMessageChildren(messageId).map { messageV2Mapper.map(it) }.injectReactorContext()

	@OptIn(FlowPreview::class)
	@Operation(summary = "Get children messages of provided message")
	@PostMapping("/children/batch")
	fun getMessagesChildren(@RequestBody parentIds: ListOfIdsDto) =
		messageService.getMessagesChildren(parentIds.ids)
			.map { m -> m.map { mm -> messageV2Mapper.map(mm) }.asFlow() }
			.flattenConcat()
			.injectReactorContext()

	@Operation(summary = "Get children messages of provided message")
	@PostMapping("/byInvoice")
	fun listMessagesByInvoices(@RequestBody ids: ListOfIdsDto) =
		messageService.listMessagesByInvoiceIds(ids.ids).map { messageV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided transportGuid")
	@GetMapping("/byTransportGuid")
	fun findMessagesByTransportGuid(
		@RequestParam(required = false) transportGuid: String?,
		@RequestParam(required = false) received: Boolean?,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		if (received == true) {
			val startKeyElements = startKey?.let { startKeyString ->
				startKeyString.takeIf { it.startsWith("[") }?.let { startKeyArray ->
					objectMapper.readValue<ComplexKey>(
						startKeyArray
					)
				} ?: Splitter.on(",").omitEmptyStrings().trimResults().splitToList(startKeyString) // Pretty sure this was not working well before, but leaving it as is for now
					.map { it.takeUnless { it == "null" } }.let { ComplexKey(it.toTypedArray()) }
			}
			val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
			messageService.findMessagesByTransportGuidReceived(hcpIdOrCurrentHcpId, transportGuid, paginationOffset)
		} else {
			val startKeyElements = startKey?.let { startKeyString ->
				startKeyString.takeIf { it.startsWith("[") }?.let { startKeyArray ->
					objectMapper.readValue<List<String?>>(
						startKeyArray
					)
				} ?: Splitter.on(",").omitEmptyStrings().trimResults().splitToList(startKeyString) // Pretty sure this was not working well before, but leaving it as is for now
					.map { it.takeUnless { it == "null" } }
			}
			val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
			messageService.findMessagesByTransportGuid(hcpIdOrCurrentHcpId, transportGuid, paginationOffset)
		}.paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Get all messages starting by a prefix between two date")
	@GetMapping("/byTransportGuidSentDate")
	fun findMessagesByTransportGuidSentDate(
		@RequestParam(required = false) transportGuid: String,
		@RequestParam(required = false, value = "from") fromDate: Long,
		@RequestParam(required = false, value = "to") toDate: Long,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyList = startKey?.takeIf { it.isNotEmpty() }
			?.let {
				if (it.startsWith("["))
					objectMapper.readValue<ComplexKey>(it)
				else
				// Pretty sure this was not working well before, but leaving it as is for now
					ComplexKey(Splitter.on(",").omitEmptyStrings().trimResults().splitToList(it).toTypedArray())
			}
			val paginationOffset = PaginationOffset(startKeyList, startDocumentId, null, realLimit + 1)
		messageService.findMessagesByTransportGuidSentDate(
			hcpId ?: sessionLogic.getCurrentHealthcarePartyId(),
			transportGuid,
			fromDate,
			toDate,
			paginationOffset
		).paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided to address")
	@GetMapping("/byToAddress")
	fun findMessagesByToAddress(
		@RequestParam(required = false) toAddress: String,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) reverse: Boolean?,
		@RequestParam(required = false) hcpId: String?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.takeIf { it.isNotEmpty() }
			?.let { objectMapper.readValue<ComplexKey>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		messageService.findMessagesByToAddress(hcpIdOrCurrentHcpId, toAddress, paginationOffset, reverse ?: false)
			.paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided from address")
	@GetMapping("/byFromAddress")
	fun findMessagesByFromAddress(
		@RequestParam(required = false) fromAddress: String,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.takeIf { it.isNotEmpty() }?.let {
			objectMapper.readValue<ComplexKey>(
				startKey
			)
		}
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		messageService.findMessagesByFromAddress(hcpIdOrCurrentHcpId, fromAddress, paginationOffset)
			.paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Updates a message")
	@PutMapping
	fun modifyMessage(@RequestBody messageDto: MessageDto) = mono {
		messageService.modifyMessage(messageV2Mapper.map(messageDto))?.let { messageV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "New delegation for message failed")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Set status bits for given list of messages")
	@PutMapping("/status/{status}")
	fun setMessagesStatusBits(
		@PathVariable status: Int,
		@RequestBody messageIds: ListOfIdsDto
	) = messageService.setStatus(messageIds.ids, status).map { messageV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "Set read status for given list of messages")
	@PutMapping("/readstatus")
	fun setMessagesReadStatus(@RequestBody data: MessagesReadStatusUpdate) = flow {
		emitAll(
			messageService.setReadStatus(
				data.ids ?: listOf(),
				data.userId ?: sessionLogic.getCurrentUserId(),
				data.status
					?: false,
				data.time ?: System.currentTimeMillis()
			).map { messageV2Mapper.map(it) }
		)
	}.injectReactorContext()

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<MessageDto>> = flow {
		emitAll(messageService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(
		summary = "List messages for the current user (HcParty) or the given hcparty in the filter ",
		description = "Returns a list of messages along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page."
	)
	@PostMapping("/filter")
	fun filterMessagesBy(
		@Parameter(description = "A Message document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<MessageDto>
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val messages = messageService.filterMessages(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		messages.paginatedList(messageV2Mapper::map, realLimit)
	}

	@Operation(summary = "Get ids of messages matching the provided filter")
	@PostMapping("/match")
	fun matchMessagesBy(@RequestBody filter: AbstractFilterDto<MessageDto>) = mono {
		filters.resolve(filterV2Mapper.tryMap(filter).orThrow()).toList()
	}
}
