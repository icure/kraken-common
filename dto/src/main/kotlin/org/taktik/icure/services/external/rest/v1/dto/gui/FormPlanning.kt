/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

/**
 * Created by aduchate on 03/12/13, 16:27
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class FormPlanning(
	val planninfForAnyDoctor: Boolean? = null,
	val planningForDelegate: Boolean? = null,
	val planningForPatientDoctor: Boolean? = null,
	val planningForMe: Boolean? = null,
	val codedDelayInDays: Int? = null,
	val repetitions: Int? = null,
	val repetitionsUnit: Int? = null,
	val descr: String? = null,
) : Serializable
