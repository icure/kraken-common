/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasCodes
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.handlers.JacksonLenientCollectionDeserializer
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
data class Address(
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val tags: Set<CodeStub> = emptySet(),
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) override val codes: Set<CodeStub> = emptySet(),
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val identifier: List<Identifier> = emptyList(),
	val addressType: AddressType? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val descr: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val street: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val houseNumber: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val postboxNumber: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val postalCode: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val city: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val state: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val country: String? = null,
	@Deprecated("Use notes instead") @param:ContentValue(ContentValues.ANY_STRING) val note: String? = null,
	val notes: List<Annotation> = emptyList(),
	@JsonDeserialize(using = JacksonLenientCollectionDeserializer::class) val telecoms: List<Telecom> = emptyList(),
	override val encryptedSelf: String? = null,
	override val extensions: RawJson.JsonObject? = null,
) : Encryptable,
	Serializable,
	Comparable<Address>,
	HasTags,
	HasCodes,
	Extendable {
	companion object : DynamicInitializer<Address>

	fun merge(other: Address) = Address(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Address) =
		super<Encryptable>.solveConflictsWith(other) +
		super<Extendable>.solveConflictsWith(other) +
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
