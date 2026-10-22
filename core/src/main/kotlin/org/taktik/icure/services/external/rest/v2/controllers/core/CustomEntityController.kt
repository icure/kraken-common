package org.taktik.icure.services.external.rest.v2.controllers.core

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.util.CachedCustomEntitiesConfigurationProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.asyncservice.CustomEntityService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.objectstorage.ObjectStorageException
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.dto.requests.customentity.CustomEntityBulkAttachmentUpdateOptions
import org.taktik.icure.services.external.rest.v2.dto.requests.document.BulkAttachmentUpdateOptions
import org.taktik.icure.services.external.rest.v2.mapper.CustomEntityBaseV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.StubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.CustomEntityBaseBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.CustomEntityControllerHelper
import org.taktik.icure.utils.enforceSize
import org.taktik.icure.utils.injectCachedReactorContext
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("customEntityControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/custom")
@Tag(name = "custom")
class CustomEntityController(
	val mapper: CustomEntityBaseV2Mapper,
	private val customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
	private val controllerHelper: CustomEntityControllerHelper,
	private val bulkShareResultV2Mapper: CustomEntityBaseBulkShareResultV2Mapper,
	private val customEntityService: CustomEntityService,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val stubV2Mapper: StubV2Mapper,
) {
	private suspend inline fun <T> withCustomConfig(
		entityType: String,
		block: (config: VersionedCustomEntitiesConfiguration, customEntityDefinition: CustomEntityDefinition) -> T,
	): T {
		val (config, customEntityConfig) = controllerHelper.requireDefinition(customEntitiesConfigurationProvider.getConfigForCurrentUser(), entityType)
		return block(config, customEntityConfig)
	}

	private suspend inline fun <T> withCustomDefinition(
		entityType: String,
		block: (customEntityDefinition: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { _, customEntityDefinition -> block(customEntityDefinition) }

	private inline fun <T> withCustomDefinitionFlow(
		entityType: String,
		crossinline block: (customEntityDefinition: CustomEntityDefinition) -> Flow<T>,
	): Flow<T> = flow {
		emitAll(withCustomConfig(entityType) { _, customEntityDefinition -> block(customEntityDefinition) })
	}

	private inline fun <T> withDelegationBasedDefinitionFlow(
		entityType: String,
		crossinline block: (customEntityDefinition: CustomEntityDefinition) -> Flow<T>,
	): Flow<T> = withCustomDefinitionFlow(entityType) { customEntityDefinition ->
		controllerHelper.requireDelegationBased(customEntityDefinition, entityType)
		block(customEntityDefinition)
	}

	private suspend inline fun <T> withAttachmentsEnabled(
		entityType: String,
		block: (customEntityDefinition: CustomEntityDefinition) -> T,
	): T = withCustomDefinition(entityType) { customEntityDefinition ->
		controllerHelper.requireAttachmentsEnabled(customEntityDefinition, entityType)
		block(customEntityDefinition)
	}

	private suspend inline fun <T> mappingCustom(
		entityType: String,
		dto: CustomEntityBaseDto,
		block: (mappedEntity: CustomEntityBase, customEntityConfig: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { config, customEntityConfig ->
		block(controllerHelper.mapFromDto(config, customEntityConfig, entityType, dto), customEntityConfig)
	}

	private suspend inline fun <T> mappingCustom(
		entityType: String,
		dtos: List<CustomEntityBaseDto>,
		block: (mappedEntities: List<CustomEntityBase>, customEntityConfig: CustomEntityDefinition) -> T,
	): T = withCustomConfig(entityType) { config, customEntityConfig ->
		block(controllerHelper.mapFromDtos(config, customEntityConfig, entityType, dtos), customEntityConfig)
	}

	private fun CustomEntityBase.toDto(entityType: String): CustomEntityBaseDto =
		mapper.map(this, entityType)

	private fun Flow<CustomEntityBase>.toDto(entityType: String): Flow<CustomEntityBaseDto> =
		map { mapper.map(it, entityType) }

	private fun Flow<EntityBulkShareResult<CustomEntityBase>>.toDtoUpdateResult(entityType: String): Flow<EntityBulkShareResultDto<CustomEntityBaseDto>> =
		map { bulkShareResultV2Mapper.map(it, entityType) }

	@PostMapping("/{entityType}")
	fun createCustomEntity(
		@PathVariable entityType: String,
		@RequestBody entityDto: CustomEntityBaseDto,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		mappingCustom(entityType, entityDto) { entity, _ ->
			customEntityService.createCustomEntity(entityType = entityType, entity = entity).toDto(entityType)
		}
	}

	@PostMapping("/{entityType}/batch")
	fun createCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityDtos: List<CustomEntityBaseDto>,
	): Flux<CustomEntityBaseDto> = flow {
		emitAll(
			mappingCustom(entityType, entityDtos) { entities, _ ->
				customEntityService.createCustomEntities(entityType = entityType, entities = entities).toDto(entityType)
			}
		)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@GetMapping("/{entityType}/{entityId}")
	fun getCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.getCustomEntity(entityType = entityType, id = entityId)?.toDto(entityType)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"$entityType $entityId not found.",
				)
		}
	}

	@PostMapping("/{entityType}/byIds")
	fun getCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsDto,
	): Flux<CustomEntityBaseDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.getCustomEntities(
			entityType,
			entityIds.ids
		).toDto(entityType)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(summary = "List custom entity stubs found by ids.")
	@PostMapping("/{entityType}/delegations")
	fun findCustomEntitiesDelegationsStubsByIds(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsDto,
	): Flux<IcureStubDto> = withDelegationBasedDefinitionFlow(entityType) {
		// Unlike builtin entities, the custom entity metadata stub already includes all the information of the IcureStub
		customEntityService.getCustomEntitiesMetadataStubs(
			entityType = entityType,
			ids = entityIds.ids
		).map(stubV2Mapper::mapToStub)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@PutMapping("/{entityType}")
	fun modifyCustomEntity(
		@PathVariable entityType: String,
		@RequestBody customEntityDto: CustomEntityBaseDto,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		mappingCustom(entityType, customEntityDto) { customEntity, _ ->
			customEntityService.modifyCustomEntity(entityType = entityType, entity = customEntity).toDto(entityType)
		}
	}

	@PutMapping("/{entityType}/batch")
	fun modifyCustomEntities(
		@PathVariable entityType: String,
		@RequestBody customEntityDtos: List<CustomEntityBaseDto>,
	): Flux<CustomEntityBaseDto> = flow {
		emitAll(
			mappingCustom(entityType, customEntityDtos) { entities, _ ->
				customEntityService.modifyCustomEntities(entityType = entityType, entities = entities).toDto(entityType)
			}
		)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/{entityType}/{entityId}")
	fun deleteCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.deleteCustomEntity(entityType = entityType, id = entityId, rev = rev).let {
				docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
			}
		}
	}

	@PostMapping("/{entityType}/delete/batch")
	fun deleteCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.deleteCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).map { docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev)) }
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@PostMapping("/{entityType}/undelete/{entityId}")
	fun undeleteCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = true) rev: String,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			customEntityService.undeleteCustomEntity(entityType = entityType, id = entityId, rev = rev).toDto(entityType)
		}
	}

	@PostMapping("/{entityType}/undelete/batch")
	fun undeleteCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<CustomEntityBaseDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.undeleteCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).toDto(entityType)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@DeleteMapping("/{entityType}/purge/{entityId}")
	fun purgeCustomEntity(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withCustomDefinition(entityType) {
			docIdentifierV2Mapper.map(customEntityService.purgeCustomEntity(entityType = entityType, id = entityId, rev = rev))
		}
	}

	@PostMapping("/{entityType}/purge/batch")
	fun purgeCustomEntities(
		@PathVariable entityType: String,
		@RequestBody entityIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = withCustomDefinitionFlow(entityType) {
		customEntityService.purgeCustomEntities(
			entityType = entityType,
			identifiers = entityIds.ids.map(idWithRevV2Mapper::map)
		).map(docIdentifierV2Mapper::map)
	}.injectCachedReactorContext(reactorCacheInjector, 100)

	@Operation(description = "Shares one or more custom entities with one or more data owners.")
	@PutMapping("/{entityType}/bulkSharedMetadataUpdate")
	fun bulkShare(
		@PathVariable entityType: String,
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<CustomEntityBaseDto>> = withDelegationBasedDefinitionFlow(entityType) {
		customEntityService.bulkShareOrUpdateMetadata(
			entityType = entityType,
			requests = entityShareOrMetadataUpdateRequestV2Mapper.map(request),
		).toDtoUpdateResult(entityType)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more custom entities with one or more data owners but does not return the updated entity.")
	@PutMapping("/{entityType}/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@PathVariable entityType: String,
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto,
	): Flux<EntityBulkShareResultDto<Nothing>> = withDelegationBasedDefinitionFlow(entityType) {
		customEntityService.bulkShareOrUpdateMetadata(
			entityType = entityType,
			requests = entityShareOrMetadataUpdateRequestV2Mapper.map(request),
		).map(bulkShareResultV2Mapper::mapMinimal)
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(
		summary = "Retrieve an attachment of a custom entity",
		description = "Get the attachment with the provided key for a custom entity",
	)
	@GetMapping("/{entityType}/{entityId}/attachments/{key}")
	fun getAttachment(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@Parameter(description = "Key of the attachment to retrieve")
		@PathVariable key: String,
		@RequestParam(required = false) fileName: String?,
		response: ServerHttpResponse,
	): Mono<Void> = response.writeWith(
		flow {
			withAttachmentsEnabled(entityType) {
				val (entity, attachment) = customEntityService.getAttachment(entityType = entityType, entityId = entityId, attachmentKey = key)
				response.headers["Content-Type"] = entity.dataAttachments.getValue(key).mimeTypeOrDefault
				response.headers["Content-Disposition"] = "attachment; filename=\"${fileName ?: key}\""
				emitAll(attachment)
			}
		}.injectReactorContext(),
	)

	@Operation(
		summary = "Creates or modifies an attachment of a custom entity",
		description = "Creates or modifies the attachment with the provided key for a custom entity and returns the modified entity afterward",
	)
	@PutMapping("/{entityType}/{entityId}/attachments/{key}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun setAttachment(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@Parameter(description = "Key of the attachment to update")
		@PathVariable key: String,
		@Parameter(
			description = "Revision of the latest known version of the entity. If the revision does not match the current version of the entity the method will fail with CONFLICT status",
		)
		@RequestParam(required = true) rev: String,
		@Parameter(description = "Utis for the attachment")
		@RequestParam(required = false) utis: List<String>?,
		@RequestParam(required = false) compressionAlgorithm: String?,
		@RequestParam(required = false) triedCompressionAlgorithmsVersion: String?,
		@RequestParam(required = false) realDataSize: Long?,
		@Schema(type = "string", format = "binary")
		@RequestBody payload: Flow<DataBuffer>,
		@RequestHeader(name = HttpHeaders.CONTENT_LENGTH, required = false) lengthHeader: Long?,
		@RequestParam(required = false) encrypted: Boolean?,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withAttachmentsEnabled(entityType) {
			val attachmentSize = lengthHeader ?: throw ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Attachment size must be specified in the content-length header",
			)
			updateAttachmentsWrappingExceptions(
				entityType = entityType,
				entityId = entityId,
				entityRev = rev,
				changes = mapOf(
					key to DataAttachmentChange.CreateOrUpdate(
						data = payload,
						size = attachmentSize,
						utis = utis,
						dataIsEncrypted = encrypted ?: false,
						compressionAlgorithm = compressionAlgorithm,
						triedCompressionAlgorithmsVersion = triedCompressionAlgorithmsVersion,
						realDataSize = realDataSize
					),
				),
			)
		}
	}

	@Operation(
		summary = "Deletes an attachment of a custom entity",
		description = "Delete the attachment with the provided key for a custom entity and returns the modified entity afterward",
	)
	@DeleteMapping("/{entityType}/{entityId}/attachments/{key}")
	fun deleteAttachment(
		@PathVariable entityType: String,
		@PathVariable entityId: String,
		@Parameter(description = "Key of the attachment to delete")
		@PathVariable key: String,
		@Parameter(
			description = "Revision of the latest known version of the entity. If the revision does not match the current version of the entity the method will fail with CONFLICT status",
		)
		@RequestParam(required = true) rev: String,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withAttachmentsEnabled(entityType) {
			updateAttachmentsWrappingExceptions(
				entityType = entityType,
				entityId = entityId,
				entityRev = rev,
				changes = mapOf(key to DataAttachmentChange.Delete),
			)
		}
	}

	@Operation(
		summary = "Creates, modifies, or delete the attachments of a custom entity",
		description = "Batch operation to modify multiple attachments of a custom entity at once",
	)
	@PutMapping("/{entityType}/{entityId}/attachments", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun setAttachments(
		@PathVariable entityType: String,
		@Parameter(description = "Id of the entity to update")
		@PathVariable entityId: String,
		@Parameter(
			description = "Revision of the latest known version of the entity. If the revision does not match the current version of the entity the method will fail with CONFLICT status",
		)
		@RequestParam(required = true) rev: String,
		@Parameter(description = "Describes the operations to execute with this update.")
		@RequestPart("options", required = true) options: CustomEntityBulkAttachmentUpdateOptions,
		@Parameter(description = "New attachments (to create or update). The file name will be used as the attachment key.")
		@RequestPart("attachments", required = false) attachments: Flux<FilePart>?,
	): Mono<CustomEntityBaseDto> = reactorCacheInjector.monoWithCachedContext(10) {
		withAttachmentsEnabled(entityType) {
			val attachmentsByKey: Map<String, FilePart> =
				attachments?.asFlow()?.toList()?.let { partsList ->
					partsList.associateBy { it.filename() }.also { partsMap ->
						require(partsList.size == partsMap.size) {
							"Duplicate keys for new attachments ${partsList.groupingBy { it.filename() }.eachCount().filter { it.value > 1 }.keys}"
						}
					}
				} ?: emptyMap()
			require(attachmentsByKey.values.all { it.headers().contentType != null }) {
				"Each attachment part must specify a ${HttpHeaders.CONTENT_TYPE} header."
			}
			require(attachmentsByKey.keys.containsAll(options.updateAttachmentsMetadata.keys)) {
				"Missing attachments for metadata: ${options.updateAttachmentsMetadata.keys - attachmentsByKey.keys}"
			}
			require(attachmentsByKey.isNotEmpty() || options.deleteAttachments.isNotEmpty()) { "Nothing to do" }
			updateAttachmentsWrappingExceptions(
				entityType = entityType,
				entityId = entityId,
				entityRev = rev,
				changes = options.deleteAttachments.associateWith { DataAttachmentChange.Delete } +
					attachmentsByKey.mapValues { (key, value) ->
						makeMultipartAttachmentUpdate("attachment $key", value, options.updateAttachmentsMetadata[key])
					},
			)
		}
	}

	private fun makeMultipartAttachmentUpdate(
		name: String,
		part: FilePart,
		metadata: BulkAttachmentUpdateOptions.AttachmentMetadata?,
	): DataAttachmentChange.CreateOrUpdate {
		val size = part.headers().contentLength.takeIf { it > 0 } ?: throw ResponseStatusException(
			HttpStatus.BAD_REQUEST,
			"Missing size information for $name: you must provide the size of the attachment in bytes using a Content-Length part header.",
		)
		return DataAttachmentChange.CreateOrUpdate(
			data = part.content().asFlow().enforceSize(size),
			size = size,
			utis = metadata?.utis,
			dataIsEncrypted = metadata?.dataIsEncrypted ?: false,
			compressionAlgorithm = metadata?.compressionAlgorithm,
			triedCompressionAlgorithmsVersion = metadata?.triedCompressionAlgorithmsVersion,
			realDataSize = metadata?.realDataSize
		)
	}

	private suspend fun updateAttachmentsWrappingExceptions(
		entityType: String,
		entityId: String,
		entityRev: String,
		changes: Map<String, DataAttachmentChange>,
	): CustomEntityBaseDto = try {
		checkNotNull(
			customEntityService.updateAttachments(entityType = entityType, entityId = entityId, entityRev = entityRev, changes = changes)
		) { "Could not update $entityType $entityId" }.toDto(entityType)
	} catch (_: ObjectStorageException) {
		throw ResponseStatusException(
			HttpStatus.SERVICE_UNAVAILABLE,
			"One or more attachments must be stored using the object storage service, but the service is currently unavailable.",
		)
	}
}
