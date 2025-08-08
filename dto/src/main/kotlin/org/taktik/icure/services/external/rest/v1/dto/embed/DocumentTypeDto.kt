/*
 * Copyright (c) 2020. Taktik SA,
 * All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

@Suppress("EnumEntryName")
enum class DocumentTypeDto {
	admission,
	alert,
	bvt_sample,
	clinicalpath,
	clinicalsummary,
	contactreport,
	quote,
	invoice,
	death,
	discharge,
	dischargereport,
	ebirth_baby_medicalform,
	ebirth_baby_notification,
	ebirth_mother_medicalform,
	ebirth_mother_notification,
	ecare_safe_consultation,
	epidemiology,
	intervention,
	labrequest,
	labresult,
	medicaladvisoragreement,
	medicationschemeelement,
	note,
	notification,
	pharmaceuticalprescription,
	prescription,
	productdelivery,
	quickdischargereport,
	radiationexposuremonitoring,
	referral,
	report,
	request,
	result,
	sumehr,
	telemonitoring,
	template,
	template_admin,
	treatmentsuspension,
	vaccination,
	;

	companion object {
		fun fromName(name: String): DocumentTypeDto? = values().find { it.name == name }
	}
}
