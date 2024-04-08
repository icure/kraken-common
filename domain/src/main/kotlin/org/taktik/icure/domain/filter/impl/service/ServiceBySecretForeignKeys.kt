/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.impl.service

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.withEncryptionMetadata

data class ServiceBySecretForeignKeys(
	override val desc: String? = null,
	override val healthcarePartyId: String? = null,
	override val patientSecretForeignKeys: Set<String>
) : AbstractFilter<Service>, org.taktik.icure.domain.filter.service.ServiceBySecretForeignKeys {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Service, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return patientSecretForeignKeys.isNotEmpty()
				&& (healthcarePartyId == null || item.withEncryptionMetadata()?.let { searchKeyMatcher(healthcarePartyId, it) } == true)
				&& (item.secretForeignKeys?.any { sfk: String? -> patientSecretForeignKeys.contains(sfk) }) == true
	}
}
