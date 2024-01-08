/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Splitter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.asyncservice.AccessLogService
import org.taktik.icure.asyncservice.HealthcarePartyService
import org.taktik.icure.asyncservice.PatientService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.Sorting
import org.taktik.icure.entities.Patient
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.PatientHealthCarePartyV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.utils.orThrow
import org.taktik.icure.services.external.rest.v2.mapper.requests.EntityShareOrMetadataUpdateRequestV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.requests.PatientBulkShareResultV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.services.external.rest.v2.utils.paginatedListOfIds
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.injectCachedReactorContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import javax.security.auth.login.LoginException

@RestController("patientControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/patient")
@Tag(name = "patient")
class PatientController(
	private val sessionLogic: SessionInformationProvider,
	private val accessLogService: AccessLogService,
	private val filters: Filters,
	private val patientService: PatientService,
	private val healthcarePartyService: HealthcarePartyService,
	private val patientV2Mapper: PatientV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val addressV2Mapper: AddressV2Mapper,
	private val patientHealthCarePartyV2Mapper: PatientHealthCarePartyV2Mapper,
	private val objectMapper: ObjectMapper,
	private val bulkShareResultV2Mapper: PatientBulkShareResultV2Mapper,
	private val entityShareOrMetadataUpdateRequestV2Mapper: EntityShareOrMetadataUpdateRequestV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector
) {

	private val patientToPatientDto = { it: Patient -> patientV2Mapper.map(it) }

	@Operation(summary = "Find patients for the current user (HcParty) ", description = "Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " + "Null it means that this is the last page.")
	@GetMapping("/byNameBirthSsinAuto")
	fun findPatientsByNameBirthSsinAuto(
		@Parameter(description = "HealthcareParty Id, if unset will user user's hcpId") @RequestParam(required = false) healthcarePartyId: String?,
		@Parameter(description = "Optional value for filtering results") @RequestParam(required = false) filterValue: String?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.") @RequestParam(required = false, defaultValue = "asc") sortDirection: String
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String>>(startKey, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)

		val currentHealthcarePartyId = sessionLogic.getCurrentHealthcarePartyId()

		currentHealthcarePartyId.let { currentHcpId ->
			val hcp = healthcarePartyService.getHealthcareParty(currentHcpId)
			(hcp?.parentId?.let { it.ifEmpty { null } } ?: hcp?.id)?.let { hcpId ->
				patientService.findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
				hcpId,
				paginationOffset,
				filterValue,
				Sorting(null, sortDirection)
			).paginatedList(patientToPatientDto, realLimit)
			} ?: PaginatedList()
		}
	}

	@Operation(summary = "List patients that have been merged towards another patient ", description = "Returns a list of patients that have been merged after the provided date")
	@GetMapping("/merges/{date}")
	fun listOfMergesAfter(@PathVariable date: Long) =
		patientService.listOfMergesAfter(date).map { patientV2Mapper.map(it) }.injectReactorContext()

	@Operation(summary = "List patients that have been modified after the provided date", description = "Returns a list of patients that have been modified after the provided date")
	@GetMapping("/modifiedAfter/{date}")
	fun findPatientsModifiedAfter(
		@PathVariable date: Long,
		@Parameter(description = "The start key for pagination the date of the first element of the new page") @RequestParam(required = false) startKey: Long?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	) = mono {
		patientService.listOfPatientsModifiedAfter(
			date, startKey, startDocumentId,
			(
				limit
					?: DEFAULT_LIMIT
				) + 1
		).paginatedList(patientToPatientDto, limit ?: DEFAULT_LIMIT)
	}

	@Suppress("DEPRECATION")
	@GetMapping("/{patientId}/keys")
	fun getPatientHcPartyKeysForDelegate(@Parameter(description = "The patient Id for which information is shared") @PathVariable patientId: String) = mono {
		patientService.getHcPartyKeysForDelegate(patientId)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)"
	)
	@GetMapping("/{patientId}/aesExchangeKeys")
	fun getPatientAesExchangeKeysForDelegate(@PathVariable patientId: String) = mono {
		patientService.getAesExchangeKeysForDelegate(patientId)
	}

	@Operation(summary = "Get count of patients for a specific HcParty or for the current HcParty ", description = "Returns the count of patients")
	@GetMapping("/hcParty/{hcPartyId}/count")
	fun countOfPatients(@Parameter(description = "Healthcare party id") @PathVariable hcPartyId: String) = mono {
		ContentDto(numberValue = patientService.countByHcParty(hcPartyId).toDouble())
	}

	@Operation(summary = "List patients for a specific HcParty", description = "Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " + "Null it means that this is the last page.")
	@GetMapping
	fun findPatientsByHealthcareParty(
		@Parameter(description = "Healthcare party id") @RequestParam(required = false) hcPartyId: String?,
		@Parameter(description = "Optional value for sorting results by a given field ('name', 'ssin', 'dateOfBirth'). " + "Specifying this deactivates filtering") @RequestParam(required = false) sortField: String?,
		@Parameter(description = "The start key for pagination: a JSON representation of an array containing all the necessary " + "components to form the Complex Key's startKey") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.") @RequestParam(required = false, defaultValue = "asc") sortDirection: String
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String>>(startKey, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
		sessionLogic.getCurrentHealthcarePartyId().let { currentHcpId ->
			val hcp = healthcarePartyService.getHealthcareParty(currentHcpId)
			(hcp?.parentId ?: hcp?.id)?.let { hcpId ->
				patientService.findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
					hcpId,
					paginationOffset,
					null,
					Sorting(sortField, sortDirection)
				).paginatedList(patientToPatientDto, realLimit)
			} ?: PaginatedList()
		}
	}

	@Operation(summary = "List patients by pages for a specific HcParty", description = "Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " + "Null it means that this is the last page.")
	@GetMapping("/byHcPartyId")
	fun findPatientsIdsByHealthcareParty(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The page first id") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Page size") @RequestParam(required = false) limit: Int?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String>>(startKey, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)
		patientService.findByHcPartyIdsOnly(hcPartyId, paginationOffset).paginatedListOfIds(realLimit)
	}

	@Operation(summary = "Get the patient having the provided externalId")
	@GetMapping("/byExternalId/{externalId}")
	fun getPatientByExternalId(
		@PathVariable("externalId")
		@Parameter(description = "A external ID", required = true) externalId: String
	) = mono {
		patientService.getByExternalId(externalId)?.let(patientToPatientDto)
	}

	@Operation(summary = "Get Paginated List of Patients sorted by Access logs descending")
	@GetMapping("/byAccess/{userId}")
	fun findPatientsByAccessLogUserAfterDate(
		@Parameter(description = "A User ID", required = true) @PathVariable userId: String,
		@Parameter(description = "The type of access (COMPUTER or USER)") @RequestParam(required = false) accessType: String?,
		@Parameter(description = "The start search epoch") @RequestParam(required = false) startDate: Long?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(defaultValue = DEFAULT_LIMIT.toString()) limit: Int
	): Mono<PaginatedList<PatientDto>> = mono {
		accessLogService.aggregatePatientByAccessLogs(userId, accessType, startDate, startKey, startDocumentId, limit).let { (totalSize, count, patients, dateNextKey, nextDocumentId) ->
			val patientDtos = patients.map { patient ->
				PatientDto(
					id = patient.id,
					lastName = patient.lastName,
					firstName = patient.firstName,
					partnerName = patient.partnerName,
					maidenName = patient.maidenName,
					dateOfBirth = patient.dateOfBirth,
					ssin = patient.ssin,
					externalId = patient.externalId,
					patientHealthCareParties = patient.patientHealthCareParties.map { phcp ->
						patientHealthCarePartyV2Mapper.map(
							phcp
						)
					},
					addresses = patient.addresses.map { addressV2Mapper.map(it) }
				)
			}

			PaginatedList(
				nextKeyPair = dateNextKey?.let {
					PaginatedDocumentKeyIdPair(
						it,
						nextDocumentId
					)
				},
				pageSize = limit,
				totalSize = (totalSize * (patientDtos.size / count.toDouble())).toInt(),
				rows = patientDtos.toList()
			)
		}
	}

	@Operation(summary = "Filter patients for the current user (HcParty) ", description = "Returns a list of patients along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterPatientsBy(
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Skip rows") @RequestParam(required = false) skip: Int?,
		@Parameter(description = "Sort key") @RequestParam(required = false) sort: String?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@RequestBody filterChain: FilterChain<PatientDto>
	) = mono {

		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyList = startKey?.takeIf { it.isNotEmpty() }?.let { ArrayList(Splitter.on(",").omitEmptyStrings().trimResults().splitToList(it)) }
		val paginationOffset = PaginationOffset<List<String>>(startKeyList, startDocumentId, skip, realLimit + 1)

		try {
			val patients = patientService.listPatients(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow(), sort, desc)
			log.info("Filter patients in " + (System.currentTimeMillis() - System.currentTimeMillis()) + " ms.")

			patients.paginatedList(patientToPatientDto, realLimit)
		} catch (e: LoginException) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}

	@Operation(summary = "Get ids of patients matching the provided filter for the current user (HcParty) ")
	@PostMapping("/match")
	fun matchPatientsBy(@RequestBody filter: AbstractFilterDto<PatientDto>) = mono {
		filters.resolve(filterV2Mapper.tryMap(filter).orThrow()).toList()
	}

	@Operation(summary = "Filter patients for the current user (HcParty) ", description = "Returns a list of patients")
	@GetMapping("/fuzzy")
	fun fuzzySearch(
		@Parameter(description = "The first name") @RequestParam(required = false) firstName: String,
		@Parameter(description = "The last name") @RequestParam(required = false) lastName: String,
		@Parameter(description = "The date of birth") @RequestParam(required = false) dateOfBirth: Int?
	): Flux<PatientDto> {

		return try {
			patientService.fuzzySearchPatients(firstName, lastName, dateOfBirth)
				.map { patientV2Mapper.map(it) }
				.injectReactorContext()
		} catch (e: Exception) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}

	@Operation(summary = "Create a patient", description = "Name, last name, date of birth, and gender are required. After creation of the patient and obtaining the ID, you need to create an initial delegation.")
	@PostMapping
	fun createPatient(@RequestBody p: PatientDto) = mono {
		val patient = patientService.createPatient(patientV2Mapper.map(p))
		patient?.let(patientToPatientDto) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Patient creation failed.")
	}

	@Operation(summary = "Deletes patients", description = "Response is an array containing the ID of deleted patients.")
	@PostMapping("/delete/batch")
	fun deletePatients(@RequestBody patientIds: ListOfIdsDto): Flux<DocIdentifier> =
		patientService.deletePatients(HashSet(patientIds.ids)).injectReactorContext()

	@Operation(summary = "Deletes a patient", description = "Deletes a patient and returns its identifier.")
	@DeleteMapping("/{patientId}")
	fun deletePatient(@PathVariable patientId: String) = mono {
		patientService.deletePatient(patientId)
	}

	@Operation(summary = "Find deleted patients", description = "Returns a list of deleted patients, within the specified time period, if any.")
	@GetMapping("/deleted/byDate")
	fun findDeletedPatients(
		@Parameter(description = "Filter deletions after this date (unix epoch), included") @RequestParam(required = false) startDate: Long,
		@Parameter(description = "Filter deletions before this date (unix epoch), included") @RequestParam(required = false) endDate: Long?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	) = mono {

		val realLimit = limit ?: DEFAULT_LIMIT
		val paginationOffset = PaginationOffset(startDate, startDocumentId, null, realLimit + 1) // TODO works with descending=true?
		patientService.findDeletedPatientsByDeleteDate(startDate, endDate, desc ?: false, paginationOffset).paginatedList(patientToPatientDto, realLimit)
	}

	@Operation(summary = "Find deleted patients", description = "Returns a list of deleted patients, by name and/or firstname prefix, if any.")
	@GetMapping("/deleted/by_name")
	fun listDeletedPatientsByName(
		@Parameter(description = "First name prefix") @RequestParam(required = false) firstName: String?,
		@Parameter(description = "Last name prefix") @RequestParam(required = false) lastName: String?
	) =
		try {
			patientService.listDeletedPatientsByNames(firstName, lastName).map { patientV2Mapper.map(it) }.injectReactorContext()
		} catch (e: Exception) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}

	@Operation(summary = "undelete previously deleted patients", description = "Response is an array containing the ID of undeleted patient..")
	@PutMapping("/undelete/{patientIds}")
	fun undeletePatient(@PathVariable patientIds: String): Flux<DocIdentifier> {
		val ids = patientIds.split(',')
		if (ids.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		return patientService.undeletePatients(HashSet(ids))
			.injectReactorContext()
	}

	@Operation(summary = "Get patients by id", description = "It gets patient administrative data.")
	@PostMapping("/byIds")
	fun getPatients(@RequestBody patientIds: ListOfIdsDto): Flux<PatientDto> = flow {
		try {
			emitAll(patientService.getPatients(patientIds.ids).map { patientV2Mapper.map(it) })
		} catch (e: java.lang.Exception) {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message).also { log.error(it.message) }
		}
	}.injectReactorContext()

	@Operation(summary = "Get patient", description = "It gets patient administrative data.")
	@GetMapping("/{patientId}")
	fun getPatient(@PathVariable patientId: String) = mono {
		patientService.getPatient(patientId)?.let(patientToPatientDto)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting patient failed. Possible reasons: no such patient exists, or server error. Please try again or read the server log.")
	}

	@Operation(summary = "Get patient by identifier", description = "It gets patient administrative data based on the identifier (root & extension) parameters.")
	@GetMapping("/{hcPartyId}/{id}")
	fun getPatientByHealthcarepartyAndIdentifier(@PathVariable hcPartyId: String, @PathVariable id: String, @RequestParam(required = false) system: String?) = mono {
		when {
			!system.isNullOrEmpty() -> {
				val patient = patientService.findByHcPartyAndIdentifier(hcPartyId, system, id)
					.map { patientV2Mapper.map(it) }

				when (patient.count()) {
					0 -> patientService.getPatient(id)?.let { patientV2Mapper.map(it) }
					else -> patient.first()
				}
			}
			else -> patientService.getPatient(id)?.let { patientV2Mapper.map(it) }
		}
	}

	@Operation(summary = "Create patients in bulk", description = "Returns the id and _rev of created patients")
	@PostMapping("/batch")
	fun createPatients(@RequestBody patientDtos: List<PatientDto>) = mono {
		try {
			val patients = patientService.createPatients(patientDtos.map { p -> patientV2Mapper.map(p) }.toList())
			patients.map { p -> IdWithRevDto(id = p.id, rev = p.rev) }.toList()
		} catch (e: Exception) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
		}
	}

	@Operation(summary = "Modify patients in bulk", description = "Returns the id and _rev of modified patients")
	@PutMapping("/batch")
	fun modifyPatients(@RequestBody patientDtos: List<PatientDto>) = mono {
		try {
			val patients = patientService.modifyPatients(patientDtos.map { p -> patientV2Mapper.map(p) }.toList())
			patients.map { p -> IdWithRevDto(id = p.id, rev = p.rev) }.toList()
		} catch (e: Exception) {
			log.warn(e.message, e)
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
		}
	}

	@Operation(summary = "Modify a patient", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyPatient(@RequestBody patientDto: PatientDto) = mono {
		patientService.modifyPatient(patientV2Mapper.map(patientDto))?.let(patientToPatientDto)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Modifying patient failed. Possible reasons: no such patient exists, or server error. Please try again or read the server log.").also { log.error(it.message) }
	}

	@Operation(summary = "Set a patient referral doctor")
	@PutMapping("/{patientId}/referral/{referralId}")
	fun modifyPatientReferral(
		@PathVariable patientId: String,
		@Parameter(description = "The referal id. Accepts 'none' for referral removal.") @PathVariable referralId: String,
		@Parameter(description = "Optional value for start of referral") @RequestParam(required = false) start: Long?,
		@Parameter(description = "Optional value for end of referral") @RequestParam(required = false) end: Long?
	) = mono {
		patientService.getPatient(patientId)
			?.let {
				patientService.modifyPatientReferral(it, if (referralId == "none") null else referralId, if (start == null) null else Instant.ofEpochMilli(start), if (end == null) null else Instant.ofEpochMilli(end))?.let(patientToPatientDto)
			}
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find patient with ID $patientId in the database").also { log.error(it.message) }
	}

	@Operation(summary = "Provides a paginated list of patients with duplicate ssin for an hecparty")
	@PostMapping("/duplicates/ssin")
	fun findDuplicatesBySsin(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String>>(startKey, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)

		patientService.getDuplicatePatientsBySsin(hcPartyId, paginationOffset).paginatedList(patientToPatientDto, realLimit)
	}

	@Operation(summary = "Provides a paginated list of patients with duplicate name for an hecparty")
	@PostMapping("/duplicates/name")
	fun findDuplicatesByName(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?
	) = mono {
		val realLimit = limit ?: DEFAULT_LIMIT
		val startKeyElements = startKey?.let { objectMapper.readValue<List<String>>(startKey, objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java)) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, realLimit + 1)

		patientService.getDuplicatePatientsByName(hcPartyId, paginationOffset).paginatedList(patientToPatientDto, realLimit)
	}

	@Operation(description = "Shares one or more patients with one or more data owners")
	@PutMapping("/bulkSharedMetadataUpdate")
	fun bulkShare(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<PatientDto>> = flow {
		emitAll(patientService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(description = "Shares one or more patients with one or more data owners but does not return the updated entity.")
	@PutMapping("/bulkSharedMetadataUpdateMinimal")
	fun bulkShareMinimal(
		@RequestBody request: BulkShareOrUpdateMetadataParamsDto
	): Flux<EntityBulkShareResultDto<PatientDto>> = flow {
		emitAll(patientService.bulkShareOrUpdateMetadata(
			entityShareOrMetadataUpdateRequestV2Mapper.map(request)
		).map { bulkShareResultV2Mapper.map(it).copy(updatedEntity = null) })
	}.injectCachedReactorContext(reactorCacheInjector, 50)

	@Operation(
		summary = "Merges two Patient entities (with different ids) which represent the same person into one. " +
			"The metadata of the `from` patient will be merged into the `into` patient, and the `from` patient will " +
			"be soft deleted. The `into` patient content will be updated as requested by the user."
	)
	@PutMapping("/mergeInto/{intoId}/from/{fromId}")
	fun mergePatients(
		@Parameter(description = "The id of the `into` patient")
		@PathVariable
		intoId: String,
		@Parameter(description = "The id of the `from` patient")
		@PathVariable
		fromId: String,
		@Parameter(description = "The expected revision of the `from` patient")
		@RequestParam(required = true)
		expectedFromRev: String,
		@Parameter(description = "The `into` patient with updated content and unchanged metadata. The content is the " +
			"result of the merge of the `from` and `into` patients according to the patient logic. The metadata will" +
			"be automatically merged by this method.")
		@RequestBody
		updatedInto: PatientDto
	): Mono<PatientDto> = mono {
		require(intoId == updatedInto.id) {
			"The id of the `into` patient in the path variable must be the same as the id of the `into` patient in the request body"
		}
		patientV2Mapper.map(patientService.mergePatients(fromId, expectedFromRev, patientV2Mapper.map(updatedInto)))
	}

	companion object {
		private val log = LoggerFactory.getLogger(this::class.java)
		private const val DEFAULT_LIMIT = 1000
	}
}
