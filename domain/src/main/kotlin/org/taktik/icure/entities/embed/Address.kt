/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasCodes
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.handlers.JacksonLenientCollectionDeserializer
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:43
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable
data class Address(
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	val identifier: List<Identifier> = emptyList(),
	val addressType: AddressType? = null,
	val descr: String? = null,
	val street: String? = null,
	val houseNumber: String? = null,
	val postboxNumber: String? = null,
	val postalCode: String? = null,
	val city: String? = null,
	val state: String? = null,
	val country: String? = null,
	@Deprecated("Use notes instead") val note: String? = null,
	val notes: List<Annotation> = emptyList(),
	@param:JsonDeserialize(using = JacksonLenientCollectionDeserializer::class) val telecoms: List<Telecom> = emptyList(),
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Comparable<Address>,
	HasTags,
	HasCodes {
	companion object : DynamicInitializer<Address>

	fun merge(other: Address) = Address(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Address) = super.solveConflictsWith(other) +
		mapOf(
			"addressType" to (this.addressType ?: other.addressType),
			"descr" to (this.descr ?: other.descr),
			"street" to (this.street ?: other.street),
			"houseNumber" to (this.houseNumber ?: other.houseNumber),
			"postboxNumber" to (this.postboxNumber ?: other.postboxNumber),
			"postalCode" to (this.postalCode ?: other.postalCode),
			"city" to (this.city ?: other.city),
			"state" to (this.state ?: other.state),
			"country" to (this.country ?: other.country),
			"note" to (this.note ?: other.note),
			"notes" to mergeListsDistinct(
				this.notes,
				other.notes,
				{ a, b -> a.modified?.equals(b.modified) ?: false },
				{ a, b -> a.merge(b) },
			),
			"telecoms" to mergeListsDistinct(
				this.telecoms,
				other.telecoms,
				{ a, b -> a.telecomType?.equals(b.telecomType) ?: false },
				{ a, b -> a.merge(b) },
			),
		)

	override fun compareTo(other: Address): Int = addressType?.compareTo(other.addressType ?: AddressType.other) ?: 0
}
