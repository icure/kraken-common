/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.base.Named
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlanOfActionTemplate(
	/** The unique identifier of this plan of action template. */
	@param:JsonProperty("_id") override val id: String,
	/** The timestamp (unix epoch in ms) of creation. */
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	/** The identifier of the author. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	/** The identifier of the responsible entity. */
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	/** Deprecated. The identifier of the medical location. */
	override val medicalLocationId: String? = null,
	/** The set of tags associated with this template. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	/** The set of codes associated with this template. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	/** The soft-delete timestamp. */
	override val endOfLife: Long? = null,

	/** The name of this plan of action template. */
	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	override val name: String? = null,
	/** A description of the template. */
	val descr: String? = null,
	/** A note associated with the template. */
	val note: String? = null,
	/** Whether this template is relevant. */
	val relevant: Boolean = true,
	/** A bitmask status (bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2: present/absent). */
	val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	var forms: List<FormSkeleton> = emptyList(),
) : ICureDocument<String>,
	Named {

	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
