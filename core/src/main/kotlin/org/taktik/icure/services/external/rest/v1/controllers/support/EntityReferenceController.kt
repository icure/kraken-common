/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asyncservice.EntityReferenceService
import org.taktik.icure.services.external.rest.v1.dto.EntityReferenceDto
import org.taktik.icure.services.external.rest.v1.mapper.EntityReferenceMapper

@RestController
@Profile("app")
@RequestMapping("/rest/v1/entityref")
@Tag(name = "entityref")
class EntityReferenceController(
	private val entityReferenceService: EntityReferenceService,
	private val entityReferenceMapper: EntityReferenceMapper,
) {
	@Operation(summary = "Find latest reference for a prefix ")
	@GetMapping("/latest/{prefix}")
	fun getLatest(
		@PathVariable prefix: String,
	) = mono {
		entityReferenceService.getLatest(prefix)?.let { entityReferenceMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to fetch Entity Reference")
	}

	@Operation(summary = "Create an entity reference")
	@PostMapping
	fun createEntityReference(
		@RequestBody er: EntityReferenceDto,
	) = mono {
		val created =
			try {
				entityReferenceService.createEntityReferences(listOf(entityReferenceMapper.map(er)))
			} catch (e: Exception) {
				throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Entity reference failed.")
			}
		created.firstOrNull()?.let { entityReferenceMapper.map(it) }
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Entity reference creation failed.")
	}
}
