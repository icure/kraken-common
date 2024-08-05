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

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.impl.predicate.AndPredicate
import org.taktik.icure.domain.filter.impl.predicate.KeyValuePredicate
import org.taktik.icure.domain.filter.impl.predicate.NotPredicate
import org.taktik.icure.domain.filter.impl.predicate.OrPredicate
import org.taktik.icure.domain.filter.predicate.Predicate
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Classification
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
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.AgendaDto
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v2.dto.ClassificationDto
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
import org.taktik.icure.services.external.rest.v2.dto.filter.ComplementFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.ExternalViewFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.IntersectionFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.UnionFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByDataOwnerPatientDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByUserIdUserTypeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaByUserIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaReadableByUserIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByDataOwnerPatientStartTimeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByPeriodAndAgendaIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByPeriodAndDataOwnerIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByRecurrenceIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.classification.ClassificationByDataOwnerPatientCreatedDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.code.AllCodesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByQualifiedLinkFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByRegionTypeCodeVersionFilters
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByRegionTypeLabelLanguageFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByRegionTypesLanguageLabelVersionFilters
import org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeIdsByTypeCodeVersionIntervalFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyPatientTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByServiceIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.AllDevicesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.DeviceByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.DeviceByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.AllHealthcarePartiesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByNameFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByTagCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartySecretForeignKeysFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyTagCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.invoice.InvoiceByHcPartyCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskAfterDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndActiveFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndAddressFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndExternalIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndSsinFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndSsinsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyAndTelecomFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyDateOfBirthBetweenFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyDateOfBirthFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyGenderEducationProfession
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyNameContainsFuzzyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByHcPartyNameFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByContactsAndSubcontactsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyHealthElementIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceBySecretForeignKeys
import org.taktik.icure.services.external.rest.v2.dto.filter.user.AllUsersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UserByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UserByNameEmailPhoneFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UsersByPatientIdFilter
import org.taktik.icure.services.external.rest.v2.mapper.base.IdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.GenderV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.utils.ExternalFilterKeyV2Mapper

