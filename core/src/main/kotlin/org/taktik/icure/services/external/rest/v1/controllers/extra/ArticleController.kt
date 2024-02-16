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
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.ArticleService
import org.taktik.icure.services.external.rest.v1.dto.ArticleDto
import org.taktik.icure.services.external.rest.v1.mapper.ArticleMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/article")
@Tag(name = "article")
class ArticleController(
	private val articleService: ArticleService,
	private val articleMapper: ArticleMapper
) {

	@Operation(summary = "Creates a article")
	@PostMapping
	fun createArticle(@RequestBody articleDto: ArticleDto) = mono {
		val article = articleService.createArticle(articleMapper.map(articleDto))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Article creation failed")

		articleMapper.map(article)
	}

	@Operation(summary = "Deletes an article")
	@DeleteMapping("/{articleIds}")
	fun deleteArticle(@PathVariable articleIds: String): Flux<DocIdentifier> {
		return articleService.deleteArticles(articleIds.split(',')).injectReactorContext()
	}

	@Operation(summary = "Gets an article")
	@GetMapping("/{articleId}")
	fun getArticle(@PathVariable articleId: String) = mono {
		val article = articleService.getArticle(articleId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Article fetching failed")

		articleMapper.map(article)
	}

	@Operation(summary = "Modifies an article")
	@PutMapping
	fun modifyArticle(@RequestBody articleDto: ArticleDto) = mono {
		val article = articleService.modifyArticle(articleMapper.map(articleDto))
			?: throw DocumentNotFoundException("AccessLog modification failed")
		articleMapper.map(article)
	}

	@Operation(summary = "Gets all articles")
	@GetMapping
	fun getArticles(): Flux<ArticleDto> =
		articleService.getAllArticles().map { a -> articleMapper.map(a) }.injectReactorContext()

}