/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.ArticleService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ArticleDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ArticleV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.ArticleBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController("articleControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/article")
@Tag(name = "article")
class ArticleController(
	private val articleService: ArticleService,
	private val articleV2Mapper: ArticleV2Mapper,
	private val bulkShareResultV2Mapper: ArticleBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(summary = "Creates a article")
	@PostMapping
	fun createArticle(@RequestBody articleDto: ArticleDto) = mono {
		val article = articleService.createArticle(articleV2Mapper.map(articleDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Article creation failed")

		articleV2Mapper.map(article)
	}

	@Operation(summary = "Deletes a batch of articles")
	@PostMapping("/delete/batch")
	fun deleteArticles(@RequestBody articleIds: ListOfIdsDto): Flux<DocIdentifierDto> =
		articleIds.ids.takeIf { it.isNotEmpty() }?.let { ids ->
			articleService.deleteArticles(ids)
				.map(docIdentifierV2Mapper::map)
				.injectReactorContext()
		} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also { logger.error(it.message) }

	@Operation(summary = "Deletes an article")
	@DeleteMapping("/{articleId}")
	fun deleteArticle(@PathVariable articleId: String) = mono {
		articleService.deleteArticle(articleId).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Gets an article")
	@GetMapping("/{articleId}")
	fun getArticle(@PathVariable articleId: String) = mono {
		val article = articleService.getArticle(articleId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Article fetching failed")

		articleV2Mapper.map(article)
	}

	@Operation(summary = "Modifies an article")
	@PutMapping
	fun modifyArticle(@RequestBody articleDto: ArticleDto) = mono {
		val article = articleService.modifyArticle(articleV2Mapper.map(articleDto))
			?: throw DocumentNotFoundException("Article modification failed")
		articleV2Mapper.map(article)
	}

	@Operation(summary = "Gets all articles with pagination")
	@GetMapping
	fun getArticles(
		@Parameter(description = "A Keyword document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<ArticleDto> {
		val offset = PaginationOffset(null, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return articleService
			.getAllArticles(offset)
			.mapElements(articleV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<ArticleDto>> = flow {
		emitAll(articleService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)
}
