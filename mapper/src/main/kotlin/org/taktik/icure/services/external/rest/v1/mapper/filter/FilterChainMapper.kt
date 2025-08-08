/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.filter

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.Code
import org.taktik.icure.services.external.rest.v1.dto.CodeDto
import org.taktik.icure.services.external.rest.v1.dto.ContactDto
import org.taktik.icure.services.external.rest.v1.dto.DeviceDto
import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v1.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v1.dto.PatientDto
import org.taktik.icure.services.external.rest.v1.dto.UserDto
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.chain.FilterChain
import org.taktik.icure.entities.embed.Service as iCureService
@Service
@Profile("app")
class FilterChainMapper(
	private val filterMapper: FilterMapper,
) {

	@JvmName("tryMapCode")
	fun tryMap(filterChainDto: FilterChain<CodeDto>): org.taktik.icure.domain.filter.chain.FilterChain<Code>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapContact")
	fun tryMap(filterChainDto: FilterChain<ContactDto>): org.taktik.icure.domain.filter.chain.FilterChain<Contact>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapHealthElement")
	fun tryMap(filterChainDto: FilterChain<HealthElementDto>): org.taktik.icure.domain.filter.chain.FilterChain<HealthElement>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapInvoice")
	fun tryMap(filterChainDto: FilterChain<InvoiceDto>): org.taktik.icure.domain.filter.chain.FilterChain<Invoice>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapMaintenanceTask")
	fun tryMap(filterChainDto: FilterChain<MaintenanceTaskDto>): org.taktik.icure.domain.filter.chain.FilterChain<MaintenanceTask>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapPatient")
	fun tryMap(filterChainDto: FilterChain<PatientDto>): org.taktik.icure.domain.filter.chain.FilterChain<Patient>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapService")
	fun tryMap(filterChainDto: FilterChain<ServiceDto>): org.taktik.icure.domain.filter.chain.FilterChain<iCureService>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapDevice")
	fun tryMap(filterChainDto: FilterChain<DeviceDto>): org.taktik.icure.domain.filter.chain.FilterChain<Device>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapHealthcareParty")
	fun tryMap(filterChainDto: FilterChain<HealthcarePartyDto>): org.taktik.icure.domain.filter.chain.FilterChain<HealthcareParty>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	@JvmName("tryMapUser")
	fun tryMap(filterChainDto: FilterChain<UserDto>): org.taktik.icure.domain.filter.chain.FilterChain<User>? = tryMap(filterChainDto) { filterMapper.tryMap(it) }

	private fun <I : IdentifiableDto<String>, O : Identifiable<String>> tryMap(
		filterChainDto: FilterChain<I>,
		mapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?,
	): org.taktik.icure.domain.filter.chain.FilterChain<O>? = mapFilter(filterChainDto.filter)?.let { mappedFilter ->
		org.taktik.icure.domain.filter.chain.FilterChain(
			mappedFilter,
			filterChainDto.predicate?.let { filterMapper.map(it) },
		)
	}
}
