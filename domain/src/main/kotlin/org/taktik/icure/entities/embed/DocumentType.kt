/*
 * Copyright (c) 2020. Taktik SA,
 * All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion

@EnumVersion(1L)
enum class DocumentType {
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
		fun fromName(name: String): DocumentType? = values().find { it.name == name }
	}
}
