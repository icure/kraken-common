/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
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
import org.taktik.icure.asyncservice.KeywordService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.KeywordDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.KeywordV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("keywordControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/keyword")
@Tag(name = "keyword")
class KeywordController(
	private val keywordService: KeywordService,
	private val keywordV2Mapper: KeywordV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Create a keyword with the current user", description = "Returns an instance of created keyword.")
	@PostMapping
	fun createKeyword(
		@RequestBody c: KeywordDto,
	) = mono {
		keywordService.createKeyword(keywordV2Mapper.map(c))?.let { keywordV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keyword creation failed.")
	}

	@Operation(summary = "Get a keyword")
	@GetMapping("/{keywordId}")
	fun getKeyword(
		@PathVariable keywordId: String,
	) = mono {
		keywordService.getKeyword(keywordId)?.let { keywordV2Mapper.map(it) }
			?: throw ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Getting keyword failed. Possible reasons: no such keyword exists, or server error. Please try again or read the server log.",
			)
	}

	@Operation(summary = "Get keywords by user")
	@GetMapping("/byUser/{userId}")
	fun getKeywordsByUser(
		@PathVariable userId: String,
	) = keywordService.getKeywordsByUser(userId).let { it.map { c -> keywordV2Mapper.map(c) } }.injectReactorContext()

	@Operation(summary = "Gets all keywords with pagination")
	@GetMapping
	fun getKeywords(
		@Parameter(description = "A Keyword document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<KeywordDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return keywordService.getAllKeywords(offset).mapElements(keywordV2Mapper::map).asPaginatedFlux()
	}

	@Operation(summary = "Delete keywords.", description = "Response is a set containing the ID's of deleted keywords.")
	@PostMapping("/delete/batch")
	fun deleteKeywords(
		@RequestBody keywordIds: ListOfIdsDto,
	): Flux<DocIdentifierDto> = keywordIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
		keywordService
			.deleteKeywords(ids.toSet())
			.map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(summary = "Modify a keyword", description = "Returns the modified keyword.")
	@PutMapping
	fun modifyKeyword(
		@RequestBody keywordDto: KeywordDto,
	) = mono {
		keywordService.modifyKeyword(keywordV2Mapper.map(keywordDto))?.let { keywordV2Mapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Keyword modification failed.")
	}
}
