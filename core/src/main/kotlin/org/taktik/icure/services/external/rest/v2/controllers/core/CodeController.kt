/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
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
import org.taktik.icure.asyncservice.CodeService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.BooleanResponseDto
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.mapper.IdWithRevV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.monoWrappingResponseToJson
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("codeControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/code")
@Tag(name = "code")
class CodeController(
	private val codeService: CodeService,
	private val codeV2Mapper: CodeV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val objectMapper: ObjectMapper,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val idWithRevV2Mapper: IdWithRevV2Mapper,
	private val paginationConfig: SharedPaginationConfig
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Operation(
		summary = "Finding codes by code, type and version with pagination.",
		description = "Returns a list of codes matched with given input. If several types are provided, pagination is not supported",
	)
	@GetMapping("/byLabel")
	fun findCodesByLabel(
		@RequestParam(required = true) region: String?,
		@RequestParam(required = true) types: String,
		@RequestParam(required = true) language: String,
		@RequestParam(required = true) label: String,
		@RequestParam(required = false) version: String?,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CodeDto> {
		if ((sanitizeString(label)?.length ?: 0) < 3) throw IllegalArgumentException("Label must contain at least 3 characters")

		val startKeyElements = startKey?.let { objectMapper.readValue<List<String?>>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		val typesList = types.split(',').toSet()
		return codeService
			.findCodesByLabel(region, language, typesList, label, version, paginationOffset)
			.mapElements(codeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Finding codes by code, type and version with pagination.",
		description = "Returns a list of codes matched with given input.",
	)
	@GetMapping
	fun findCodesByType(
		@RequestParam(required = true) region: String,
		@RequestParam(required = false) type: String?,
		@RequestParam(required = false) code: String?,
		@RequestParam(required = false) version: String?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CodeDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String?>>(it) }
		val paginationOffset =
			PaginationOffset(
				startKeyElements,
				startDocumentId,
				null,
				limit ?: paginationConfig.defaultLimit,
			)

		return codeService
			.findCodesBy(region, type, code, version, paginationOffset)
			.mapElements(codeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Finding codes by code, type and version with pagination.",
		description = "Returns a list of codes matched with given input.",
	)
	@GetMapping("/byLink/{linkType}")
	fun findCodesByLink(
		@PathVariable linkType: String,
		@RequestParam(required = false) linkedId: String?,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		)
		@RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<CodeDto> {
		val startKeyElements: List<String>? = startKey?.let { objectMapper.readValue<List<String>>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return codeService
			.findCodesByQualifiedLinkId(linkType, linkedId, paginationOffset)
			.mapElements(codeV2Mapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Finding codes by code, type and version", description = "Returns a list of codes matched with given input.")
	@GetMapping("/byRegionTypeCode")
	fun listCodesByRegionTypeCodeVersion(
		@Parameter(description = "Code region") @RequestParam(required = true) region: String,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?,
		@Parameter(description = "Code code") @RequestParam(required = false) code: String?,
		@Parameter(description = "Code version") @RequestParam(required = false) version: String?,
	): Flux<CodeDto> = codeService
		.findCodesBy(region, type, code, version)
		.map { c -> codeV2Mapper.map(c) }
		.injectReactorContext()

	@Operation(summary = "Finding code types.", description = "Returns a list of code types matched with given input.")
	@GetMapping("/codetype/byRegionType", produces = [APPLICATION_JSON_VALUE])
	fun listCodeTypesBy(
		@Parameter(description = "Code region") @RequestParam(required = false) region: String?,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?,
	): Flux<String> = codeService.listCodeTypesBy(region, type).injectReactorContext()

	@Operation(summary = "Finding tag types.", description = "Returns a list of tag types matched with given input.")
	@GetMapping("/tagtype/byRegionType", produces = [APPLICATION_JSON_VALUE])
	fun listTagTypesBy(
		@Parameter(description = "Code region") @RequestParam(required = false) region: String?,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?,
	): Flux<String> {
		val tagTypeCandidates = codeService.getTagTypeCandidates()
		return codeService
			.listCodeTypesBy(region, type)
			.filter { tagTypeCandidates.contains(it) }
			.injectReactorContext()
	}

	@Operation(summary = "Create a Code", description = "Type, Code and Version are required.")
	@PostMapping
	fun createCode(
		@RequestBody c: CodeDto,
	): Mono<CodeDto> = mono {
		codeV2Mapper.map(codeService.create(codeV2Mapper.map(c)))
	}

	@Operation(
		summary = "Create a batch of codes",
		description = "Create a batch of code entities. Fields Type, Code and Version are required for each code.",
	)
	@PostMapping("/batch")
	fun createCodes(
		@RequestBody codeBatch: List<CodeDto>,
	): Flux<CodeDto> = codeService.create(
		codeBatch.map(codeV2Mapper::map)
	).map(codeV2Mapper::map).injectReactorContext()

	@Operation(summary = "Checks if a code is valid")
	@GetMapping("/isValid")
	fun isCodeValid(
		@RequestParam type: String,
		@RequestParam code: String,
		@RequestParam version: String?,
	): Mono<BooleanResponseDto> = mono {
		BooleanResponseDto(
			response = codeService.isValid(type, code, version),
		)
	}

	@Deprecated("This method gives invalid json if no matching code is found, use byRegionLanguagesTypeLabelOr404")
	@GetMapping("/byRegionLanguagesTypeLabel")
	fun getCodeByRegionLanguageTypeLabel(
		@RequestParam region: String,
		@RequestParam label: String,
		@RequestParam type: String,
		@RequestParam languages: String?,
	): Mono<ResponseEntity<CodeDto?>> = monoWrappingResponseToJson {
		val code =
			languages?.let {
				codeService.getCodeByLabel(region, label, type, it.split(","))
			} ?: codeService.getCodeByLabel(region, label, type)
		code?.let { codeV2Mapper.map(it) }
	}

	@GetMapping("/byRegionLanguagesTypeLabelOr404")
	fun getCodeByRegionLanguageTypeLabelOr404(
		@RequestParam region: String,
		@RequestParam label: String,
		@RequestParam type: String,
		@RequestParam languages: String?,
	): Mono<CodeDto> = mono {
		codeV2Mapper.map(
			languages?.let {
				codeService.getCodeByLabel(region, label, type, it.split(","))
			} ?: codeService.getCodeByLabel(region, label, type)
			?: throw NotFoundRequestException("No code found for region=$region, label=$label, type=$type${if (languages != null) ", languages=$languages" else ""}")
		)
	}

	@Operation(summary = "Get a list of codes by ids")
	@PostMapping("/byIds")
	fun getCodes(
		@RequestBody codeIds: ListOfIdsDto,
	): Flux<CodeDto> = codeIds.ids
		.takeIf { it.isNotEmpty() }
		?.let { ids -> codeService.getCodes(ids).map { f -> codeV2Mapper.map(f) }.injectReactorContext() }
		?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.").also {
			logger.error(it.message)
		}

	@Operation(
		summary = "Get a Code",
		description = "Get a code based on ID or (code,type,version) as query strings. (code,type,version) is unique.",
	)
	@GetMapping("/{codeId}")
	fun getCode(
		@Parameter(description = "Code id") @PathVariable codeId: String,
	): Mono<CodeDto> = mono {
		val c = codeService.get(codeId)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the code. Read the app logs.")
		codeV2Mapper.map(c)
	}

	@Operation(
		summary = "Get a code",
		description = "Get a code based on ID or (code,type,version) as query strings. (code,type,version) is unique.",
	)
	@GetMapping("/{type}/{code}/{version}")
	fun getCodeWithParts(
		@Parameter(description = "Code type") @PathVariable type: String,
		@Parameter(description = "Code code") @PathVariable code: String,
		@Parameter(description = "Code version") @PathVariable version: String,
	): Mono<CodeDto> = mono {
		val c =
			codeService.get(type, code, version)
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the code with parts. Read the app logs.")
		codeV2Mapper.map(c)
	}

	@Operation(summary = "Modify a code", description = "Modification of (type, code, version) is not allowed.")
	@PutMapping
	fun modifyCode(
		@RequestBody codeDto: CodeDto,
	): Mono<CodeDto> = mono {
		val modifiedCode = codeService.modify(codeV2Mapper.map(codeDto))
		codeV2Mapper.map(modifiedCode)
	}

	@Operation(summary = "Modify a batch of codes", description = "Modification of (type, code, version) is not allowed.")
	@PutMapping("/batch")
	fun modifyCodes(
		@RequestBody codeBatch: List<CodeDto>,
	): Flux<CodeDto> = codeService
		.modify(codeBatch.map { codeV2Mapper.map(it) })
		.map { codeV2Mapper.map(it) }
		.injectReactorContext()

	@Operation(
		summary = "Filter codes ",
		description = "Returns a list of codes along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterCodesBy(
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Skip rows") @RequestParam(required = false) skip: Int?,
		@Parameter(description = "Sort key") @RequestParam(required = false) sort: String?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@RequestBody(required = false) filterChain: FilterChain<CodeDto>,
	): Mono<PaginatedList<CodeDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val startKeyList = startKey?.split(',')?.filter { it.isNotBlank() }?.map { it.trim() } ?: listOf()

		val paginationOffset = PaginationOffset(startKeyList, startDocumentId, skip, realLimit + 1)
		codeService
			.listCodes(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow(), sort, desc)
			.paginatedList(codeV2Mapper::map, realLimit, objectMapper = objectMapper)
	}

	@Operation(summary = "Get the ids of the Codes matching a filter")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchCodesBy(
		@RequestBody filter: AbstractFilterDto<CodeDto>,
	): Flux<String> = codeService
		.matchCodesBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(summary = "Import codes", description = "Import codes from the resources XML file depending on the passed pathVariable")
	@PostMapping("/{codeType}")
	fun importCodes(
		@PathVariable codeType: String,
	): Mono<Unit> = mono {
		val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
		resolver.getResources("classpath*:/org/taktik/icure/db/codes/$codeType.*.xml").forEach {
			it.filename?.let { filename ->
				val md5 = filename.replace(Regex(".+\\.([0-9a-f]{20}[0-9a-f]+)\\.xml"), "$1")
				codeService.importCodesFromXml(md5, filename.replace(Regex("(.+)\\.[0-9a-f]{20}[0-9a-f]+\\.xml"), "$1"), it.inputStream)
			} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong codeType provided.")
		}
	}

	@DeleteMapping("/{codeId}")
	fun deleteCode(
		@PathVariable codeId: String,
		@RequestParam rev: String,
	): Mono<DocIdentifierDto> = mono {
		codeService.deleteCode(codeId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/delete/batch")
	fun deleteCodes(
		@RequestBody codeIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = codeService
		.deleteCodes(
			codeIds.ids.map(idWithRevV2Mapper::map),
		).map(docIdentifierV2Mapper::map)
		.injectReactorContext()

	@PostMapping("/undelete/{codeId}")
	fun undeleteCode(
		@PathVariable codeId: String,
		@RequestParam rev: String,
	): Mono<CodeDto> = mono {
		codeService.undeleteCode(codeId, rev).let(codeV2Mapper::map)
	}

	@PostMapping("/undelete/batch")
	fun undeleteCodes(
		@RequestBody codeIds: ListOfIdsAndRevDto,
	): Flux<CodeDto> = codeService
		.undeleteCodes(
			codeIds.ids.map(idWithRevV2Mapper::map),
		).map(codeV2Mapper::map)
		.injectReactorContext()

	@DeleteMapping("/purge/{codeId}")
	fun purgeCode(
		@PathVariable codeId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		codeService.purgeCode(codeId, rev).let(docIdentifierV2Mapper::map)
	}

	@PostMapping("/purge/batch")
	fun purgeCodes(
		@RequestBody codeIds: ListOfIdsAndRevDto,
	): Flux<DocIdentifierDto> = codeService
		.purgeCodes(
			codeIds.ids.map(idWithRevV2Mapper::map),
		).map(docIdentifierV2Mapper::map)
		.injectReactorContext()
}
