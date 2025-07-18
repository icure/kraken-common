/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.mapper.filter

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.Code
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.DeviceDto
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.chain.FilterChain

open class FilterChainV2Mapper(
	private val filterV2Mapper: FilterV2Mapper,
) {
// 	fun <O : Identifiable<String>> map(filterChain: org.taktik.icure.domain.filter.chain.FilterChain<O>): FilterChain<O> =
// 		FilterChain(filterChain.filter.let { filterV2Mapper.tryMap(it) } as AbstractFilterDto<O>, filterChain.predicate?.let { filterV2Mapper.map(it) })

	@JvmName("tryMapCode")
	fun tryMap(filterChainDto: FilterChain<CodeDto>): org.taktik.icure.domain.filter.chain.FilterChain<Code>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapContact")
	fun tryMap(filterChainDto: FilterChain<ContactDto>): org.taktik.icure.domain.filter.chain.FilterChain<Contact>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapHealthElement")
	fun tryMap(filterChainDto: FilterChain<HealthElementDto>): org.taktik.icure.domain.filter.chain.FilterChain<HealthElement>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapInvoice")
	fun tryMap(filterChainDto: FilterChain<InvoiceDto>): org.taktik.icure.domain.filter.chain.FilterChain<Invoice>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapMaintenanceTask")
	fun tryMap(filterChainDto: FilterChain<MaintenanceTaskDto>): org.taktik.icure.domain.filter.chain.FilterChain<MaintenanceTask>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapPatient")
	fun tryMap(filterChainDto: FilterChain<PatientDto>): org.taktik.icure.domain.filter.chain.FilterChain<Patient>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapService")
	fun tryMap(filterChainDto: FilterChain<ServiceDto>): org.taktik.icure.domain.filter.chain.FilterChain<org.taktik.icure.entities.embed.Service>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapDevice")
	fun tryMap(filterChainDto: FilterChain<DeviceDto>): org.taktik.icure.domain.filter.chain.FilterChain<Device>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapHealthcareParty")
	fun tryMap(filterChainDto: FilterChain<HealthcarePartyDto>): org.taktik.icure.domain.filter.chain.FilterChain<HealthcareParty>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapUser")
	fun tryMap(filterChainDto: FilterChain<UserDto>): org.taktik.icure.domain.filter.chain.FilterChain<User>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapMessage")
	fun tryMap(filterChainDto: FilterChain<MessageDto>): org.taktik.icure.domain.filter.chain.FilterChain<Message>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	@JvmName("tryMapCalendarItem")
	fun tryMap(filterChainDto: FilterChain<CalendarItemDto>): org.taktik.icure.domain.filter.chain.FilterChain<CalendarItem>? = tryMap(filterChainDto) { filterV2Mapper.tryMap(it) }

	protected fun <I : IdentifiableDto<String>, O : Identifiable<String>> tryMap(
		filterChainDto: FilterChain<I>,
		mapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?,
	): org.taktik.icure.domain.filter.chain.FilterChain<O>? = mapFilter(filterChainDto.filter)?.let { mappedFilter ->
		org.taktik.icure.domain.filter.chain.FilterChain(
			mappedFilter,
			filterChainDto.predicate?.let { filterV2Mapper.map(it) },
		)
	}
}
