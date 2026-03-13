package org.taktik.icure.services.external.rest.v2.dto.security

/**
 * Enumerates the categories of operations or data access that can be granted or revoked as permissions in iCure.
 */
enum class PermissionTypeDto {
	AUTHENTICATE,
	HCP,
	PHYSICIAN,
	ADMIN,
	PATIENT_VIEW,
	PATIENT_CREATE,
	PATIENT_CHANGE_DELETE,
	MEDICAL_DATA_VIEW,
	MEDICAL_DATA_CREATE,
	MEDICAL_CHANGE_DELETE,
	FINANCIAL_DATA_VIEW,
	FINANCIAL_DATA_CREATE,
	FINANCIAL_CHANGE_DELETE,
	LEGACY_DATA_VIEW,
}
