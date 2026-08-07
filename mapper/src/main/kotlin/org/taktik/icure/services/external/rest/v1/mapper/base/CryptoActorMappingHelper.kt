package org.taktik.icure.services.external.rest.v1.mapper.base

import org.taktik.icure.config.CardinalVersionConfig
import org.taktik.icure.entities.base.CryptoActor
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2Mapper

object CryptoActorMappingHelper {
	private val minDataOwnerGroupLinksVersion: SemanticVersion = SemanticVersion("3.0.0-PREVIEW-1")

	/**
	 * The `parentId`/`dataOwnerGroups` shape to present when mapping [cryptoActor] to a DTO for reading.
	 *
	 * A cardinal 3+ reader understands `dataOwnerGroups` natively: fold the legacy [CryptoActor.parentId] into it
	 * (if not already present there) and always report `parentId` as `null`.
	 *
	 * An older reader only understands `parentId`: pass through [cryptoActor]'s stored `parentId`/`dataOwnerGroups`
	 * verbatim, with no attempt to collapse one into the other. This is safe because the *stored* representation is
	 * already normalized towards the legacy shape at write time (see the logic layer, which prefers `parentId`
	 * whenever there is a single admin-type link) — there is nothing left for a read-time collapse to do, and
	 * unlike the old per-edge-typed model there is no longer a link type to assert on.
	 */
	suspend fun mapParentIdAndDataOwnerGroupLinks(
		cryptoActor: CryptoActor,
		linkMapper: DataOwnerGroupLinkV2Mapper,
		cardinalVersionConfig: CardinalVersionConfig,
	): Pair<String?, List<DataOwnerGroupLinkDto>> = if (shouldUseLegacyParentId(cardinalVersionConfig)) {
		cryptoActor.parentId to cryptoActor.dataOwnerGroups.map(linkMapper::map)
	} else {
		null to CryptoActor.normalizedDataOwnerGroupLinks(cryptoActor.dataOwnerGroups, cryptoActor.parentId).map(linkMapper::map)
	}

	private suspend fun shouldUseLegacyParentId(cardinalVersionConfig: CardinalVersionConfig) =
		cardinalVersionConfig.getUserCardinalVersion()?.let {
			it < minDataOwnerGroupLinksVersion
		} ?: true
}