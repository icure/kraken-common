package org.taktik.icure.services.external.rest.v1.mapper.base

import org.taktik.icure.config.CardinalVersionConfig
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2Mapper

object CryptoActorMappingHelper {
	private val minDataOwnerGroupLinksVersion: SemanticVersion = SemanticVersion("3.0.0-PREVIEW-1")

	// If should use legacy parent id check try to move from data owner group link to parent id, failing with illegal argument if not possible (and leave list of links empty)
	// Otherwise move parent id (if any) into links and set null
	@Suppress("DEPRECATION")
	suspend fun mapParentIdAndDataOwnerGroupLinks(
		cryptoActor: CryptoActor,
		linkMapper: DataOwnerGroupLinkV2Mapper,
		cardinalVersionConfig: CardinalVersionConfig,
	): Pair<String?, List<DataOwnerGroupLinkDto>> {
		val normalized = CryptoActor.normalizedDataOwnerGroupLinks(cryptoActor.dataOwnerGroups, cryptoActor.parentId)
		return if (shouldUseLegacyParentId(cardinalVersionConfig)) {
			val legacyParentId = if (normalized.isEmpty()) {
				null
			} else {
				require(normalized.size == 1) {
					"Cannot represent dataOwnerGroups links $normalized as a single legacy parentId: a client on an older Cardinal version only supports a single parent."
				}
				val onlyLink = normalized.single()
				require(onlyLink.linkType == DataOwnerGroupLinkType.parent) {
					"Cannot represent dataOwnerGroups link $onlyLink as a legacy parentId: a client on an older Cardinal version only supports links of type ${DataOwnerGroupLinkType.parent}."
				}
				onlyLink.dataOwnerId
			}
			legacyParentId to emptyList()
		} else {
			null to normalized.map { linkMapper.map(it) }
		}
	}

	// If has single link of type parent put back in parentId (only, no duplication), as it allows non-updated views to still work (required for cases where new cardinal is used alongside older cardinal/legacy typescript)
	// Do this regardless of version (new views will also handle legacy case without issues)
	@Suppress("DEPRECATION", "UNUSED_PARAMETER")
	suspend fun mapParentIdAndDataOwnerGroupLinks(
		cryptoActor: CryptoActorDto,
		linkMapper: DataOwnerGroupLinkV2Mapper,
		// Intentionally unused: this mapping direction is version-independent (see comment above); kept for
		// signature symmetry with the other overload.
		cardinalVersionConfig: CardinalVersionConfig,
	): Pair<String?, List<DataOwnerGroupLink>> {
		val normalized = CryptoActor.normalizedDataOwnerGroupLinks(
			cryptoActor.dataOwnerGroups.map(linkMapper::map),
			cryptoActor.parentId,
		)
		val singleParentLink = normalized.singleOrNull()?.takeIf { it.linkType == DataOwnerGroupLinkType.parent }
		return if (singleParentLink != null) {
			singleParentLink.dataOwnerId to emptyList()
		} else {
			null to normalized.toList()
		}
	}

	private suspend fun shouldUseLegacyParentId(cardinalVersionConfig: CardinalVersionConfig) =
		cardinalVersionConfig.getUserCardinalVersion()?.let {
			it < minDataOwnerGroupLinksVersion
		} ?: true
}