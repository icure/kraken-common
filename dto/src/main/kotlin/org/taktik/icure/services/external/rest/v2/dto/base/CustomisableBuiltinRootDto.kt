package org.taktik.icure.services.external.rest.v2.dto.base

import org.taktik.icure.CardinalMetadataProperty
import org.taktik.icure.dto.annotations.filtering.ActiveField

interface CustomisableBuiltinRootDto : CustomisableRootDto {
	/**
	 * Id of the customised type of this entity: a key of the extensions configured for this builtin entity type, or
	 * null for unextended entities. It is null if and only if [customisedModelVersion] is null.
	 */
	@CardinalMetadataProperty
	@ActiveField
	val customisedTypeId: String?
}
