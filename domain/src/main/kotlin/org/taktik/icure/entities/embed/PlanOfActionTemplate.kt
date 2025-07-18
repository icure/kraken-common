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
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlanOfActionTemplate(
	@param:JsonProperty("_id") override val id: String,
	@field:NotNull(autoFix = AutoFix.NOW) override val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) override val modified: Long? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) override val author: String? = null,
	@field:NotNull(autoFix = AutoFix.CURRENTDATAOWNERID, applyOnModify = false) override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	override val endOfLife: Long? = null,

	// Usually one of the following is used (either valueDate or openingDate and closingDate)
	override val name: String? = null,
	val descr: String? = null,
	val note: String? = null,
	val relevant: Boolean = true,
	val status: Int = 0, // bit 0: active/inactive, bit 1: relevant/irrelevant, bit 2 : present/absent, ex: 0 = active,relevant and present
	var forms: List<FormSkeleton> = emptyList(),
) : ICureDocument<String>,
	Named {
	companion object : DynamicInitializer<PlanOfActionTemplate>

	fun merge(other: PlanOfActionTemplate) = PlanOfActionTemplate(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: PlanOfActionTemplate) = super.solveConflictsWith(other) +
		mapOf(
			"name" to (this.descr ?: other.descr),
			"descr" to (this.descr ?: other.descr),
			"note" to (this.note ?: other.note),
			"relevant" to this.relevant,
			"status" to (this.status),
			"forms" to mergeListsDistinct(this.forms, other.forms),
		)

	override fun withTimestamps(created: Long?, modified: Long?) = when {
		created != null && modified != null -> this.copy(created = created, modified = modified)
		created != null -> this.copy(created = created)
		modified != null -> this.copy(modified = modified)
		else -> this
	}
}
