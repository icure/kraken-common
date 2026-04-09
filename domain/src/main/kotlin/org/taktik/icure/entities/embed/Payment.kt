/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
	/** The date of the payment (unix epoch in ms). */
	val paymentDate: Long = 0,
	/** The type of payment method used. */
	val paymentType: PaymentType? = null,
	/** The amount paid. */
	val paid: Double? = null,
) : Serializable
