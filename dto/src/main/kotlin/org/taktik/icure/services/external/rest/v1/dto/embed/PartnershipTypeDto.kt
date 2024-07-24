/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto

@EnumVersionDto(1L)
enum class PartnershipTypeDto {
	primary_contact, primary_contact_for, family, friend, counselor, contact, //From Kmehr
	brother, brotherinlaw, child, daughter, employer, father, grandchild, grandparent, husband, lawyer, mother, neighbour, notary, partner, sister, sisterinlaw, son, spouse, stepdaughter, stepfather, stepmother, stepson, tutor, cohabiting, registered_partner,
	next_of_kin, federal_agency, insurance_company, state_agency, unknown, //from FHIR : http://terminology.hl7.org/CodeSystem/v2-0131
	seealso, refer //from FHIR : https://www.hl7.org/fhir/codesystem-link-type.html
}
