/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asyncservice.MessageService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.MessagesReadStatusUpdate
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.MessageV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.MessageBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
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
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val filters: Filters,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig
) {
	companion object {
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
	fun deleteMessages(@RequestBody messageIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		messageService.deleteMessages(messageIds.ids)
			.map(docIdentifierV2Mapper::map)
			.injectReactorContext()

	@Operation(summary = "Deletes a message")
	@DeleteMapping("/{messageId}")
	fun deleteMessage(@PathVariable messageId: String) = mono {
		messageService.deleteMessage(messageId).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets a message")
	@GetMapping("/{messageId}")
	fun getMessage(@PathVariable messageId: String) = mono {
		messageService.getMessage(messageId)?.let { messageV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Gets multiple messages by their ids")
	@PostMapping("/byIds")
	fun getMessages(@RequestBody messageIds: ListOfIdsDto): Flux<MessageDto> {
		require(messageIds.ids.isNotEmpty()) { "You must specify at least one id." }
		return messageService.getMessages(messageIds.ids).map(messageV2Mapper::map).injectReactorContext()
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

	@Operation(summary = "Find Messages ids by data owner id, patient secret keys and sent date")
	@PostMapping("/byDataOwnerPatientSentDate", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun listMessageIdsByDataOwnerPatientSentDate(
		@RequestParam dataOwnerId: String,
		@RequestParam(required = false) startDate: Long?,
		@RequestParam(required = false) endDate: Long?,
		@RequestParam(required = false) descending: Boolean?,
		@RequestBody secretPatientKeys: ListOfIdsDto
	): Flux<String> {
		require(secretPatientKeys.ids.isNotEmpty()) {
			"You need to provide at least one secret patient key"
		}
		return messageService
			.listMessageIdsByDataOwnerPatientSentDate(
				dataOwnerId = dataOwnerId,
				secretForeignKeys = secretPatientKeys.ids.toSet(),
				startDate = startDate?.let{ FuzzyValues.getFuzzyDateTime(it) },
				endDate = endDate?.let { FuzzyValues.getFuzzyDateTime(it) },
				descending = descending ?: false)
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
		@RequestParam(required = false) startKey: JsonString?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?
	): PaginatedFlux<MessageDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return messageService
			.findForCurrentHcPartySortedByReceived(paginationOffset)
			.mapElements(messageV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get children messages of provided message")
	@GetMapping("/{messageId}/children")
	fun getChildrenMessages(@PathVariable messageId: String) =
		messageService.getMessageChildren(messageId).map { messageV2Mapper.map(it) }.injectReactorContext()

	@OptIn(ExperimentalCoroutinesApi::class)
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
		@RequestParam(required = false) startKey: JsonString?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	): PaginatedFlux<MessageDto> = flow {
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		val startKeyElements = startKey?.let { startKeyArray -> objectMapper.readValue<ComplexKey>(startKeyArray) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		if (received == true) {
			emitAll(messageService.findMessagesByTransportGuidReceived(hcpIdOrCurrentHcpId, transportGuid, paginationOffset))
		} else {
			emitAll(messageService.findMessagesByTransportGuid(hcpIdOrCurrentHcpId, transportGuid, paginationOffset))
		}
	}.mapElements(messageV2Mapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages starting by a prefix between two date")
	@GetMapping("/byTransportGuidSentDate")
	fun findMessagesByTransportGuidSentDate(
		@RequestParam(required = true) transportGuid: String,
		@RequestParam(required = true) from: Long,
		@RequestParam(required = true) to: Long,
		@RequestParam(required = false) startKey: String?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	): PaginatedFlux<MessageDto> = flow {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		emitAll(messageService.findMessagesByTransportGuidSentDate(
			hcpId ?: sessionLogic.getCurrentHealthcarePartyId(),
			transportGuid,
			from,
			to,
			paginationOffset
		))
	}.mapElements(messageV2Mapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided to address")
	@GetMapping("/byToAddress")
	fun findMessagesByToAddress(
		@RequestParam(required = true) toAddress: String,
		@RequestParam(required = false) startKey: JsonString?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) reverse: Boolean?,
		@RequestParam(required = false) hcpId: String?
	): PaginatedFlux<MessageDto> = flow {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		emitAll(messageService.findMessagesByToAddress(hcpIdOrCurrentHcpId, toAddress, paginationOffset, reverse ?: false))
	}.mapElements(messageV2Mapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided from address")
	@GetMapping("/byFromAddress")
	fun findMessagesByFromAddress(
		@RequestParam(required = true) fromAddress: String,
		@RequestParam(required = false) startKey: JsonString?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	): PaginatedFlux<MessageDto> = flow {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		emitAll(messageService.findMessagesByFromAddress(hcpIdOrCurrentHcpId, fromAddress, paginationOffset))
	}.mapElements(messageV2Mapper::map).asPaginatedFlux()

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
		data.ids?.takeIf { it.isNotEmpty() }?.let {
			emitAll(
				messageService.setReadStatus(
					it,
					data.userId,
					data.status ?: false,
					data.time
				).map { messageV2Mapper.map(it) }
			)
		}
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
		val realLimit = limit ?: paginationConfig.defaultLimit

		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val messages = messageService.filterMessages(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		messages.paginatedList(messageV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(summary = "Get ids of messages matching the provided filter")
	@PostMapping("/match")
	fun matchMessagesBy(@RequestBody filter: AbstractFilterDto<MessageDto>) = mono {
		filters.resolve(filterV2Mapper.tryMap(filter).orThrow()).toList()
	}
}
