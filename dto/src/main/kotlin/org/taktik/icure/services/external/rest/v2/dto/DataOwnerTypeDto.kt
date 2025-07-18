package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonProperty

enum class DataOwnerTypeDto {
	@JsonProperty("hcp")
	HCP,

	@JsonProperty("device")
	DEVICE,

	@JsonProperty("patient")
	PATIENT,
}
