/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

/**
 * Created by aduchate on 29/03/13, 18:37
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Delegation(
	// This is not the owner of a piece of date (patient, contact). It is the owner of the delegation.
	var owner: String? = null, // owner id
	var delegatedTo: String? = null, // delegatedTo id
	var key: String? = null, // An arbitrary key (generated, patientId, any ID, etc.), usually prefixed with the entity ID followed by ":", encrypted using an exchange AES key.
	@Deprecated("replaced by access control") var tags: List<String> = emptyList(), // Used for rights
) : Serializable
