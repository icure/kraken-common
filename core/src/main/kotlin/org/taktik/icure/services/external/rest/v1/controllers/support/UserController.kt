/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
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
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asyncservice.UserService
import org.taktik.icure.config.SharedPaginationConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.pagination.PaginatedFlux
import org.taktik.icure.pagination.asPaginatedFlux
import org.taktik.icure.pagination.mapElements
import org.taktik.icure.services.external.rest.v1.dto.PropertyStubDto
import org.taktik.icure.services.external.rest.v1.dto.UserDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.services.external.rest.v1.mapper.SecureUserV1Mapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterChainMapper
import org.taktik.icure.services.external.rest.v1.mapper.filter.FilterMapper
import org.taktik.icure.services.external.rest.v1.utils.paginatedList
import org.taktik.icure.utils.injectReactorContext
import org.taktik.icure.utils.orThrow
import org.taktik.icure.utils.warn

/* Useful notes:
 * @RequestParam is required by default, but @ApiParam (which is useful to add a description)
 * is not required by default and overrides it, so we have to make sure they always match!
 * Nicknames are required so that operationId is e.g. 'modifyAccessLog' instead of 'modifyAccessLogUsingPUT' */
@RestController
@Profile("app")
@RequestMapping("/rest/v1/user")
@Tag(name = "user") // otherwise would default to "user-controller"
class UserController (
	private val userService: UserService,
	private val sessionInfo: SessionInformationProvider,
	private val userMapper: SecureUserV1Mapper,
	private val propertyStubMapper: PropertyStubMapper,
	private val filterChainMapper: FilterChainMapper,
	private val filterMapper: FilterMapper,
	private val paginationConfig: SharedPaginationConfig
) {

	companion object {
		private val logger = LoggerFactory.getLogger(this::class.java)
	}

	@Operation(summary = "Get presently logged-in user.", description = "Get current user.")
	@GetMapping(value = ["/current"])
	fun getCurrentUser() = mono {
		val user = userService.getUser(sessionInfo.getCurrentUserId())
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting Current User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.")
		logger.warn { "Current user is ${user.id}" }
		userMapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "List users with pagination", description = "Returns a list of users.")
	@GetMapping
	fun listUsers(
		@Parameter(description = "An user email") @RequestParam(required = false) startKey: String?,
		@Parameter(description = "An user document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@Parameter(description = "Filter out patient users") @RequestParam(required = false) skipPatients: Boolean?,
	): PaginatedFlux<UserDto> {
		val paginationOffset = PaginationOffset(startKey, startDocumentId, null, limit ?: paginationConfig.defaultLimit)
		return userService
			.listUsers(paginationOffset, skipPatients ?: true)
			.mapElements(userMapper::mapOmittingSecrets)
			.asPaginatedFlux()
	}

	@Operation(summary = "Create a user", description = "Create a user. HealthcareParty ID should be set. Email or Login have to be set. If login hasn't been set, Email will be used for Login instead.")
	@PostMapping
	fun createUser(@RequestBody userDto: UserDto) = mono {
		val user = userService.createUser(userMapper.mapFillingOmittedSecrets(userDto.copy(groupId = null)))
			?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User creation failed.")
		userMapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get a user by his ID", description = "General information about the user")
	@GetMapping("/{userId}")
	fun getUser(@PathVariable userId: String) = mono {
		val user = userService.getUser(userId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.")
		userMapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get a user by his Email/Login", description = "General information about the user")
	@GetMapping("/byEmail/{email}")
	fun getUserByEmail(@PathVariable email: String) = mono {
		val user = userService.getUserByEmail(email)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.")
		userMapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get a user by his Phone Number/Login", description = "General information about the user")
	@GetMapping("/byPhoneNumber/{phoneNumber}")
	fun getUserByPhoneNumber(@PathVariable phoneNumber: String) = mono {
		val user = userService.getUserByPhone(phoneNumber)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Getting User failed. Possible reasons: no such user exists, or server error. Please try again or read the server log.")
			userMapper.mapOmittingSecrets(user)
	}

	@Operation(summary = "Get the list of users by healthcare party id")
	@GetMapping("/byHealthcarePartyId/{id}")
	fun findByHcpartyId(@PathVariable id: String) = mono {
		userService.listUserIdsByHcpartyId(id).toList()
	}

	@Operation(summary = "Get the list of users by patient id")
	@GetMapping("/byPatientId/{id}")
	fun findByPatientId(@PathVariable id: String) = mono {
		userService.findByPatientId(id).toList()
	}

	@Operation(summary = "Delete a User based on his/her ID.", description = "Delete a User based on his/her ID. The return value is an array containing the ID of deleted user.")
	@DeleteMapping("/{userId}")
	fun deleteUser(@PathVariable userId: String) = mono {
		userService.deleteUser(userId)
	}

	@Operation(summary = "Modify a user.", description = "No particular return value. It's just a message.")
	@PutMapping
	fun modifyUser(@RequestBody userDto: UserDto) = mono {
		//Sanitize group
		val modifiedUser = userService.modifyUser(userMapper.mapFillingOmittedSecrets(userDto.copy(groupId = null)))
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User modification failed.")

		userMapper.mapOmittingSecrets(modifiedUser)
	}

	@Operation(summary = "Assign a healthcare party ID to current user", description = "UserDto gets returned.")
	@PutMapping("/current/hcparty/{healthcarePartyId}")
	fun assignHealthcareParty(@PathVariable healthcarePartyId: String) = mono {
		val modifiedUser = userService.getUser(sessionInfo.getCurrentUserId())
		modifiedUser?.let {
			userService.modifyUser(modifiedUser.copy(healthcarePartyId = healthcarePartyId))

			userMapper.mapOmittingSecrets(modifiedUser)
		} ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Assigning healthcare party ID to the current user failed.").also { logger.error(it.message) }
	}

	@Operation(summary = "Modify a User property", description = "Modify a User properties based on his/her ID. The return value is the modified user.")
	@PutMapping("/{userId}/properties")
	fun modifyProperties(@PathVariable userId: String, @RequestBody properties: List<PropertyStubDto>?) = mono {
		userService.setProperties(
			userId,
			properties?.map { p -> propertyStubMapper.map(p) } ?: listOf()
		)?.let(userMapper::mapOmittingSecrets) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Modify a User property failed.")
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
		token: String? = null
	) = mono {
		userService.createOrUpdateToken(userId, key, tokenValidity ?: 3600, token)
	}

	@Operation(summary = "Filter users for the current user (HcParty)", description = "Returns a list of users along with next start keys and Document ID. If the nextStartKey is Null it means that this is the last page.")
	@PostMapping("/filter")
	fun filterUsersBy(
		@Parameter(description = "A User document ID") @RequestParam(required = false) startDocumentId: String?,
		@Parameter(description = "Number of rows") @RequestParam(required = false) limit: Int?,
		@RequestBody filterChain: FilterChain<UserDto>
	) = mono {
		val realLimit = limit ?: paginationConfig.defaultLimit
		val paginationOffset = PaginationOffset(null, startDocumentId, null, realLimit + 1)
		val users = userService.filterUsers(paginationOffset, filterChainMapper.tryMap(filterChain).orThrow())

		users.paginatedList(userMapper::mapOmittingSecrets, realLimit)
	}

	@Operation(summary = "Get the ids of the Users matching the provided filter.")
	@PostMapping("/match", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun matchUsersBy(
		@RequestBody filter: AbstractFilterDto<UserDto>
	) =
		userService.matchUsersBy(
			filter = filterMapper.tryMap(filter).orThrow()
		).injectReactorContext()
}
