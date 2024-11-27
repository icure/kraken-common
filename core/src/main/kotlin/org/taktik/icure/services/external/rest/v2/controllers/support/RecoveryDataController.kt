package org.taktik.icure.services.external.rest.v2.controllers.support

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asyncservice.RecoveryDataService
import org.taktik.icure.entities.RecoveryData
import org.taktik.icure.services.external.rest.v2.dto.RecoveryDataDto
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v2.mapper.RecoveryDataV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.couchdb.DocIdentifierV2Mapper
import reactor.core.publisher.Mono

@RestController("recoveryDataControllerV2")
@Profile("app")
@RequestMapping("/rest/v2/recoverydata")
@Tag(name = "recoveryData")
class RecoveryDataController(
    private val recoveryDataService: RecoveryDataService,
    private val recoveryDataV2Mapper: RecoveryDataV2Mapper,
    private val docIdentifierMapper: DocIdentifierV2Mapper
) {
    @PostMapping
    fun createRecoveryData(
        @RequestBody(required = true) recoveryData: RecoveryDataDto
    ): Mono<RecoveryDataDto> = mono {
        recoveryDataV2Mapper.map(recoveryDataService.createRecoveryData(recoveryDataV2Mapper.map(recoveryData)))
    }

    @GetMapping("/{id}")
    fun getRecoveryData(
        @PathVariable id: String
    ): Mono<RecoveryDataDto> = mono {
        recoveryDataV2Mapper.map(
            recoveryDataService.getRecoveryData(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "RecoveryData not found")
        )
    }

    @DeleteMapping("/{id}")
    fun deleteRecoveryData(
        @PathVariable id: String
    ): Mono<DocIdentifierDto> = mono {
        recoveryDataService.purgeRecoveryData(id).let(docIdentifierMapper::map)
    }

    @DeleteMapping("/forRecipient/{recipientId}")
    fun deleteAllRecoveryDataForRecipient(
        @PathVariable recipientId: String
    ): Mono<ContentDto> = mono {
        ContentDto(numberValue = recoveryDataService.deleteAllRecoveryDataForRecipient(recipientId).toDouble())
    }

    @DeleteMapping("/forRecipient/{recipientId}/ofType/{type}")
    fun deleteAllRecoveryDataOfTypeForRecipient(
        @PathVariable type: RecoveryDataDto.Type,
        @PathVariable recipientId: String
    ): Mono<ContentDto> = mono {
        ContentDto(
            numberValue = recoveryDataService.deleteAllRecoveryDataOfTypeForRecipient(
                RecoveryData.Type.valueOf(type.name),
                recipientId
            ).toDouble()
        )
    }

    @GetMapping("/waiting/{id}")
    fun getRecoveryDataWaiting(
        @PathVariable id: String,
        @RequestParam(required = false) timeoutSeconds: Int? = null
    ): Mono<RecoveryDataDto> = mono {
        require(timeoutSeconds == null || timeoutSeconds <= 30) { "Max wait time is 30 seconds" }
        recoveryDataService.waitForRecoveryData(id, timeoutSeconds ?: 30)?.let(recoveryDataV2Mapper::map)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "RecoveryData not found")
    }
}