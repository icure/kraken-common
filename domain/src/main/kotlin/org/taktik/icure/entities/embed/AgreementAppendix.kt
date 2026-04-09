/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AgreementAppendix(
	/** The sequence number of the document. */
	val docSeq: Int? = null,
	/** The sequence number of the verse within the document. */
	val verseSeq: Int? = null,
	/** The identifier of the linked document. */
	val documentId: String? = null,
	/** The path to the appendix content. */
	val path: String? = null,
) : Serializable
