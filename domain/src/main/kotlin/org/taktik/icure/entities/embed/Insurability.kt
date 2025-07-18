/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 15:37
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Insurability(
	// Key from InsuranceParameter
	val parameters: Map<String, String> = emptyMap(),
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val hospitalisation: Boolean? = null,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val ambulatory: Boolean? = null,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val dental: Boolean? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val identificationNumber: String? = null, // N° in form (number for the insurance's identification)
	@param:ContentValue(ContentValues.ANY_STRING) val insuranceId: String? = null, // UUID to identify Partena, etc. (link to Insurance object's document ID)
	@param:ContentValue(ContentValues.FUZZY_DATE) val startDate: Long? = null,
	@param:ContentValue(ContentValues.FUZZY_DATE) val endDate: Long? = null,
	val titularyId: String? = null, // UUID of the contact person who is the titulary of the insurance
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
