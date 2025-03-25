/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class  TimeTableHour(
	@param:ContentValue(ContentValues.ANY_LONG) val startHour: Long? = null, // hh:mm:ss
	@param:ContentValue(ContentValues.ANY_LONG) val endHour: Long? = null // hh:mm:ss
) : Serializable
