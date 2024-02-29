/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseInitialisationDto(
	val users: List<UserDto>? = emptyList(),
	val healthcareParties: List<HealthcarePartyDto>? = emptyList(),
	val replication: ReplicationDto? = null,
	val minimumKrakenVersion: String?,
) : Serializable
