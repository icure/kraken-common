/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentityDocumentReader(
	/** The justification document number. */
	val justificatifDocumentNumber: String? = null,
	/** The serial number of the support used to read the document. */
	val supportSerialNumber: String? = null,
	/** The timestamp (unix epoch in ms) when the eID document was read. */
	val timeReadingEIdDocument: Long? = null,
	/** The type of eID document support used. */
	val eidDocumentSupportType: Int = 0,
	/** The reason code for manual encoding, if applicable. */
	val reasonManualEncoding: Int = 0,
	/** The reason code for using a vignette, if applicable. */
	val reasonUsingVignette: Int = 0,
) : Serializable