@Mapper(componentModel = "default", uses = [IdentifierV2Mapper::class, GenderV2Mapper::class, ExternalFilterKeyV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class FilterV2Mapper {

	abstract fun <O : Identifiable<String>> map(filterDto: ExternalViewFilter): org.taktik.icure.domain.filter.impl.ExternalViewFilter<O>

	abstract fun map(filterDto: AccessLogByDataOwnerPatientDateFilter): org.taktik.icure.domain.filter.impl.accesslog.AccessLogByDataOwnerPatientDateFilter
	abstract fun map(filterDto: AccessLogByDateFilter): org.taktik.icure.domain.filter.impl.accesslog.AccessLogByDateFilter
	abstract fun map(filterDto: AccessLogByUserIdUserTypeDateFilter): org.taktik.icure.domain.filter.impl.accesslog.AccessLogByUserIdUserTypeDateFilter

	@JvmName("tryMapAccessLogFilter")
	fun tryMap(filterDto: AbstractFilterDto<AccessLogDto>): AbstractFilter<AccessLog>? = when(filterDto) {
		is AccessLogByDataOwnerPatientDateFilter -> map(filterDto)
		is AccessLogByDateFilter -> map(filterDto)
		is AccessLogByUserIdUserTypeDateFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: AgendaByUserIdFilter): org.taktik.icure.domain.filter.impl.agenda.AgendaByUserIdFilter
	abstract fun map(filterDto: AgendaReadableByUserIdFilter): org.taktik.icure.domain.filter.impl.agenda.AgendaReadableByUserIdFilter

	@JvmName("tryMapAgendaFilter")
	fun tryMap(filterDto: AbstractFilterDto<AgendaDto>): AbstractFilter<Agenda>? = when(filterDto) {
		is AgendaByUserIdFilter -> map(filterDto)
		is AgendaReadableByUserIdFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: CalendarItemByPeriodAndAgendaIdFilter): org.taktik.icure.domain.filter.impl.calendaritem.CalendarItemByPeriodAndAgendaIdFilter
	abstract fun map(filterDto: CalendarItemByPeriodAndDataOwnerIdFilter): org.taktik.icure.domain.filter.impl.calendaritem.CalendarItemByPeriodAndDataOwnerIdFilter
	abstract fun map(filterDto: CalendarItemByDataOwnerPatientStartTimeFilter): org.taktik.icure.domain.filter.impl.calendaritem.CalendarItemByDataOwnerPatientStartTimeFilter
	abstract fun map(filterDto: CalendarItemByRecurrenceIdFilter): org.taktik.icure.domain.filter.impl.calendaritem.CalendarItemByRecurrenceIdFilter

	@JvmName("tryMapCalendarItemFilter")
	fun tryMap(filterDto: AbstractFilterDto<CalendarItemDto>): AbstractFilter<CalendarItem>? = when(filterDto) {
		is CalendarItemByPeriodAndAgendaIdFilter -> map(filterDto)
		is CalendarItemByPeriodAndDataOwnerIdFilter -> map(filterDto)
		is CalendarItemByDataOwnerPatientStartTimeFilter -> map(filterDto)
		is CalendarItemByRecurrenceIdFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDot: ClassificationByDataOwnerPatientCreatedDateFilter): org.taktik.icure.domain.filter.impl.classification.ClassificationByDataOwnerPatientCreatedDateFilter

	@JvmName("tryMapClassificationFilter")
	fun tryMap(filterDto: AbstractFilterDto<ClassificationDto>): AbstractFilter<Classification>? = when(filterDto) {
		is ClassificationByDataOwnerPatientCreatedDateFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: CodeByRegionTypeLabelLanguageFilter): org.taktik.icure.domain.filter.impl.code.CodeByRegionTypeLabelLanguageFilter
	abstract fun map(filterDto: CodeIdsByTypeCodeVersionIntervalFilter): org.taktik.icure.domain.filter.impl.code.CodeIdsByTypeCodeVersionIntervalFilter
	abstract fun map(filterDto: AllCodesFilter): org.taktik.icure.domain.filter.impl.code.AllCodesFilter
	abstract fun map(filterDto: CodeByIdsFilter): org.taktik.icure.domain.filter.impl.code.CodeByIdsFilter
	abstract fun map(filterDto: CodeByRegionTypesLanguageLabelVersionFilters): org.taktik.icure.domain.filter.impl.code.CodeByRegionTypesLanguageLabelVersionFilter
	abstract fun map(filterDto: CodeByRegionTypeCodeVersionFilters): org.taktik.icure.domain.filter.impl.code.CodeByRegionTypeCodeVersionFilter
	abstract fun map(filterDto: CodeByQualifiedLinkFilter): org.taktik.icure.domain.filter.impl.code.CodeByQualifiedLinkFilter

	@JvmName("tryMapCodeFilter")
	fun tryMap(filterDto: AbstractFilterDto<CodeDto>): AbstractFilter<Code>? = when (filterDto) {
		is CodeByRegionTypeLabelLanguageFilter -> map(filterDto)
		is CodeIdsByTypeCodeVersionIntervalFilter -> map(filterDto)
		is AllCodesFilter -> map(filterDto)
		is CodeByIdsFilter -> map(filterDto)
		is CodeByRegionTypesLanguageLabelVersionFilters -> map(filterDto)
		is CodeByRegionTypeCodeVersionFilters -> map(filterDto)
		is CodeByQualifiedLinkFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: ContactByHcPartyPatientTagCodeDateFilter): org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyPatientTagCodeDateFilter
	abstract fun map(filterDto: ContactByHcPartyTagCodeDateFilter): org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyTagCodeDateFilter
	abstract fun map(filterDto: ContactByServiceIdsFilter): org.taktik.icure.domain.filter.impl.contact.ContactByServiceIdsFilter
	abstract fun map(filterDto: ContactByHcPartyIdentifiersFilter): org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyIdentifiersFilter
	abstract fun map(filterDto: ContactByHcPartyFilter): org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyFilter

	@JvmName("tryMapContactFilter")
	fun tryMap(filterDto: AbstractFilterDto<ContactDto>): AbstractFilter<Contact>? = when (filterDto) {
		is ContactByHcPartyPatientTagCodeDateFilter -> map(filterDto)
		is ContactByHcPartyTagCodeDateFilter -> map(filterDto)
		is ContactByServiceIdsFilter -> map(filterDto)
		is ContactByHcPartyIdentifiersFilter -> map(filterDto)
		is ContactByHcPartyFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: HealthElementByHcPartyTagCodeFilter): org.taktik.icure.domain.filter.impl.healthelement.HealthElementByHcPartyTagCodeFilter
	abstract fun map(filterDto: HealthElementByHcPartyIdentifiersFilter): org.taktik.icure.domain.filter.impl.healthelement.HealthElementByHcPartyIdentifiersFilter
	abstract fun map(filterDto: HealthElementByHcPartySecretForeignKeysFilter): org.taktik.icure.domain.filter.impl.healthelement.HealthElementByHcPartySecretForeignKeysFilter
	abstract fun map(filterDto: HealthElementByHcPartyFilter): org.taktik.icure.domain.filter.impl.healthelement.HealthElementByHcPartyFilter
	abstract fun map(filterDto: HealthElementByIdsFilter): org.taktik.icure.domain.filter.impl.healthelement.HealthElementByIdsFilter

	@JvmName("tryMapHealthElementFilter")
	fun tryMap(filterDto: AbstractFilterDto<HealthElementDto>): AbstractFilter<HealthElement>? = when (filterDto) {
		is HealthElementByHcPartyTagCodeFilter -> map(filterDto)
		is HealthElementByHcPartyIdentifiersFilter -> map(filterDto)
		is HealthElementByHcPartySecretForeignKeysFilter -> map(filterDto)
		is HealthElementByHcPartyFilter -> map(filterDto)
		is HealthElementByIdsFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: InvoiceByHcPartyCodeDateFilter): org.taktik.icure.domain.filter.impl.invoice.InvoiceByHcPartyCodeDateFilter

	@JvmName("tryMapInvoiceFilter")
	fun tryMap(filterDto: AbstractFilterDto<InvoiceDto>): AbstractFilter<Invoice>? = when (filterDto) {
		is InvoiceByHcPartyCodeDateFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: MaintenanceTaskByHcPartyAndTypeFilter): org.taktik.icure.domain.filter.impl.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
	abstract fun map(filterDto: MaintenanceTaskByHcPartyAndIdentifiersFilter): org.taktik.icure.domain.filter.impl.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
	abstract fun map(filterDto: MaintenanceTaskByIdsFilter): org.taktik.icure.domain.filter.impl.maintenancetask.MaintenanceTaskByIdsFilter
	abstract fun map(filterDto: MaintenanceTaskAfterDateFilter): org.taktik.icure.domain.filter.impl.maintenancetask.MaintenanceTaskAfterDateFilter

	@JvmName("tryMapMaintenanceTaskFilter")
	fun tryMap(filterDto: AbstractFilterDto<MaintenanceTaskDto>): AbstractFilter<MaintenanceTask>? = when (filterDto) {
		is MaintenanceTaskByHcPartyAndTypeFilter -> map(filterDto)
		is MaintenanceTaskByHcPartyAndIdentifiersFilter -> map(filterDto)
		is MaintenanceTaskByIdsFilter -> map(filterDto)
		is MaintenanceTaskAfterDateFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: PatientByHcPartyAndActiveFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndActiveFilter
	abstract fun map(filterDto: PatientByHcPartyAndExternalIdFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndExternalIdFilter
	abstract fun map(filterDto: PatientByHcPartyAndAddressFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndAddressFilter
	abstract fun map(filterDto: PatientByHcPartyAndTelecomFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndTelecomFilter
	abstract fun map(filterDto: PatientByHcPartyAndSsinFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndSsinFilter
	abstract fun map(filterDto: PatientByHcPartyAndSsinsFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndSsinsFilter
	abstract fun map(filterDto: PatientByHcPartyDateOfBirthBetweenFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyDateOfBirthBetweenFilter
	abstract fun map(filterDto: PatientByHcPartyDateOfBirthFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyDateOfBirthFilter
	abstract fun map(filterDto: PatientByHcPartyFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyFilter
	abstract fun map(filterDto: PatientByHcPartyGenderEducationProfession): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyGenderEducationProfession
	abstract fun map(filterDto: PatientByHcPartyNameContainsFuzzyFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyNameContainsFuzzyFilter
	abstract fun map(filterDto: PatientByHcPartyNameFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyNameFilter
	abstract fun map(filterDto: PatientByHcPartyAndIdentifiersFilter): org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndIdentifiersFilter
	abstract fun map(filterDto: PatientByIdsFilter): org.taktik.icure.domain.filter.impl.patient.PatientByIdsFilter

	@JvmName("tryMapPatientFilter")
	fun tryMap(filterDto: AbstractFilterDto<PatientDto>): AbstractFilter<Patient>? = when (filterDto) {
		is PatientByHcPartyAndActiveFilter -> map(filterDto)
		is PatientByHcPartyAndExternalIdFilter -> map(filterDto)
		is PatientByHcPartyAndAddressFilter -> map(filterDto)
		is PatientByHcPartyAndTelecomFilter -> map(filterDto)
		is PatientByHcPartyAndSsinFilter -> map(filterDto)
		is PatientByHcPartyAndSsinsFilter -> map(filterDto)
		is PatientByHcPartyDateOfBirthBetweenFilter -> map(filterDto)
		is PatientByHcPartyDateOfBirthFilter -> map(filterDto)
		is PatientByHcPartyFilter -> map(filterDto)
		is PatientByHcPartyGenderEducationProfession -> map(filterDto)
		is PatientByHcPartyNameContainsFuzzyFilter -> map(filterDto)
		is PatientByHcPartyNameFilter -> map(filterDto)
		is PatientByHcPartyAndIdentifiersFilter -> map(filterDto)
		is PatientByIdsFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: ServiceByContactsAndSubcontactsFilter): org.taktik.icure.domain.filter.impl.service.ServiceByContactsAndSubcontactsFilter
	abstract fun map(filterDto: ServiceByHcPartyTagCodeDateFilter): org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyTagCodeDateFilter
	abstract fun map(filterDto: ServiceByHcPartyIdentifiersFilter): org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyIdentifiersFilter
	abstract fun map(filterDto: ServiceByHcPartyHealthElementIdsFilter): org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyHealthElementIdsFilter
	abstract fun map(filterDto: ServiceBySecretForeignKeys): org.taktik.icure.domain.filter.impl.service.ServiceBySecretForeignKeys
	abstract fun map(filterDto: ServiceByHcPartyFilter): org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyFilter
	abstract fun map(filterDto: ServiceByIdsFilter): org.taktik.icure.domain.filter.impl.service.ServiceByIdsFilter

	@JvmName("tryMapServiceFilter")
	fun tryMap(filterDto: AbstractFilterDto<ServiceDto>): AbstractFilter<Service>? = when (filterDto) {
		is ServiceByContactsAndSubcontactsFilter -> map(filterDto)
		is ServiceByHcPartyTagCodeDateFilter -> map(filterDto)
		is ServiceByHcPartyIdentifiersFilter -> map(filterDto)
		is ServiceByHcPartyHealthElementIdsFilter -> map(filterDto)
		is ServiceBySecretForeignKeys -> map(filterDto)
		is ServiceByHcPartyFilter -> map(filterDto)
		is ServiceByIdsFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: DeviceByHcPartyFilter): org.taktik.icure.domain.filter.impl.device.DeviceByHcPartyFilter
	abstract fun map(filterDto: AllDevicesFilter): org.taktik.icure.domain.filter.impl.device.AllDevicesFilter
	abstract fun map(filterDto: DeviceByIdsFilter): org.taktik.icure.domain.filter.impl.device.DeviceByIdsFilter

	@JvmName("tryMapDeviceFilter")
	fun tryMap(filterDto: AbstractFilterDto<DeviceDto>): AbstractFilter<Device>? = when (filterDto) {
		is DeviceByHcPartyFilter -> map(filterDto)
		is AllDevicesFilter -> map(filterDto)
		is DeviceByIdsFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}
	abstract fun map(filterDto: AllHealthcarePartiesFilter): org.taktik.icure.domain.filter.impl.hcparty.AllHealthcarePartiesFilter
	abstract fun map(filterDto: HealthcarePartyByIdsFilter): org.taktik.icure.domain.filter.impl.hcparty.HealthcarePartyByIdsFilter
	abstract fun map(filterDto: HealthcarePartyByNameFilter): org.taktik.icure.domain.filter.impl.hcparty.HealthcarePartyByNameFilter
	abstract fun map(filterDto: HealthcarePartyByIdentifiersFilter): org.taktik.icure.domain.filter.impl.hcparty.HealthcarePartyByIdentifiersFilter
	abstract fun map(filterDto: HealthcarePartyByTagCodeFilter): org.taktik.icure.domain.filter.impl.hcparty.HealthcarePartyByTagCodeFilter

	@JvmName("tryMapHealthcarePartyFilter")
	fun tryMap(filterDto: AbstractFilterDto<HealthcarePartyDto>): AbstractFilter<HealthcareParty>? = when (filterDto) {
		is AllHealthcarePartiesFilter -> map(filterDto)
		is HealthcarePartyByIdsFilter -> map(filterDto)
		is HealthcarePartyByNameFilter -> map(filterDto)
		is HealthcarePartyByIdentifiersFilter -> map(filterDto)
		is HealthcarePartyByTagCodeFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(filterDto: AllUsersFilter): org.taktik.icure.domain.filter.impl.user.AllUsersFilter
	abstract fun map(filterDto: UserByIdsFilter): org.taktik.icure.domain.filter.impl.user.UserByIdsFilter
	abstract fun map(filterDto: UserByNameEmailPhoneFilter): org.taktik.icure.domain.filter.impl.user.UserByNameEmailPhoneFilter
	abstract fun map(filterDto: UsersByPatientIdFilter): org.taktik.icure.domain.filter.impl.user.UsersByPatientIdFilter

	@JvmName("tryMapUserFilter")
	fun tryMap(filterDto: AbstractFilterDto<UserDto>): AbstractFilter<User>? = when (filterDto) {
		is AllUsersFilter -> map(filterDto)
		is UserByIdsFilter -> map(filterDto)
		is UserByNameEmailPhoneFilter -> map(filterDto)
		is UsersByPatientIdFilter -> map(filterDto)
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	@JvmName("tryMapMessageFilter")
	fun tryMap(filterDto: AbstractFilterDto<MessageDto>): AbstractFilter<Message>? = tryMapMessage(filterDto)

	protected open fun tryMapMessage(filterDto: AbstractFilterDto<MessageDto>) = when (filterDto) {
		else -> mapGeneralFilterToDomain(filterDto) { tryMap(it) }
	}

	abstract fun map(predicate: OrPredicate): org.taktik.icure.services.external.rest.v2.dto.filter.predicate.OrPredicate
	abstract fun map(predicate: AndPredicate): org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AndPredicate
	abstract fun map(predicate: NotPredicate): org.taktik.icure.services.external.rest.v2.dto.filter.predicate.NotPredicate
	abstract fun map(predicate: KeyValuePredicate): org.taktik.icure.services.external.rest.v2.dto.filter.predicate.KeyValuePredicate

	fun map(predicate: Predicate): org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate {
		return when (predicate) {
			is OrPredicate -> map(predicate)
			is AndPredicate -> map(predicate)
			is NotPredicate -> map(predicate)
			is KeyValuePredicate -> map(predicate)
			else -> throw IllegalArgumentException("Unsupported filter class")
		}
	}

	abstract fun map(predicateDto: org.taktik.icure.services.external.rest.v2.dto.filter.predicate.OrPredicate): OrPredicate
	abstract fun map(predicateDto: org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AndPredicate): AndPredicate
	abstract fun map(predicateDto: org.taktik.icure.services.external.rest.v2.dto.filter.predicate.NotPredicate): NotPredicate
	abstract fun map(predicateDto: org.taktik.icure.services.external.rest.v2.dto.filter.predicate.KeyValuePredicate): KeyValuePredicate

	fun map(predicateDto: org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate): Predicate {
		return when (predicateDto) {
			is org.taktik.icure.services.external.rest.v2.dto.filter.predicate.OrPredicate -> map(predicateDto)
			is org.taktik.icure.services.external.rest.v2.dto.filter.predicate.AndPredicate -> map(predicateDto)
			is org.taktik.icure.services.external.rest.v2.dto.filter.predicate.NotPredicate -> map(predicateDto)
			is org.taktik.icure.services.external.rest.v2.dto.filter.predicate.KeyValuePredicate -> map(predicateDto)
			else -> throw IllegalArgumentException("Unsupported predicate class")
		}
	}

	protected inline fun <
				I : IdentifiableDto<String>,
				O : Identifiable<String>
				> mapGeneralFilterToDomain(
		filterDto: AbstractFilterDto<I>,
		tryMapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?
		) = when (filterDto) {
			is UnionFilter<I> -> mapToDomain(filterDto, tryMapFilter)
			is ComplementFilter<I> -> mapToDomain(filterDto, tryMapFilter)
			is IntersectionFilter<I> -> mapToDomain(filterDto, tryMapFilter)
			is ExternalViewFilter -> map(filterDto)
			else -> null
		}

	protected inline fun <
				I : IdentifiableDto<String>,
				O : Identifiable<String>
				> mapToDomain(
			filterDto: UnionFilter<I>,
			tryMapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?
		) = filterDto.filters.mapNotNull { tryMapFilter(it) }.takeIf { it.size == filterDto.filters.size }?.let {
			org.taktik.icure.domain.filter.impl.UnionFilter(
				desc = filterDto.desc,
				filters = it
			)
		}

	protected inline fun <
				I : IdentifiableDto<String>,
				O : Identifiable<String>
				> mapToDomain(
			filterDto: ComplementFilter<I>,
			tryMapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?
		) =
			tryMapFilter(filterDto.superSet)?.let { superSet ->
				tryMapFilter(filterDto.subSet)?.let { subSet ->
					org.taktik.icure.domain.filter.impl.ComplementFilter(
						desc = filterDto.desc,
						subSet = subSet,
						superSet = superSet
					)
				}
			}

	protected inline fun <
				I : IdentifiableDto<String>,
				O : Identifiable<String>
				> mapToDomain(
			filterDto: IntersectionFilter<I>,
			tryMapFilter: (AbstractFilterDto<I>) -> AbstractFilter<O>?
		) = filterDto.filters.mapNotNull { tryMapFilter(it) }.takeIf { it.size == filterDto.filters.size }?.let {
			org.taktik.icure.domain.filter.impl.IntersectionFilter(
				desc = filterDto.desc,
				filters = it
			)
		}
}

