package org.taktik.icure.services.external.rest.v2.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.RawJson
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
	val mapFromDtoWithNoConfig = { it: RawJson.JsonObject? ->
		if (it != null)
			throw IllegalArgumentException("Can't use extension value when no extension is configured")
		else
			it
	}

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dto: DTO,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (DTO, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): OBJ {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
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
			doMap(dto, mapFromDtoWithNoConfig)
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDomainWithExtension(
		obj: OBJ,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): DTO {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
		return if (extension != null) {
			val context = CustomEntityConfigResolutionContext.ofConfig(config)
			doMap(obj) {
				if (it != null) extension.mapValueForRead(
					context,
					it
				) else null
			}
		} else {
			doMap(obj) { null }
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: List<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (DTO, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): List<OBJ> {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
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
			dtos.map { dto -> doMap(dto, mapFromDtoWithNoConfig) }
		}
	}

	suspend inline fun <DTO, OBJ> mapFromDomainWithExtension(
		objs: List<OBJ>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		doMap: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): List<DTO> {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
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
			objs.map { obj -> doMap(obj) { null } }
		}
	}

	inline fun <DTO, OBJ> mapFromDtoWithExtension(
		dtos: Flow<DTO>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMap: (DTO, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> OBJ,
		crossinline getPathRoot: (DTO) -> String
	): Flow<OBJ> = flow {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
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
			emitAll(dtos.map { dto -> doMap(dto, mapFromDtoWithNoConfig) })
		}
	}

	inline fun <DTO, OBJ> mapFromDomainWithExtension(
		objs: Flow<OBJ>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMap: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): Flow<DTO> = flow {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getExtension()
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
			emitAll(objs.map { obj -> doMap(obj) { null } })
		}
	}

	inline fun <DTO : Any, OBJ : HasEncryptionMetadata> mapFromDomainWithExtension(
		shareResults: Flow<EntityBulkShareResult<OBJ>>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		bulkShareResultV2Mapper: BulkShareResultV2Mapper,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): Flow<EntityBulkShareResultDto<DTO>> = flow {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getEntityExtension()
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
					if (obj != null) {
						doMapEntity(obj) { null }
					} else null
				}
			})
		}
	}

	inline fun <reified OBJ : Identifiable<String>, reified DTO> mapPaginationElementsWithExtensions(
		paginationElements: Flow<PaginationElement>,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): Flow<PaginationElement> = flow {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getEntityExtension()
		val context = config?.let { CustomEntityConfigResolutionContext.ofConfig(it) }
		emitAll(
			if (extension != null && context != null)
				paginationElements.map { paginationElement ->
					when (paginationElement) {
						is NextPageElement<*> -> paginationElement
						is PaginationRowElement<*, *> ->
							PaginationRowElement(
								element = doMapEntity(paginationElement.element as OBJ) {
									if (it != null) {
										extension.mapValueForRead(context, it)
									} else it
								},
								key = paginationElement.key,
							)
					}
				}
			else
				paginationElements.map { paginationElement ->
					when (paginationElement) {
						is NextPageElement<*> -> paginationElement
						is PaginationRowElement<*, *> ->
							PaginationRowElement(
								element = doMapEntity(paginationElement.element as OBJ) { null },
								key = paginationElement.key,
							)
					}
				}
		)
	}

	suspend inline fun <OBJ : Identifiable<String>, DTO : Serializable> paginatedListWithExtensions(
		events: Flow<ViewQueryResultEvent>,
		realLimit: Int,
		objectMapper: ObjectMapper,
		predicate: Predicate? = null,
		customEntitiesConfigurationProvider: CachedCustomEntitiesConfigurationProvider,
		crossinline getEntityExtension: ExtensionConfiguration.() -> ObjectDefinition?,
		crossinline doMapEntity: (OBJ, (RawJson.JsonObject?) -> RawJson.JsonObject?) -> DTO,
	): PaginatedList<DTO> {
		val config = customEntitiesConfigurationProvider.getConfigForCurrentUser()
		val extension = config?.extensions?.getEntityExtension()
		val context = config?.let { CustomEntityConfigResolutionContext.ofConfig(it) }
		return events.paginatedList<OBJ, DTO>(
			mapper = if (extension != null && context != null) ({ obj ->
				doMapEntity(obj) { if (it != null) extension.mapValueForRead(context, it) else null }
			}) else ({ obj ->
				doMapEntity(obj) { null }
			}),
			realLimit = realLimit,
			objectMapper = objectMapper,
			predicate = predicate
		)
	}
}