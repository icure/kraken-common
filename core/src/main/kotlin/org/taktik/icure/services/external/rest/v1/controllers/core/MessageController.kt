/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
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
import org.taktik.icure.asyncservice.MessageService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.MessageDto
import org.taktik.icure.services.external.rest.v1.dto.MessagesReadStatusUpdate
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.mapper.MessageMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/message")
@Tag(name = "message")
class MessageController(
	private val messageService: MessageService,
	private val sessionLogic: SessionInformationProvider,
	private val messageMapper: MessageMapper,
	private val delegationMapper: DelegationMapper,
	private val stubMapper: StubMapper,
	private val objectMapper: ObjectMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig
) {
	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Creates a message")
	@PostMapping
	fun createMessage(@RequestBody messageDto: MessageDto) = mono {
		messageService.createMessage(messageMapper.map(messageDto))?.let { messageMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Message creation failed").also { logger.error (it.message) }
	}

	@Operation(summary = "Deletes a message delegation")
	@DeleteMapping("/{messageId}/delegate/{delegateId}")
	fun deleteDelegation(
		@PathVariable messageId: String,
		@PathVariable delegateId: String
	) = mono {
		val message = messageService.getMessage(messageId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message with ID: $messageId not found").also { logger.error(it.message) }

		messageService.modifyMessage(message.copy(delegations = message.delegations - delegateId))
			?.let(messageMapper::map)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Message delegation deletion failed").also { logger.error(it.message) }
	}

	@Operation(summary = "Deletes multiple messages")
	@DeleteMapping("/{messageIds}")
	fun deleteMessages(@PathVariable messageIds: String): Flux<DocIdentifierDto> =
		messageIds.split(',').takeIf { it.isNotEmpty() }
			?.let { messageService.deleteMessages(it).injectReactorContext() }
			?.map(docIdentifierMapper::map)
			?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong id format")

	@Operation(summary = "Deletes multiple messages")
	@PostMapping("/delete/byIds")
	fun deleteMessagesBatch(@RequestBody messagesIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		messageService.deleteMessages(messagesIds.ids)
			.map(docIdentifierMapper::map)
			.injectReactorContext()

	@Operation(summary = "Gets a message")
	@GetMapping("/{messageId}")
	fun getMessage(@PathVariable messageId: String) = mono {
		messageService.getMessage(messageId)?.let { messageMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Get all messages for current HC Party and provided transportGuids")
	@PostMapping("/byTransportGuid/list")
	fun listMessagesByTransportGuids(@RequestParam("hcpId") hcpId: String, @RequestBody transportGuids: ListOfIdsDto) =
		messageService.getMessagesByTransportGuids(hcpId, transportGuids.ids.toSet()).map { messageMapper.map(it) }.injectReactorContext()

	@Operation(summary = "List messages found By Healthcare Party and secret foreign keys.", description = "Keys must be delimited by comma")
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findMessagesByHCPartyPatientForeignKeys(@RequestParam secretFKeys: String): Flux<MessageDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		return messageService.listMessagesByCurrentHCPartySecretPatientKeys(secretPatientKeys)
			.map { contact -> messageMapper.map(contact) }
			.injectReactorContext()
	}

	@Operation(summary = "List messages found By Healthcare Party and secret foreign keys.")
	@PostMapping("/byHcPartySecretForeignKeys")
	fun findMessagesByHCPartyPatientForeignKeys(@RequestBody secretPatientKeys: List<String>): Flux<MessageDto> {
		return messageService.listMessagesByCurrentHCPartySecretPatientKeys(secretPatientKeys)
			.map { contact -> messageMapper.map(contact) }
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
			.mapElements(messageMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get children messages of provided message")
	@GetMapping("/{messageId}/children")
	fun getChildrenMessages(@PathVariable messageId: String) =
		messageService.getMessageChildren(messageId).map { messageMapper.map(it) }.injectReactorContext()

	@Operation(summary = "Get children messages of provided message")
	@PostMapping("/children/batch")
	fun getChildrenMessagesOfList(@RequestBody parentIds: ListOfIdsDto) =
		messageService.getMessagesChildren(parentIds.ids)
			.map(messageMapper::map)
			.injectReactorContext()

	@Operation(summary = "Get children messages of provided message")
	@PostMapping("byInvoiceId")
	fun listMessagesByInvoiceIds(@RequestBody ids: ListOfIdsDto) =
		messageService.listMessagesByInvoiceIds(ids.ids).map { messageMapper.map(it) }.injectReactorContext()

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
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		if (received == true) {
			emitAll(messageService.findMessagesByTransportGuidReceived(hcpIdOrCurrentHcpId, transportGuid, paginationOffset))
		} else {
			emitAll(messageService.findMessagesByTransportGuid(hcpIdOrCurrentHcpId, transportGuid, paginationOffset))
		}
	}.mapElements(messageMapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages starting by a prefix between two date")
	@GetMapping("/byTransportGuidSentDate")
	fun findMessagesByTransportGuidSentDate(
		@RequestParam(required = true) transportGuid: String,
		@RequestParam(required = true) from: Long,
		@RequestParam(required = true) to: Long,
		@RequestParam(required = false) startKey: JsonString?,
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
	}.mapElements(messageMapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided to address")
	@GetMapping("/byToAddress")
	fun findMessagesByToAddress(
		@RequestParam(required = false) toAddress: String,
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
	}.mapElements(messageMapper::map).asPaginatedFlux()

	@Operation(summary = "Get all messages (paginated) for current HC Party and provided from address")
	@GetMapping("/byFromAddress")
	fun findMessagesByFromAddress(
		@RequestParam(required = false) fromAddress: String,
		@RequestParam(required = false) startKey: JsonString?,
		@RequestParam(required = false) startDocumentId: String?,
		@RequestParam(required = false) limit: Int?,
		@RequestParam(required = false) hcpId: String?
	): PaginatedFlux<MessageDto> = flow {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		val hcpIdOrCurrentHcpId = hcpId ?: sessionLogic.getCurrentHealthcarePartyId()
		emitAll(messageService.findMessagesByFromAddress(hcpIdOrCurrentHcpId, fromAddress, paginationOffset))
	}.mapElements(messageMapper::map).asPaginatedFlux()

	@Operation(summary = "Updates a message")
	@PutMapping
	fun modifyMessage(@RequestBody messageDto: MessageDto) = mono {
		messageService.modifyMessage(messageMapper.map(messageDto))?.let { messageMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "New delegation for message failed")
				.also { logger.error(it.message) }
	}

	@Operation(summary = "Set status bits for given list of messages")
	@PutMapping("/status/{status}")
	fun setMessagesStatusBits(
		@PathVariable status: Int,
		@RequestBody messageIds: ListOfIdsDto
	) = messageService.setStatus(messageIds.ids, status).map { messageMapper.map(it) }.injectReactorContext()

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
				).map { messageMapper.map(it) }
			)
		}
	}.injectReactorContext()

	@Operation(summary = "Adds a delegation to a message")
	@PutMapping("/{messageId}/delegate")
	fun newMessageDelegations(
		@PathVariable messageId: String,
		@RequestBody ds: List<DelegationDto>
	) = mono {
		messageService.addDelegations(messageId, ds.map { delegationMapper.map(it) })?.takeIf { it.delegations.isNotEmpty() }?.let { stubMapper.mapToStub(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "New delegation for message failed")
	}
}
