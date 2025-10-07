package org.taktik.icure.services.external.rest.v2.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.customentities.config.ExtensionConfiguration
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.util.CachedCustomEntitiesConfigurationProvider
import org.taktik.icure.domain.customentities.util.CustomEntityConfigResolutionContext
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.domain.filter.predicate.Predicate
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.pagination.NextPageElement
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.PaginationRowElement
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.requests.BulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import java.io.Serializable

object MappersWithCustomExtensions {
	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (DTO, (ObjectNode?) -> ObjectNode?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): OBJ {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		return if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			doMap(dto) {
				if (it != null) extension.validateAndMapValueForStore(
					context,
					ResolutionPath(arrayListOf(getPathRoot(dto))),
					it
				) else null
			}
		} else {
			doMap(dto) { it }
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDomainWithExtension(
		obj: OBJ,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): DTO {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		return if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			doMap(obj) {
				if (it != null) extension.mapValueForRead(
					context,
					it
				) else null
			}
		} else {
			doMap(obj) { it }
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (DTO, (ObjectNode?) -> ObjectNode?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): List<OBJ> {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		return if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			val path = ResolutionPath(ArrayList())
			dtos.map { dto ->
				doMap(dto) {
					if (it != null) path.appending(getPathRoot(dto)) {
						extension.validateAndMapValueForStore(
							context,
							path,
							it
						)
					} else null
				}
			}
		} else {
			dtos.map { dto -> doMap(dto) { it } }
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDomainWithExtension(
		objs: List<OBJ>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): List<DTO> {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		return if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			objs.map { obj ->
				doMap(obj) {
					if (it != null) extension.mapValueForRead(
						context,
						it
					) else null
				}
			}
		} else {
			objs.map { obj -> doMap(obj) { it } }
		}
	}

	inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: Flow<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMap: (DTO, (ObjectNode?) -> ObjectNode?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): Flow<OBJ> = flow {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			val path = ResolutionPath(ArrayList())
			emitAll(dtos.map { dto ->
				doMap(dto) {
					if (it != null) path.appending(getPathRoot(dto)) {
						extension.validateAndMapValueForStore(
							context,
							path,
							it
						)
					} else null
				}
			})
		} else {
			emitAll(dtos.map { dto -> doMap(dto) { it } })
		}
	}

	inline fun <DTO, OBJ> mapFromDomainWithExtension(
		objs: Flow<OBJ>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMap: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): Flow<DTO> = flow {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getExtension()
		if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			emitAll(objs.map { obj ->
				doMap(obj) {
					if (it != null) extension.mapValueForRead(
						context,
						it
					) else null
				}
			})
		} else {
			emitAll(objs.map { obj -> doMap(obj) { it } })
		}
	}

	inline fun <DTO : Any, OBJ : HasEncryptionMetadata> mapFromDomainWithExtension(
		shareResults: Flow<EntityBulkShareResult<OBJ>>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		bulkShareResultV2Mapper: BulkShareResultV2Mapper,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): Flow<EntityBulkShareResultDto<DTO>> = flow {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getEntityExtension()
		if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			emitAll(shareResults.map { shareResult ->
				bulkShareResultV2Mapper.map(shareResult) { obj ->
					if (obj != null)
						doMapEntity(obj) {
							if (it != null) extension.mapValueForRead(
								context,
								it
							) else null
						}
					else
						null
				}
			})
		} else {
			emitAll(shareResults.map { shareResult ->
				bulkShareResultV2Mapper.map(shareResult) { obj ->
					if (obj != null) doMapEntity(obj) { it } else null
				}
			})
		}
	}

	inline fun <reified OBJ : Identifiable<String>, reified DTO> mapPaginationElementsWithExtensions(
		paginationElements: Flow<PaginationElement>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): Flow<PaginationElement> = flow {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getEntityExtension()
		val context = CustomEntityConfigResolutionContext.ofConfig(config)
		emitAll(paginationElements.map { paginationElement ->
			when (paginationElement) {
				is NextPageElement<*> -> paginationElement
				is PaginationRowElement<*, *> ->
					PaginationRowElement(
						element = doMapEntity(paginationElement.element as OBJ) {
							if (extension != null && it != null) {
								extension.mapValueForRead(context, it)
							} else it
						},
						key = paginationElement.key,
					)
			}
		})
	}

	suspend inline fun <OBJ : Identifiable<String>, DTO : Serializable> paginatedListWithExtensions(
		events: Flow<ViewQueryResultEvent>,
		realLimit: Int,
		objectMapper: ObjectMapper,
		predicate: Predicate? = null,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (ObjectNode?) -> ObjectNode?) -> DTO,
	): PaginatedList<DTO> {
		val config = customEntitiesConfigurationProvider.getCurrentUserCustomConfig()
		val extension = config.extensions.getEntityExtension()
		val context = CustomEntityConfigResolutionContext.ofConfig(config)
		return events.paginatedList<OBJ, DTO>(
			mapper = if (extension != null) ({ obj ->
				doMapEntity(obj) { if (it != null) extension.mapValueForRead(context, it) else null }
			}) else ({ obj ->
				doMapEntity(obj) { it }
			}),
			realLimit = realLimit,
			objectMapper = objectMapper,
			predicate = predicate
		)
	}
}