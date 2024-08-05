/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
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
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.BooleanResponseDto
import org.taktik.icure.services.external.rest.v1.dto.CodeDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/code")
@Tag(name = "code")
class CodeController(
    private val codeService: CodeService,
    private val codeMapper: CodeMapper,
    private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
    private val objectMapper: ObjectMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	@Operation(summary = "Get paginated list of codes by code, type and version.", description = "Returns a list of codes matched with given input. If several types are provided, pagination is not supported")
	@GetMapping("/byLabel")
	fun findPaginatedCodesByLabel(
		@RequestParam(required = false) region: String?,
		@RequestParam(required = false) types: String,
		@RequestParam(required = false) language: String,
		@RequestParam(required = false) label: String,
		@RequestParam(required = false) version: String?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<CodeDto> {
		if((sanitizeString(label)?.length ?: 0) < 3) throw IllegalArgumentException("Label must contain at least 3 characters")

		val startKeyElements = startKey?.let { objectMapper.readValue<List<String?>>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		val typesList = types.split(',').toSet()
		return codeService
			.findCodesByLabel(region, language, typesList, label, version, paginationOffset)
			.mapElements(codeMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Gets paginated list of codes by code, type and version.", description = "Returns a list of codes matched with given input.")
	@GetMapping
	fun findPaginatedCodes(
		@RequestParam(required = true) region: String,
		@RequestParam(required = false) type: String?,
		@RequestParam(required = false) code: String?,
		@RequestParam(required = false) version: String?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	): PaginatedFlux<CodeDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String?>>(it) }
		val paginationOffset = PaginationOffset(
			startKeyElements,
			startDocumentId,
			null,
			limit ?: paginationConfig.defaultLimit
		)

		return codeService
			.findCodesBy(region, type, code, version, paginationOffset)
			.mapElements(codeMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Gets paginated list of codes by link and link type.", description = "Returns a list of codes matched with given input.")
	@GetMapping("link/{linkType}")
	fun findPaginatedCodesWithLink(
		@PathVariable linkType: String,
		@RequestParam(required = false) linkedId: String?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey")
		@RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A code document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	) : PaginatedFlux<CodeDto> {
		val startKeyElements: List<String>? = startKey?.let { objectMapper.readValue<List<String>>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return codeService
			.findCodesByQualifiedLinkId(linkType, linkedId, paginationOffset)
			.mapElements(codeMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Gets list of codes by code, type and version", description = "Returns a list of codes matched with given input.")
	@GetMapping("/byRegionTypeCode")
	fun findCodes(
		@Parameter(description = "Code region") @RequestParam(required = true) region: String,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?,
		@Parameter(description = "Code code") @RequestParam(required = false) code: String?,
		@Parameter(description = "Code version") @RequestParam(required = false) version: String?
	) = codeService.findCodesBy(region, type, code, version)
			.map { c -> codeMapper.map(c) }
			.injectReactorContext()

	@Operation(summary = "Get list of code types by region and type.", description = "Returns a list of code types matched with given input.")
	@GetMapping("/codetype/byRegionType", produces = [APPLICATION_JSON_VALUE])
	fun findCodeTypes(
		@Parameter(description = "Code region") @RequestParam(required = false) region: String?,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?
	) = codeService.listCodeTypesBy(region, type).injectReactorContext()

	@Operation(summary = "Gets list of tag types by region and type.", description = "Returns a list of tag types matched with given input.")
	@GetMapping("/tagtype/byRegionType", produces = [APPLICATION_JSON_VALUE])
	fun findTagTypes(
		@Parameter(description = "Code region") @RequestParam(required = false) region: String?,
		@Parameter(description = "Code type") @RequestParam(required = false) type: String?
	): Flux<String> {
		val tagTypeCandidates = codeService.getTagTypeCandidates()
		return codeService.listCodeTypesBy(region, type)
			.filter { tagTypeCandidates.contains(it) }
			.injectReactorContext()
	}

	@Operation(summary = "Create a code", description = "Create a code entity. Fields Type, Code and Version are required.")
	@PostMapping
	fun createCode(@RequestBody c: CodeDto) = mono {
		val code = codeService.create(codeMapper.map(c))
		code?.let { codeMapper.map(it) }
	}

	@Operation(summary = "Create a batch of codes", description = "Create a batch of code entities. Fields Type, Code and Version are required for each code.")
	@PostMapping("/batch")
	fun createCodes(@RequestBody codeBatch: List<CodeDto>) = mono {
		val codes = codeBatch.map { codeMapper.map(it) }
		try {
			codeService.create(codes)?.map { codeMapper.map(it) }
		} catch (e: IllegalStateException) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}

	@Operation(summary = "Checks if a code is valid")
	@GetMapping("/isValid")
	fun isCodeValid(
		@RequestParam type: String,
		@RequestParam code: String,
		@RequestParam version: String?
	) = mono {
		BooleanResponseDto(
			response = codeService.isValid(type, code, version)
		)
	}

	@GetMapping("/byRegionLanguagesTypeLabel")
	fun getCodeByRegionLanguageTypeLabel(
		@RequestParam region: String,
		@RequestParam label: String,
		@RequestParam type: String,
		@RequestParam languages: String?
	) = mono {
		(languages?.let {
			codeService.getCodeByLabel(region, label, type, it.split(","))
		} ?: codeService.getCodeByLabel(region, label, type))?.let(codeMapper::map)
	}

	@Operation(summary = "Gets a list of codes by ids", description = "Get a list of codes by ids/keys. Keys must be delimited by coma")
	@GetMapping("/byIds/{codeIds}")
	fun getCodes(@PathVariable codeIds: String): Flux<CodeDto> {
		val codes = codeService.getCodes(codeIds.split(','))
		return codes
			.map { f -> codeMapper.map(f) }
			.injectReactorContext()
	}

	@Operation(summary = "Get a code by id", description = "Get a code based on its id")
	@GetMapping("/{codeId}")
	fun getCode(@Parameter(description = "Code id") @PathVariable codeId: String) = mono {
		val c = codeService.get(codeId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the code. Read the app logs.")
		codeMapper.map(c)
	}

	@Operation(summary = "Get a code", description = "Get a code based on (type, code, version) as query strings. (type, code, version) is unique.")
	@GetMapping("/{type}/{code}/{version}")
	fun getCodeWithParts(
		@Parameter(description = "Code type") @PathVariable type: String,
		@Parameter(description = "Code code") @PathVariable code: String,
		@Parameter(description = "Code version") @PathVariable version: String
	) = mono {

		val c = codeService.get(type, code, version)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A problem regarding fetching the code with parts. Read the app logs.")
		codeMapper.map(c)
	}

	@Operation(summary = "Modify a code", description = "Modification of (type, code, version) is not allowed.")
	@PutMapping
	fun modifyCode(@RequestBody codeDto: CodeDto) = mono {
		val modifiedCode = try {
			codeService.modify(codeMapper.map(codeDto))
		} catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A problem regarding modification of the code. Read the app logs: " + e.message)
		}
		modifiedCode?.let { codeMapper.map(it) }
	}

	@Operation(summary = "Modify a batch of codes", description = "Modification of (type, code, version) is not allowed.")
	@PutMapping("/batch")
	fun modifyCodes(@RequestBody codeBatch: List<CodeDto>) =
		codeService.modify(codeBatch.map { codeMapper.map(it) })
			.catch { e ->
				if (e is IllegalStateException) throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
				else throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A problem regarding modification of the code. Read the app logs: " + e.message)
			}
			.map { codeMapper.map(it) }.injectReactorContext()

	@Operation(summary = "Filter codes", description = "Returns a list of codes along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterCodesBy(
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Skip rows") @RequestParam(required = false) skip: Int?,
		@Parameter(description = "Sort key") @RequestParam(required = false) sort: String?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@RequestBody(required = false) filterChain: FilterChain<CodeDto>
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val startKeyList = startKey?.split(',')?.filter { it.isNotBlank() }?.map { it.trim() } ?: listOf()
		val paginationOffset = PaginationOffset(startKeyList, startDocumentId, skip, realLimit + 1)

		codeService.listCodes(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow(), sort, desc)
			.paginatedList(codeMapper::map, realLimit)
	}

	@Operation(summary = "Get ids of code matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match", produces = [APPLICATION_JSON_VALUE])
	fun matchCodesBy(
		@RequestBody filter: AbstractFilterDto<CodeDto>,
		@RequestParam(required = false) deduplicate: Boolean? = null
	) =
		codeService.matchCodesBy(
			filter = filterMapper.tryMap(filter).orThrow(),
			deduplicate = deduplicate ?: false
		).injectReactorContext()

	@Operation(summary = "Import codes", description = "Import codes from the resources XML file depending on the passed pathVariable")
	@PostMapping("/{codeType}")
	fun importCodes(@PathVariable codeType: String) = mono {
		val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)
		resolver.getResources("classpath*:/org/taktik/icure/db/codes/$codeType.*.xml").forEach {
			it.filename?.let { filename ->
				val md5 = filename.replace(Regex(".+\\.([0-9a-f]{20}[0-9a-f]+)\\.xml"), "$1")
				codeService.importCodesFromXml(md5, filename.replace(Regex("(.+)\\.[0-9a-f]{20}[0-9a-f]+\\.xml"), "$1"), it.inputStream)
			} ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong codeType provided.")
		}
	}
}