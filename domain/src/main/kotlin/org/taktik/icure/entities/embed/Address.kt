/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.HasCodes
import org.taktik.icure.entities.base.HasTags
import org.taktik.icure.handlers.JacksonLenientCollectionDeserializer
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.ValidCode
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:43
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["addressType", "street", "postalCode", "houseNumber", "country"])
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
	override val extensions: RawJson.JsonObject? = null,
) : Encryptable,
	Serializable,
	Comparable<Address>,
	HasTags,
	HasCodes,
	Extendable {

	override fun compareTo(other: Address): Int = addressType?.compareTo(other.addressType ?: AddressType.other) ?: 0
}
