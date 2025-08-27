/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.UserService
import org.taktik.icure.cache.ReactorCacheInjector
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsAndRevDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.dto.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PropertyStubDto
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v2.mapper.SecureUserV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.PropertyStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterChainV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.filter.FilterV2Mapper
import org.taktik.icure.services.external.rest.v2.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/* Useful notes:
 * @RequestParam is required by default, but @ApiParam (which is useful to add a description)
 * is not required by default and overrides it, so we have to make sure they always match!
 * Nicknames are required so that operationId is e.g. 'modifyAccessLog' instead of 'modifyAccessLogUsingPUT' */
@RestController("userControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/user")
@Tag(name = "user") // otherwise would default to "user-controller"
class UserController(
	private val userService: UserService,
	private val sessionInfo: SessionInformationProvider,
	private val userV2Mapper: SecureUserV2Mapper,
	private val propertyStubV2Mapper: PropertyStubV2Mapper,
	private val filterChainV2Mapper: FilterChainV2Mapper,
	private val filterV2Mapper: FilterV2Mapper,
	private val reactorCacheInjector: ReactorCacheInjector,
	private val paginationConfig: SharedPaginationConfig,
	private val docIdentifierV2Mapper: DocIdentifierV2Mapper,
	private val objectMapper: ObjectMapper,
) {
	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Get presently logged-in user.", description = "Get current user.")
	@GetMapping(value = ["/current"])
	fun getCurrentUser(
		@RequestParam(required = false, defaultValue = "false") includeMetadataFromGlobalUser: Boolean = false,
	): Mono<UserDto> = mono {
		val user =
			userService.getUser(sessionInfo.getCurrentUserId(), includeMetadataFromGlobalUser)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting Current User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.",
				)
		userV2Mapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "List users with pagination", description = "Returns a list of users.")
	@GetMapping
	fun listUsersBy(
		@Parameter(description = "An user email") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "An user document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Filter out patient users") @RequestParam(required = false) skipPatients: Boolean?,
	): PaginatedFlux<UserDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)

		return userService
			.listUsers(paginationOffset, skipPatients ?: true)
			.mapElements(userV2Mapper::mapOmittingSecrets)
			.asPaginatedFlux()
	}

	@Operation(
		summary = "Create a user",
		description = "Create a user. HealthcareParty ID should be set. Email or Login have to be set. If login hasn't been set, Email will be used for Login instead.",
	)
	@PostMapping
	fun createUser(
		@RequestBody userDto: UserDto,
	): Mono<UserDto> = mono {
		val user =
			userService.createUser(userV2Mapper.mapFillingOmittedSecrets(userDto.copy(groupId = null)))
				?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User creation failed.")
		userV2Mapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get a user by his ID", description = "General information about the user")
	@GetMapping("/{userId}")
	fun getUser(
		@PathVariable userId: String,
		@RequestParam(required = false, defaultValue = "false") includeMetadataFromGlobalUser: Boolean = false,
	): Mono<UserDto> = mono {
		val user =
			userService.getUser(userId, includeMetadataFromGlobalUser)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.",
				)
		userV2Mapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get multiple users by their ids", description = "General information about the user")
	@PostMapping("/byIds")
	fun getUsers(
		@RequestBody userIds: ListOfIdsDto,
	): Flux<UserDto> = userService
		.getUsers(userIds.ids)
		.map { user ->
			userV2Mapper.mapOmittingSecrets(user)
		}.injectReactorContext()

	@Operation(summary = "Get a user by his Email/Login", description = "General information about the user")
	@GetMapping("/byEmail/{email}")
	fun getUserByEmail(
		@PathVariable email: String,
	): Mono<UserDto> = mono {
		val user =
			userService.getUserByEmail(email)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.",
				)
		userV2Mapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get a user by his Phone Number/Login", description = "General information about the user")
	@GetMapping("/byPhoneNumber/{phoneNumber}")
	fun getUserByPhoneNumber(
		@PathVariable phoneNumber: String,
	): Mono<UserDto> = mono {
		val user =
			userService.getUserByPhone(phoneNumber)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.",
				)
		userV2Mapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get the list of User ids by healthcare party id")
	@GetMapping("/byHealthcarePartyId/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun findByHcpartyId(
		@PathVariable id: String,
	): Flux<String> = userService.listUserIdsByHcpartyId(id).injectReactorContext()

	@Operation(summary = "Get the list of User ids by patient id")
	@GetMapping("/byPatientId/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun findByPatientId(
		@PathVariable id: String,
	): Flux<String> = userService.findByPatientId(id).injectReactorContext()

	@Operation(summary = "Deletes an User")
	@DeleteMapping("/{userId}")
	fun deleteUser(
		@PathVariable userId: String,
		@RequestParam(required = false) rev: String? = null,
	): Mono<DocIdentifierDto> = mono {
		userService.deleteUser(userId, rev).let {
			docIdentifierV2Mapper.map(DocIdentifier(it.id, it.rev))
		}
	}

	@PostMapping("/undelete/{userId}")
	fun undeleteUser(
		@PathVariable userId: String,
		@RequestParam(required = true) rev: String,
	): Mono<UserDto> = mono {
		userV2Mapper.mapOmittingSecrets(userService.undeleteUser(userId, rev))
	}

	@DeleteMapping("/purge/{userId}")
	fun purgeUser(
		@PathVariable userId: String,
		@RequestParam(required = true) rev: String,
	): Mono<DocIdentifierDto> = mono {
		userService.purgeUser(userId, rev).let(docIdentifierV2Mapper::map)
	}

	@Operation(summary = "Modify a user.", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyUser(
		@RequestBody userDto: UserDto,
	): Mono<UserDto> = mono {
		// Sanitize group
		val modifiedUser =
			userService.modifyUser(userV2Mapper.mapFillingOmittedSecrets(userDto.copy(groupId = null)))
				?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User modification failed.")

		userV2Mapper.mapOmittingSecrets(modifiedUser)
	}

	@Operation(summary = "Assign a healthcare party ID to current user", description = "UserDto gets returned.")
	@PutMapping("/current/hcparty/{healthcarePartyId}")
	fun assignHealthcareParty(
		@PathVariable healthcarePartyId: String,
		@RequestParam(required = false, defaultValue = "false") includeMetadataFromGlobalUser: Boolean = false,
	): Mono<UserDto> = mono {
		val modifiedUser = userService.getUser(sessionInfo.getCurrentUserId(), includeMetadataFromGlobalUser)
		modifiedUser?.let {
			userService.modifyUser(modifiedUser.copy(healthcarePartyId = healthcarePartyId))

			userV2Mapper.mapOmittingSecrets(modifiedUser)
		}
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Assigning healthcare party ID to the current user failed.").also {
				logger.error(it.message)
			}
	}

	@Operation(
		summary = "Modify a User property",
		description = "Modify a User properties based on his/her ID. The return value is the modified user.",
	)
	@PutMapping("/{userId}/properties")
	fun modifyProperties(
		@PathVariable userId: String,
		@RequestBody properties: List<PropertyStubDto>?,
	): Mono<UserDto> = mono {
		userService
			.setProperties(
				userId,
				properties?.map { p -> propertyStubV2Mapper.map(p) }
					?: listOf(),
			)?.let(userV2Mapper::mapOmittingSecrets)
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Modify a User property failed.")
	}

	@Operation(summary = "Request a new temporary token for authentication")
	@PostMapping("/token/{userId}/{key}")
	fun getToken(
		@PathVariable
		userId: String,
		@Parameter(description = "The token key. Only one instance of a token with a defined key can exist at the same time")
		@PathVariable
		key: String,
		@Parameter(description = "The token validity in seconds", required = false)
		@RequestParam(required = false)
		tokenValidity: Long?,
		@RequestHeader
		token: String? = null,
	): Mono<String> = reactorCacheInjector.monoWithCachedContext(10) {
		// Highly sensitive -> better to cache to speed up access control
		userService.createOrUpdateToken(userId, key, tokenValidity ?: 3600, token)
	}

	@Operation(
		summary = "Filter users for the current user (HcParty)",
		description = "Returns a list of users along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.",
	)
	@PostMapping("/filter")
	fun filterUsersBy(
		@Parameter(description = "A User document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<UserDto>,
	): Mono<PaginatedList<UserDto>> = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val users = userService.filterUsers(paginationOffset, filterChainV2Mapper.tryMap(filterChain).orThrow())

		users.paginatedList(userV2Mapper::mapOmittingSecrets, realLimit, objectMapper = objectMapper)
	}

	@Operation(summary = "Get the ids of the Users matching the provided filter.")
	@PostMapping("/match", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun matchUsersBy(
		@RequestBody filter: AbstractFilterDto<UserDto>,
	): Flux<String> = userService
		.matchUsersBy(
			filter = filterV2Mapper.tryMap(filter).orThrow(),
		).injectReactorContext()
}
