package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Enumeration of the possible types of data owners in the iCure platform: healthcare parties, devices, and patients.
 */
enum class DataOwnerTypeDto {
	@JsonProperty("hcp")
	HCP,

	@JsonProperty("device")
	DEVICE,

	@JsonProperty("patient")
	PATIENT,
}
