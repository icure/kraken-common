/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Splitter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.PatientLogic.Companion.PatientSearchField
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.AccessLogService
import org.taktik.icure.asyncservice.HealthcarePartyService
import org.taktik.icure.asyncservice.PatientService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.SortDirection
import org.taktik.icure.db.Sorting
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.security.warn
import org.taktik.icure.services.external.rest.v1.dto.IdWithRevDto
import org.taktik.icure.services.external.rest.v1.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v1.dto.PaginatedDocumentKeyIdPair
import org.taktik.icure.services.external.rest.v1.dto.PaginatedList
import org.taktik.icure.services.external.rest.v1.dto.PatientDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.SortDirectionDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.PatientMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PatientHealthCarePartyMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.FluxString
import org.taktik.icure.utils.JsonString
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import javax.security.auth.login.LoginException

@RestController
@Profile("app")
@RequestMapping("/rest/v1/patient")
@Tag(name = "patient")
class PatientController(
	private val sessionLogic: SessionInformationProvider,
	private val accessLogService: AccessLogService,
	private val patientService: PatientService,
	private val healthcarePartyService: HealthcarePartyService,
	private val patientMapper: PatientMapper,
	private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
	private val addressMapper: AddressMapper,
	private val patientHealthCarePartyMapper: PatientHealthCarePartyMapper,
	private val delegationMapper: DelegationMapper,
	private val objectMapper: ObjectMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
	private val paginationConfig: SharedPaginationConfig,
) {
	@Operation(
		summary = "Find patients for the current healthcare party",
		description =
		"Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " +
			"Null it means that this is the last page.",
	)
	@GetMapping("/byNameBirthSsinAuto")
	fun findByNameBirthSsinAuto(
		@Parameter(description = "HealthcareParty Id. If not set, will use user's hcpId") @RequestParam(required = false) healthcarePartyId: String?,
		@Parameter(description = "Optional value for filtering results") @RequestParam(required = true) filterValue: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(
			description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.",
		) @RequestParam(required = false, defaultValue = "asc") sortDirection: SortDirectionDto,
	): PaginatedFlux<PatientDto> = flow {
		require(filterValue.length >= 2) { "The filterValue parameter should have at least 2 characters" }

		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		val currentHcpId = healthcarePartyId ?: sessionLogic.getCurrentDataOwnerId()
		val hcp = healthcarePartyService.getHealthcareParty(currentHcpId)

		emitAll(
			(hcp?.parentId?.takeIf { it.isNotEmpty() } ?: hcp?.id)?.let { hcpId ->
				patientService.findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
					hcpId,
					paginationOffset,
					filterValue,
					Sorting(PatientSearchField.patientName, SortDirection.valueOf(sortDirection.name)),
				)
			} ?: emptyFlow(),
		)
	}.mapElements(patientMapper::map).asPaginatedFlux()

	@Operation(
		summary = "List patients of a specific HcParty or of the current HcParty ",
		description =
		"Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " +
			"Null it means that this is the last page.",
	)
	@GetMapping("/ofHcParty/{hcPartyId}")
	fun listPatientsOfHcParty(
		@PathVariable hcPartyId: String,
		@Parameter(
			description =
			"Optional value for sorting results by a given field ('name', 'ssin', 'dateOfBirth'). " + "Specifying this deactivates filtering",
		) @RequestParam(required = false, defaultValue = "name") sortField: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(
			description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.",
		) @RequestParam(required = false, defaultValue = "asc") sortDirection: SortDirectionDto,
	): PaginatedFlux<PatientDto> {
		val sortFieldAsEnum = PatientSearchField.lenientValueOf(sortField)
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return patientService
			.findOfHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
				hcPartyId,
				paginationOffset,
				null,
				Sorting(sortFieldAsEnum, SortDirection.valueOf(sortDirection.name)),
			).mapElements(patientMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "List patients that have been merged towards another patient ",
		description = "Returns a list of patients that have been merged after the provided date",
	)
	@GetMapping("/merges/{date}")
	fun listOfMergesAfter(
		@PathVariable date: Long,
	) = patientService.listOfMergesAfter(date).map { patientMapper.map(it) }.injectReactorContext()

	@Operation(
		summary = "List patients that have been modified after the provided date",
		description = "Returns a list of patients that have been modified after the provided date",
	)
	@GetMapping("/modifiedAfter/{date}")
	fun listOfPatientsModifiedAfter(
		@PathVariable date: Long,
		@Parameter(
			description = "The start key for pagination the date of the first element of the new page",
		) @RequestParam(required = false) startKey: Long?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<PatientDto> {
		val offset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return patientService
			.listOfPatientsModifiedAfter(date, offset)
			.mapElements(patientMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "List patients for a specific HcParty or for the current HcParty ",
		description =
		"Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " +
			"Null it means that this is the last page.",
	)
	@GetMapping("/hcParty/{hcPartyId}")
	fun listPatientsByHcParty(
		@PathVariable hcPartyId: String,
		@Parameter(
			description =
			"Optional value for sorting results by a given field ('name', 'ssin', 'dateOfBirth'). " + "Specifying this deactivates filtering",
		) @RequestParam(required = false, defaultValue = "name") sortField: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(
			description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.",
		) @RequestParam(required = false, defaultValue = "asc") sortDirection: SortDirectionDto,
	) = findPatientsByHealthcareParty(hcPartyId, sortField, startKey, startDocumentId, limit, sortDirection)

	@Operation(
		summary = "Get the patient (identified by patientId) hcparty keys. Those keys are AES keys (encrypted) used to share information between HCPs and a patient.",
		description = """This endpoint is used to recover all keys that have already been created and that can be used to share information with this patient. It returns a map with the following structure: ID of the owner of the encrypted AES key -> encrypted AES key. The returned encrypted AES keys will have to be decrypted using the patient's private key.

                {
                    "hcparty 1 delegator ID": "AES hcparty key (encrypted using patient public RSA key)"
                    "hcparty 2 delegator ID": "other AES hcparty key (encrypted using patient public RSA key)"
                }
                """,
		deprecated = true,
		responses = [
			ApiResponse(responseCode = "200", description = "Successful operation"),
			ApiResponse(
				responseCode = "401",
				description = "Unauthorized operation: the provided credentials are invalid",
				content = [],
			),
		],
	)
	@GetMapping("/{patientId}/keys")
	@Suppress("DEPRECATION")
	fun getPatientHcPartyKeysForDelegate(
		@Parameter(description = "The patient Id for which information is shared") @PathVariable patientId: String,
	) = mono {
		patientService.getHcPartyKeysForDelegate(patientId)
	}

	@Operation(
		summary = "Get the HcParty encrypted AES keys indexed by owner.",
		description = "(key, value) of the map is as follows: (ID of the owner of the encrypted AES key, encrypted AES keys)",
	)
	@GetMapping("/{patientId}/aesExchangeKeys")
	fun getPatientAesExchangeKeysForDelegate(
		@PathVariable patientId: String,
	) = mono {
		patientService.getAesExchangeKeysForDelegate(patientId)
	}

	@Operation(
		summary = "Get count of patients for a specific HcParty or for the current HcParty ",
		description = "Returns the count of patients",
	)
	@GetMapping("/hcParty/{hcPartyId}/count")
	fun countOfPatients(
		@Parameter(description = "Healthcare party id") @PathVariable hcPartyId: String,
	) = mono {
		ContentDto(numberValue = patientService.countByHcParty(hcPartyId).toDouble())
	}

	@Operation(
		summary = "List patients for a specific HcParty",
		description =
		"Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " +
			"Null it means that this is the last page.",
	)
	@GetMapping
	fun findPatientsByHealthcareParty(
		@Parameter(description = "Healthcare party id") @RequestParam(required = false) hcPartyId: String?,
		@Parameter(
			description = "Optional value for sorting results by a given field ('name', 'ssin', 'dateOfBirth').",
		) @RequestParam(required = false, defaultValue = "name") sortField: String,
		@Parameter(
			description =
			"The start key for pagination: a JSON representation of an array containing all the necessary " +
				"components to form the Complex Key's startKey",
		) @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(
			description = "Optional value for providing a sorting direction ('asc', 'desc'). Set to 'asc' by default.",
		) @RequestParam(required = false, defaultValue = "asc") sortDirection: SortDirectionDto,
	): PaginatedFlux<PatientDto> = flow {
		val sortFieldAsEnum = PatientSearchField.lenientValueOf(sortField)
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		val currentHcpId = hcPartyId ?: sessionLogic.getCurrentDataOwnerId()
		val hcp = healthcarePartyService.getHealthcareParty(currentHcpId)
		emitAll(
			(hcp?.parentId?.takeIf { it.isNotEmpty() } ?: hcp?.id)?.let { hcpId ->
				patientService.findByHcPartyAndSsinOrDateOfBirthOrNameContainsFuzzy(
					hcpId,
					paginationOffset,
					null,
					Sorting(sortFieldAsEnum, SortDirection.valueOf(sortDirection.name)),
				)
			} ?: emptyFlow(),
		)
	}.mapElements(patientMapper::map).asPaginatedFlux()

	@Operation(
		summary = "List patients by pages for a specific HcParty",
		description =
		"Returns a list of patients along with next start keys and Document ID. If the nextStartKey is " +
			"Null it means that this is the last page.",
	)
	@GetMapping("/idsPages")
	fun listPatientsIds(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The page first id") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Page size") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<FluxString> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return patientService
			.findByHcPartyIdsOnly(hcPartyId, paginationOffset)
			.asPaginatedFlux()
	}

	@Operation(summary = "Get the patient having the provided externalId")
	@GetMapping("/byExternalId/{externalId}")
	fun findByExternalId(
		@PathVariable("externalId")
		@Parameter(description = "A external ID") externalId: String,
	) = mono {
		patientService.getByExternalId(externalId)?.let(patientMapper::map)
	}

	@Operation(summary = "Get Paginated List of Patients sorted by Access logs descending")
	@GetMapping("/byAccess/{userId}")
	fun findByAccessLogUserAfterDate(
		@Parameter(description = "A User ID") @PathVariable userId: String,
		@Parameter(description = "The type of access (COMPUTER or USER)") @RequestParam(required = false) accessType: String?,
		@Parameter(description = "The start search epoch") @RequestParam(required = false) startDate: Long?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): Mono<PaginatedList<PatientDto>> = mono {
		accessLogService
			.aggregatePatientByAccessLogs(
				userId,
				accessType,
				startDate,
				startKey,
				startDocumentId,
				limit ?: paginationConfig.defaultLimit,
			).let { (_, _, patients, dateNextKey, nextDocumentId) ->
				val patientDtos =
					patients.map { patient ->
						PatientDto(
							id = patient.id,
							lastName = patient.lastName,
							firstName = patient.firstName,
							partnerName = patient.partnerName,
							maidenName = patient.maidenName,
							dateOfBirth = patient.dateOfBirth,
							ssin = patient.ssin,
							externalId = patient.externalId,
							patientHealthCareParties =
							patient.patientHealthCareParties.map { phcp ->
								patientHealthCarePartyMapper.map(
									phcp,
								)
							},
							addresses = patient.addresses.map { addressMapper.map(it) },
						)
					}

				PaginatedList(
					nextKeyPair =
					dateNextKey?.let {
						PaginatedDocumentKeyIdPair(
							it,
							nextDocumentId,
						)
					},
					rows = patientDtos.toList(),
				)
			}
	}

	@Operation(
		summary = "Filter patients for the current user (HcParty) ",
		description = "Returns a list of patients along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterPatientsBy(
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Skip rows") @RequestParam(required = false) skip: Int?,
		@Parameter(description = "Sort key") @RequestParam(required = false) sort: String?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@RequestBody filterChain: FilterChain<PatientDto>,
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val startKeyList =
			startKey
				?.takeIf {
					it.isNotEmpty()
				}?.let {
					ArrayList(
						Splitter
							.on(",")
							.omitEmptyStrings()
							.trimResults()
							.splitToList(it),
					)
				}
		val paginationOffset = PaginationOffset<List<String>>(startKeyList, startDocumentId, skip, realLimit + 1)

		try {
			val patients = patientService.listPatients(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow(), sort, desc)
			log.info("Filter patients in " + (System.currentTimeMillis() - System.currentTimeMillis()) + " ms.")

			patients.paginatedList(patientMapper::map, realLimit)
		} catch (e: LoginException) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}

	@Operation(summary = "Get the ids of the Patients matching the provided filter.")
	@PostMapping("/match", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun matchPatientsBy(
		@RequestBody filter: AbstractFilterDto<PatientDto>,
	) = patientService
		.matchPatientsBy(
			filter = filterMapper.tryMap(filter).orThrow(),
		).injectReactorContext()

	@Operation(summary = "Filter patients for the current user (HcParty) ", description = "Returns a list of patients")
	@GetMapping("/fuzzy")
	fun fuzzySearch(
		@Parameter(description = "The first name") @RequestParam(required = true) firstName: String,
		@Parameter(description = "The last name") @RequestParam(required = true) lastName: String,
		@Parameter(description = "The date of birth") @RequestParam(required = false) dateOfBirth: Int?,
	): Flux<PatientDto> = flow {
		try {
			emitAll(
				patientService
					.fuzzySearchPatients(firstName, lastName, dateOfBirth)
					.map { patientMapper.map(it) },
			)
		} catch (e: Exception) {
			log.warn(e) { e.message }
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
		}
	}.injectReactorContext()

	@Operation(
		summary = "Create a patient",
		description = "Name, last name, date of birth, and gender are required. After creation of the patient and obtaining the ID, you need to create an initial delegation.",
	)
	@PostMapping
	fun createPatient(
		@RequestBody p: PatientDto,
	) = mono {
		val patient = patientService.createPatient(patientMapper.map(p))
		patient?.let(patientMapper::map) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Patient creation failed.")
	}

	@Operation(summary = "Delete patients.", description = "Response is an array containing the ID of deleted patient..")
	@DeleteMapping("/{patientIds}")
	fun deletePatient(
		@PathVariable patientIds: String,
	): Flux<DocIdentifierDto> {
		val ids = patientIds.split(',')
		if (ids.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		return patientService
			.deletePatients(ids.toSet().map { IdAndRev(it, null) })
			.map { docIdentifierMapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}

	@Operation(
		summary = "Find deleted patients",
		description = "Returns a list of deleted patients, within the specified time period, if any.",
	)
	@GetMapping("/deleted/by_date")
	fun listDeletedPatients(
		@Parameter(description = "Filter deletions after this date (unix epoch), included") @RequestParam(required = true) startDate: Long,
		@Parameter(description = "Filter deletions before this date (unix epoch), included") @RequestParam(required = false) endDate: Long?,
		@Parameter(description = "Descending") @RequestParam(required = false) desc: Boolean?,
		@Parameter(description = "The start key for pagination") @RequestParam(required = false) startKey: Long?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<PatientDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return patientService
			.findDeletedPatientsByDeleteDate(startDate, endDate, desc ?: false, paginationOffset)
			.mapElements(patientMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Find deleted patients", description = "Returns a list of deleted patients, by name and/or firstname prefix, if any.")
	@GetMapping("/deleted/by_name")
	fun listDeletedPatientsByName(
		@Parameter(description = "First name prefix") @RequestParam(required = false) firstName: String?,
		@Parameter(description = "Last name prefix") @RequestParam(required = false) lastName: String?,
	) = patientService.listDeletedPatientsByNames(firstName, lastName).map { patientMapper.map(it) }.injectReactorContext()

	@Operation(summary = "undelete previously deleted patients", description = "Response is an array containing the ID of undeleted patient..")
	@PutMapping("/undelete/{patientIds}")
	fun undeletePatient(
		@PathVariable patientIds: String,
	): Flux<DocIdentifierDto> {
		val ids = patientIds.split(',')
		if (ids.isEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		return patientService
			.undeletePatients(ids.toSet().map { IdAndRev(it, null) })
			.map { DocIdentifierDto(it.id, it.rev) }
			.injectReactorContext()
	}

	@Operation(
		summary = "Delegates a patients to a healthcare party",
		description = "It delegates a patient to a healthcare party (By current healthcare party). A modified patient with new delegation gets returned.",
	)
	@PostMapping("/{patientId}/delegate")
	fun newPatientDelegations(
		@PathVariable patientId: String,
		@RequestBody ds: List<DelegationDto>,
	) = mono {
		patientService.addDelegations(patientId, ds.map { d -> delegationMapper.map(d) })
		val patientWithDelegations = patientService.getPatient(patientId)

		patientWithDelegations?.takeIf { it.delegations.isNotEmpty() }?.let(patientMapper::map)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred in creation of the delegation.")
	}

	@Operation(summary = "Get patients by id", description = "It gets patient administrative data.")
	@PostMapping("/byIds")
	fun getPatients(
		@RequestBody patientIds: ListOfIdsDto,
	): Flux<PatientDto> = patientService
		.getPatients(patientIds.ids)
		.map { patientMapper.map(it) }
		.injectReactorContext()

	@Operation(summary = "Get patient", description = "It gets patient administrative data.")
	@GetMapping("/{patientId}")
	fun getPatient(
		@PathVariable patientId: String,
	) = mono {
		patientService.getPatient(patientId)?.let(patientMapper::map)
			?: throw ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Getting patient failed. Possible reasons: no such patient exists, or server error. Please try again or read the server log.",
			)
	}

	@Operation(
		summary = "Get patient by identifier",
		description = "It gets patient administrative data based on the identifier (root & extension) parameters.",
	)
	@GetMapping("/{hcPartyId}/{id}")
	fun getPatientByHealthcarePartyAndIdentifier(
		@PathVariable hcPartyId: String,
		@PathVariable id: String,
		@RequestParam(required = false) system: String?,
	) = mono {
		when {
			!system.isNullOrEmpty() -> {
				patientService
					.findByHcPartyAndIdentifier(hcPartyId, system, id)
					.map { patientMapper.map(it) }
					.firstOrNull() ?: patientService.getPatient(id)?.let { patientMapper.map(it) }
			}
			else -> patientService.getPatient(id)?.let { patientMapper.map(it) }
		}
	}

	@Operation(summary = "Create patients in bulk", description = "Returns the id and _rev of created patients")
	@PostMapping("/bulk", "/batch")
	fun bulkCreatePatients(
		@RequestBody patientDtos: List<PatientDto>,
	) = flow {
		val patients = patientService.createPatients(patientDtos.map { p -> patientMapper.map(p) }.toList())
		emitAll(patients.map { p -> IdWithRevDto(id = p.id, rev = p.rev) })
	}.injectReactorContext()

	@Operation(summary = "Modify patients in bulk", description = "Returns the id and _rev of modified patients")
	@PutMapping("/bulk", "/batch")
	fun bulkUpdatePatients(
		@RequestBody patientDtos: List<PatientDto>,
	) = flow {
		val patients = patientService.modifyPatients(patientDtos.map { p -> patientMapper.map(p) }.toList())
		emitAll(patients.map { p -> IdWithRevDto(id = p.id, rev = p.rev) })
	}.injectReactorContext()

	@Operation(summary = "Modify a patient", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyPatient(
		@RequestBody patientDto: PatientDto,
	) = mono {
		patientService.modifyPatient(patientMapper.map(patientDto))?.let(patientMapper::map)
			?: throw ResponseStatusException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Getting patient failed. Possible reasons: no such patient exists, or server error. Please try again or read the server log.",
			).also {
				log.error(it.message)
			}
	}

	@Operation(summary = "Set a patient referral doctor")
	@PutMapping("/{patientId}/referral/{referralId}")
	fun modifyPatientReferral(
		@PathVariable patientId: String,
		@Parameter(description = "The referral id. Accepts 'none' for referral removal.") @PathVariable referralId: String,
		@Parameter(description = "Optional value for start of referral") @RequestParam(required = false) start: Long?,
		@Parameter(description = "Optional value for end of referral") @RequestParam(required = false) end: Long?,
	) = mono {
		patientService.getPatient(patientId)?.let {
			patientService
				.modifyPatientReferral(
					it,
					if (referralId == "none") null else referralId,
					if (start == null) null else Instant.ofEpochMilli(start),
					if (end == null) null else Instant.ofEpochMilli(end),
				)?.let(patientMapper::map)
		}
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find patient with ID $patientId in the database").also {
				log.error(it.message)
			}
	}

	@Operation(summary = "Provides a paginated list of patients with duplicate ssin for an healthcare party")
	@PostMapping("/duplicates/ssin")
	fun findDuplicatesBySsin(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<PatientDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(startKey) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return patientService
			.getDuplicatePatientsBySsin(hcPartyId, paginationOffset)
			.mapElements(patientMapper::map)
			.asPaginatedFlux()
	}

	@Operation(summary = "Provides a paginated list of patients with duplicate name for an healthcare party")
	@PostMapping("/duplicates/name")
	fun findDuplicatesByName(
		@Parameter(description = "Healthcare party id") @RequestParam hcPartyId: String,
		@Parameter(description = "The start key for pagination, depends on the filters used") @RequestParam(required = false) startKey: JsonString?,
		@Parameter(description = "A patient document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
	): PaginatedFlux<PatientDto> {
		val startKeyElements = startKey?.let { objectMapper.readValue<ComplexKey>(it) }
		val paginationOffset = PaginationOffset(startKeyElements, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return patientService
			.getDuplicatePatientsByName(hcPartyId, paginationOffset)
			.mapElements(patientMapper::map)
			.asPaginatedFlux()
	}

	@Operation(
		summary =
		"Merges two Patient entities (with different ids) which represent the same person into one. " +
			"The metadata of the `from` patient will be merged into the `into` patient, and the `from` patient will " +
			"be soft deleted. The `into` patient content will be updated as requested by the user.",
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
		@Parameter(
			description =
			"The `into` patient with updated content and unchanged metadata. The content is the " +
				"result of the merge of the `from` and `into` patients according to the patient logic. The metadata will" +
				"be automatically merged by this method.",
		)
		@RequestBody
		updatedInto: PatientDto,
	): Mono<PatientDto> = mono {
		require(intoId == updatedInto.id) {
			"The id of the `into` patient in the path variable must be the same as the id of the `into` patient in the request body"
		}
		patientMapper.map(patientService.mergePatients(fromId, expectedFromRev, patientMapper.map(updatedInto), true))
	}

	companion object {
		private val log = LoggerFactory.getLogger(this::class.java)
	}
}
