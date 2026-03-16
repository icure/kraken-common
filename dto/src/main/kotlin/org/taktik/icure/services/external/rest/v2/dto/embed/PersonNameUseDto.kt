package org.taktik.icure.services.external.rest.v2.dto.embed

import org.taktik.icure.entities.base.EnumVersion
import java.io.Serializable

@EnumVersion(1L)
/**
 * Enumerates the possible uses of a person name, following FHIR HumanName use codes.
 */
enum class PersonNameUseDto : Serializable {
	usual,
	official,
	temp,
	nickname,
	anonymous,
	maiden,
	old,
	other,
}
