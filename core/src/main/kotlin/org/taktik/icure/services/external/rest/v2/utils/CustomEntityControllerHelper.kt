package org.taktik.icure.services.external.rest.v2.utils

import com.icure.cardinal.customentities.config.CustomEntityDefinition
import com.icure.cardinal.customentities.config.VersionedCustomEntitiesConfiguration
import com.icure.cardinal.customentities.util.ExtendableBuiltinEntityValidatorMapperConfigsProvider
import com.icure.cardinal.errorreporting.MapperScopePathProvider
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.mapper.CustomEntityBaseV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.MappersWithCustomExtensions.mapCustomFromDto

/**
 * Validation and mapping shared by all controllers of custom entities, regardless of where the custom entities
 * configuration comes from (current user, specific group, ...).
 */
@Component
@Profile("app")
class CustomEntityControllerHelper(
	private val mapper: CustomEntityBaseV2Mapper,
	private val scopePathProvider: MapperScopePathProvider,
	private val builtinValidationConfigsProvider: ExtendableBuiltinEntityValidatorMapperConfigsProvider,
) {
	/**
	 * Get the definition of [entityType] from [config], failing with a not found error if the definition doesn't exist
	 * or is disabled.
	 */
	fun requireDefinition(
		config: VersionedCustomEntitiesConfiguration?,
		entityType: String,
	): Pair<VersionedCustomEntitiesConfiguration, CustomEntityDefinition> {
		val customEntityConfig = config?.customEntities[entityType]?.takeIf { !it.disabled } ?: throw NotFoundRequestException("No custom entity config found for $entityType")
		return config to customEntityConfig
	}

	fun requireDelegationBased(
		customEntityDefinition: CustomEntityDefinition,
		entityType: String,
	) {
		require(customEntityDefinition.accessControl == CustomEntityDefinition.AccessControl.DelegationBased) {
			"Custom entity type $entityType does not support delegations and/or encryption"
		}
	}

	/**
	 * Attachment endpoints are only available if the entity supports attachments and they are not disabled.
	 * Note: even if attachments are disabled, the modification of entities still preserves the existing attachments
	 * information, this is handled by the logic.
	 */
	fun requireAttachmentsEnabled(
		customEntityDefinition: CustomEntityDefinition,
		entityType: String,
	) {
		require(customEntityDefinition.attachmentsConfiguration?.disable == false) {
			"Attachments are not enabled for custom entity type $entityType"
		}
	}

	suspend fun mapFromDto(
		config: VersionedCustomEntitiesConfiguration,
		customEntityDefinition: CustomEntityDefinition,
		entityType: String,
		dto: CustomEntityBaseDto,
	): CustomEntityBase {
		validateEntityMetadataWithConfig(dto, customEntityDefinition, entityType)
		return mapCustomFromDto(
			dto = dto,
			config = config,
			customEntityTypeId = entityType,
			doMap = { a, b ->
				mapper.map(
					customEntityBaseDto = a,
					entityTypeId = entityType,
					mapperExtensionsValidationContext = b
				)
			},
			scopePath = scopePathProvider.getScopePathFor(entityType),
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		)
	}

	suspend fun mapFromDtos(
		config: VersionedCustomEntitiesConfiguration,
		customEntityDefinition: CustomEntityDefinition,
		entityType: String,
		dtos: List<CustomEntityBaseDto>,
	): List<CustomEntityBase> {
		dtos.forEach { dto ->
			validateEntityMetadataWithConfig(dto, customEntityDefinition, entityType)
		}
		// Can do extra validation here
		return mapCustomFromDto(
			dtos = dtos,
			config = config,
			customEntityTypeId = entityType,
			doMap = { a, b ->
				mapper.map(
					customEntityBaseDto = a,
					entityTypeId = entityType,
					mapperExtensionsValidationContext = b
				)
			},
			scopePath = scopePathProvider.getScopePathFor(entityType),
			builtinValidationConfigsProvider = builtinValidationConfigsProvider,
		)
	}

	private fun validateEntityMetadataWithConfig(
		entity: CustomEntityBaseDto,
		customEntityDefinition: CustomEntityDefinition,
		customEntityType: String,
	) {
		if (customEntityDefinition.accessControl != CustomEntityDefinition.AccessControl.DelegationBased) {
			require (
				entity.secretForeignKeys.isEmpty() &&
					entity.cryptedForeignKeys.isEmpty() &&
					entity.delegations.isEmpty() &&
					entity.encryptionKeys.isEmpty() &&
					entity.securityMetadata == null
			) {
				"Custom entity type $customEntityType does not support delegations and/or encryption"
			}
		}
		if (customEntityDefinition.attachmentsConfiguration == null) {
			require (
				entity.dataAttachments.isEmpty()
			) {
				"Custom entity type $customEntityType does not support attachments"
			}
		}
	}
}
