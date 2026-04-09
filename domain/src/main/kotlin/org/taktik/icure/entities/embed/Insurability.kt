/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["insuranceId", "startDate"])
data class Insurability(
	/** Insurance extra parameters. */
	// Key from InsuranceParameter
	val parameters: Map<String, String> = emptyMap(),
	/** Is hospitalization covered. */
	val hospitalisation: Boolean? = null,
	/** Is outpatient care covered. */
	val ambulatory: Boolean? = null,
	/** Is dental care covered. */
	val dental: Boolean? = null,
	/** Identification number of the patient at the insurance. */
	val identificationNumber: String? = null, // N° in form (number for the insurance's identification)
	/** Id of the Insurance. */
	val insuranceId: String? = null, // UUID to identify Partena, etc. (link to Insurance object's document ID)
	/** Start date of the coverage (YYYYMMDD). */
	val startDate: Long? = null,
	/** End date of the coverage (YYYYMMDD). */
	val endDate: Long? = null,
	/** UUID of the contact person who is the policyholder of the insurance (when the patient is covered by the insurance of a third person). */
	val titularyId: String? = null, // UUID of the contact person who is the titulary of the insurance
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
