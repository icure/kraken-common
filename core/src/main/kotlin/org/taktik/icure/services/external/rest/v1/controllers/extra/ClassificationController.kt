/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.extra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
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
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.icure.asyncservice.ClassificationService
import org.taktik.icure.services.external.rest.v1.dto.ClassificationDto
import org.taktik.icure.services.external.rest.v1.dto.IcureStubDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.mapper.ClassificationMapper
import org.taktik.icure.services.external.rest.v1.mapper.StubMapper
import org.taktik.icure.services.external.rest.v1.mapper.couchdb.DocIdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.utils.injectReactorContext
import reactor.core.publisher.Flux

@RestController
@Profile("app")
@RequestMapping("/rest/v1/classification")
@Tag(name = "classification")
class ClassificationController(
	private val classificationService: ClassificationService,
	private val classificationMapper: ClassificationMapper,
	private val delegationMapper: DelegationMapper,
	private val stubMapper: StubMapper,
	private val docIdentifierMapper: DocIdentifierMapper,
) {
	@Operation(
		summary = "Create a classification with the current user",
		description = "Returns an instance of created classification Template.",
	)
	@PostMapping
	fun createClassification(
		@RequestBody c: ClassificationDto,
	) = mono {
		classificationMapper.map(
			classificationService.createClassification(classificationMapper.map(c))
		)
	}

	@Operation(summary = "Get a classification Template")
	@GetMapping("/{classificationId}")
	fun getClassification(
		@PathVariable classificationId: String,
	) = mono {
		val element =
			classificationService.getClassification(classificationId)
				?: throw ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Getting classification failed. Possible reasons: no such classification exists, or server error. Please try again or read the server log.",
				)

		classificationMapper.map(element)
	}

	@Operation(summary = "Get a list of classifications", description = "Ids are seperated by a coma")
	@GetMapping("/byIds/{ids}")
	fun getClassificationByHcPartyId(
		@PathVariable ids: String,
	): Flux<ClassificationDto> {
		val elements = classificationService.getClassifications(ids.split(','))

		return elements.map { classificationMapper.map(it) }.injectReactorContext()
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listClassificationIdsByDataOwnerPatientCreated instead")
	@Operation(
		summary = "List classification Templates found By Healthcare Party and secret foreign key elementIds.",
		description = "Keys have to be delimited by comma",
	)
	@GetMapping("/byHcPartySecretForeignKeys")
	fun findClassificationsByHCPartyPatientForeignKeys(
		@RequestParam hcPartyId: String,
		@RequestParam secretFKeys: String,
	): Flux<ClassificationDto> {
		val secretPatientKeys = secretFKeys.split(',').map { it.trim() }
		val elementList = classificationService.listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId, secretPatientKeys)

		return elementList.map { classificationMapper.map(it) }.injectReactorContext()
	}

	@Operation(
		summary = "Delete classification Templates.",
		description = "Response is a set containing the ID's of deleted classification Templates.",
	)
	@DeleteMapping("/{classificationIds}")
	fun deleteClassifications(
		@PathVariable classificationIds: String,
	): Flux<DocIdentifierDto> {
		val ids = classificationIds.split(',')
		if (ids.isEmpty()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A required query parameter was not specified for this request.")
		}

		return classificationService
			.deleteClassifications(ids.toSet().map { IdAndRev(it, null) })
			.map { docIdentifierMapper.map(DocIdentifier(it.id, it.rev)) }
			.injectReactorContext()
	}

	@Operation(summary = "Modify a classification Template", description = "Returns the modified classification Template.")
	@PutMapping
	fun modifyClassification(
		@RequestBody classificationDto: ClassificationDto,
	) = mono {
		classificationService
			.modifyClassification(classificationMapper.map(classificationDto))
			?.let { classificationMapper.map(it) }
			?: throw DocumentNotFoundException("Classification modification failed.")
	}

	@Operation(
		summary = "Delegates a classification to a healthcare party",
		description = "It delegates a classification to a healthcare party (By current healthcare party). Returns the element with new delegations.",
	)
	@PostMapping("/{classificationId}/delegate")
	fun newClassificationDelegations(
		@PathVariable classificationId: String,
		@RequestBody ds: List<DelegationDto>,
	) = mono {
		classificationService.addDelegations(classificationId, ds.map { delegationMapper.map(it) })
		val classificationWithDelegation = classificationService.getClassification(classificationId)

		val succeed = classificationWithDelegation?.delegations != null && classificationWithDelegation.delegations.isNotEmpty()
		if (succeed) {
			classificationWithDelegation?.let { classificationMapper.map(it) }
		} else {
			throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delegation creation for classification failed.")
		}
	}

	@Operation(summary = "Update delegations in classification", description = "Keys must be delimited by coma")
	@PostMapping("/delegations")
	fun setClassificationsDelegations(
		@RequestBody stubs: List<IcureStubDto>,
	) = flow {
		val classifications =
			classificationService.getClassifications(stubs.map { it.id }).map { classification ->
				stubs.find { s -> s.id == classification.id }?.let { stub ->
					classification.copy(
						delegations =
						classification.delegations.mapValues { (s, dels) -> stub.delegations[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
							stub.delegations
								.filterKeys { k ->
									!classification.delegations.containsKey(k)
								}.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
						encryptionKeys =
						classification.encryptionKeys.mapValues { (s, dels) -> stub.encryptionKeys[s]?.map { delegationMapper.map(it) }?.toSet() ?: dels } +
							stub.encryptionKeys
								.filterKeys { k ->
									!classification.encryptionKeys.containsKey(k)
								}.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
						cryptedForeignKeys =
						classification.cryptedForeignKeys.mapValues { (s, dels) ->
							stub.cryptedForeignKeys[s]?.map { delegationMapper.map(it) }?.toSet()
								?: dels
						} +
							stub.cryptedForeignKeys
								.filterKeys { k ->
									!classification.cryptedForeignKeys.containsKey(k)
								}.mapValues { (_, value) -> value.map { delegationMapper.map(it) }.toSet() },
					)
				} ?: classification
			}
		emitAll(classificationService.modifyEntities(classifications.toList()).map { stubMapper.mapToStub(it) })
	}.injectReactorContext()
}
