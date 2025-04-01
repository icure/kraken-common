/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier

import org.taktik.icure.asyncservice.KeywordService
import org.taktik.icure.services.external.rest.v1.dto.KeywordDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.mapper.KeywordMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/keyword")
@Tag(name = "keyword")
class KeywordController(
	private val keywordService: KeywordService,
	private val keywordMapper: KeywordMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
) {

	@Operation(summary = "Create a keyword with the current user", description = "Returns an instance of created keyword.")
	@PostMapping
	fun createKeyword(@RequestBody c: KeywordDto) = mono {
		keywordService.createKeyword(keywordMapper.map(c))?.let { keywordMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keyword creation failed.")
	}

	@Operation(summary = "Get a keyword")
	@GetMapping("/{keywordId}")
	fun getKeyword(@PathVariable keywordId: String) = mono {
		keywordService.getKeyword(keywordId)?.let { keywordMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting keyword failed. Possible reasons: no such keyword exists, or server error. Please try again or read the server log.")
	}

	@Operation(summary = "Get keywords by user")
	@GetMapping("/byUser/{userId}")
	fun getKeywordsByUser(@PathVariable userId: String) =
		keywordService.getKeywordsByUser(userId).let { it.map { c -> keywordMapper.map(c) } }.injectReactorContext()

	@Operation(summary = "Gets all keywords with pagination")
	@GetMapping
	fun getKeywords() = keywordService
		.getAllKeywords()
		.map(keywordMapper::map)
		.injectReactorContext()

	@Operation(summary = "Delete keywords.", description = "Response is a set containing the ID's of deleted keywords.")
	@DeleteMapping("/{keywordIds}")
	fun deleteKeywords(@PathVariable keywordIds: String): Flux<DocIdentifierDto> {
		val ids = keywordIds.split(',')
		if (ids.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		return keywordService.deleteKeywords(ids.toSet())
			.map { docIdentifierMapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}

	@Operation(summary = "Modify a keyword", description = "Returns the modified keyword.")
	@PutMapping
	fun modifyKeyword(@RequestBody keywordDto: KeywordDto) = mono {
		keywordService.modifyKeyword(keywordMapper.map(keywordDto))?.let {
			keywordMapper.map(it)
		} ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Keyword modification failed.")
	}
}