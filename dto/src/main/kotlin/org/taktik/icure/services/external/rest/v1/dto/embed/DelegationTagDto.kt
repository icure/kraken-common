/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

@Suppress("EnumEntryName")
enum class DelegationTagDto {
	all,
	administrativeData,
	anonymousMedicalInformation,
	generalInformation,
	financialInformation,
	medicalInformation,
	sensitiveInformation,
	confidentialInformation,
	cdItemRisk,
	cdItemFamilyRisk,
	cdItemHealthcareelement,
	cdItemHealthcareapproach,
	cdItemAllergy,
	cdItemDiagnosis,
	cdItemLab,
	cdItemResult,
	cdItemParameter,
	cdItemMedication,
	cdItemTreatment,
	cdItemVaccine,
}
