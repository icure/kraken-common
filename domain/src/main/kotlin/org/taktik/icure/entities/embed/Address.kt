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
	/** The identifiers of the Address. */
	val identifier: List<Identifier> = emptyList(),
	/** The type of place the address represents, ex: home, office, hospital, clinic, etc. */
	val addressType: AddressType? = null,
	/** Descriptive notes about the address. */
	val descr: String? = null,
	/** Street name. */
	val street: String? = null,
	/** Building / house number. */
	val houseNumber: String? = null,
	/** Post / PO box number. */
	val postboxNumber: String? = null,
	/** Postal/PIN/ZIP/Area code. */
	val postalCode: String? = null,
	/** Name of city in the address. */
	val city: String? = null,
	/** Name of state in the Address. */
	val state: String? = null,
	/** Name / code of country in the address. */
	val country: String? = null,
	/** Additional notes. */
	@Deprecated("Use notes instead") val note: String? = null,
	/** Additional notes. */
	val notes: List<Annotation> = emptyList(),
	/** List of other contact details available through telecom services, ex: email, phone number, fax, etc. */
	@param:JsonDeserialize(using = JacksonLenientCollectionDeserializer::class) val telecoms: List<Telecom> = emptyList(),
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Comparable<Address>,
	HasTags,
	HasCodes {

	override fun compareTo(other: Address): Int = addressType?.compareTo(other.addressType ?: AddressType.other) ?: 0
}
