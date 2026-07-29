/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion

/**
 * The type of entity a [Partnership.partnerId] refers to. When null the partner is either a patient
 * or a healthcare party (legacy behavior, the client has to resolve the ambiguity).
 */
@EnumVersion(1L)
enum class PartnerType {
	patient,
	healthcareParty,
	relatedPerson,
}
