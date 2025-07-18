package org.taktik.icure.services.external.rest.v1.controllers.support

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.services.external.rest.v1.dto.filter.code.AllCodesFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.code.CodeByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.code.CodeByRegionTypeLabelLanguageFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.code.CodeIdsByTypeCodeVersionIntervalFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.contact.ContactByHcPartyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.contact.ContactByHcPartyPatientTagCodeDateFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.contact.ContactByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.contact.ContactByServiceIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.device.AllDevicesFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.device.DeviceByHcPartyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.device.DeviceByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.hcparty.AllHealthcarePartiesFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.hcparty.HealthcarePartyByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.healthelement.HealthElementByHcPartyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.healthelement.HealthElementByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.healthelement.HealthElementByHcPartySecretForeignKeysFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.healthelement.HealthElementByHcPartyTagCodeFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.healthelement.HealthElementByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.invoice.InvoiceByHcPartyCodeDateFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.maintenancetask.MaintenanceTaskAfterDateFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.maintenancetask.MaintenanceTaskByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyAndActiveFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyAndExternalIdFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyAndIdentifiersFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyAndSsinFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyAndSsinsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyDateOfBirthBetweenFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyDateOfBirthFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyGenderEducationProfession
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyNameContainsFuzzyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByHcPartyNameFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.patient.PatientByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceByContactsAndSubcontactsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceByHcPartyFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.service.ServiceBySecretForeignKeys
import org.taktik.icure.services.external.rest.v1.dto.filter.user.AllUsersFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.user.UserByIdsFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.user.UserByNameEmailPhoneFilter
import org.taktik.icure.services.external.rest.v1.dto.filter.user.UsersByPatientIdFilter

@RestController
@Profile("app")
@RequestMapping("/rest/v1/filter")
@Tag(name = "filter")
class FilterController {
	data class FilterDefinitions(
		val allCodesFilter: AllCodesFilter? = null,
		val codeByIdsFilter: CodeByIdsFilter? = null,
		val codeByRegionTypeLabelLanguageFilter: CodeByRegionTypeLabelLanguageFilter? = null,
		val codeIdsByTypeCodeVersionIntervalFilter: CodeIdsByTypeCodeVersionIntervalFilter? = null,
		val contactByHcPartyFilter: ContactByHcPartyFilter? = null,
		val contactByHcPartyPatientTagCodeDateFilter: ContactByHcPartyPatientTagCodeDateFilter? = null,
		val contactByHcPartyTagCodeDateFilter: ContactByHcPartyTagCodeDateFilter? = null,
		val contactByServiceIdsFilter: ContactByServiceIdsFilter? = null,
		val allDevicesFilter: AllDevicesFilter? = null,
		val deviceByHcPartyFilter: DeviceByHcPartyFilter? = null,
		val deviceByIdsFilter: DeviceByIdsFilter? = null,
		val allHealthcarePartiesFilter: AllHealthcarePartiesFilter? = null,
		val healthcarePartyByIdsFilter: HealthcarePartyByIdsFilter? = null,
		val healthElementByHcPartyFilter: HealthElementByHcPartyFilter? = null,
		val healthElementByHcPartyIdentifiersFilter: HealthElementByHcPartyIdentifiersFilter? = null,
		val healthElementByHcPartySecretForeignKeysFilter: HealthElementByHcPartySecretForeignKeysFilter? = null,
		val healthElementByHcPartyTagCodeFilter: HealthElementByHcPartyTagCodeFilter? = null,
		val healthElementByIdsFilter: HealthElementByIdsFilter? = null,
		val invoiceByHcPartyCodeDateFilter: InvoiceByHcPartyCodeDateFilter? = null,
		val maintenanceTaskAfterDateFilter: MaintenanceTaskAfterDateFilter? = null,
		val maintenanceTaskByHcPartyAndIdentifiersFilter: MaintenanceTaskByHcPartyAndIdentifiersFilter? = null,
		val maintenanceTaskByIdsFilter: MaintenanceTaskByIdsFilter? = null,
		val maintenanceTaskByHcPartyAndTypeFilter: MaintenanceTaskByHcPartyAndTypeFilter? = null,
		val patientByHcPartyAndActiveFilter: PatientByHcPartyAndActiveFilter? = null,
		val patientByHcPartyAndExternalIdFilter: PatientByHcPartyAndExternalIdFilter? = null,
		val patientByHcPartyAndIdentifiersFilter: PatientByHcPartyAndIdentifiersFilter? = null,
		val patientByHcPartyAndSsinFilter: PatientByHcPartyAndSsinFilter? = null,
		val patientByHcPartyAndSsinsFilter: PatientByHcPartyAndSsinsFilter? = null,
		val patientByHcPartyDateOfBirthBetweenFilter: PatientByHcPartyDateOfBirthBetweenFilter? = null,
		val patientByHcPartyDateOfBirthFilter: PatientByHcPartyDateOfBirthFilter? = null,
		val patientByHcPartyFilter: PatientByHcPartyFilter? = null,
		val patientByHcPartyGenderEducationProfession: PatientByHcPartyGenderEducationProfession? = null,
		val patientByHcPartyNameContainsFuzzyFilter: PatientByHcPartyNameContainsFuzzyFilter? = null,
		val patientByHcPartyNameFilter: PatientByHcPartyNameFilter? = null,
		val patientByIdsFilter: PatientByIdsFilter? = null,
		val serviceByContactsAndSubcontactsFilter: ServiceByContactsAndSubcontactsFilter? = null,
		val serviceByHcPartyFilter: ServiceByHcPartyFilter? = null,
		val serviceByHcPartyIdentifiersFilter: ServiceByHcPartyIdentifiersFilter? = null,
		val serviceByHcPartyTagCodeDateFilter: ServiceByHcPartyTagCodeDateFilter? = null,
		val serviceByIdsFilter: ServiceByIdsFilter? = null,
		val serviceBySecretForeignKeys: ServiceBySecretForeignKeys? = null,
		val allUsersFilter: AllUsersFilter? = null,
		val userByIdsFilter: UserByIdsFilter? = null,
		val usersByPatientIdFilter: UsersByPatientIdFilter? = null,
		val userByNameEmailPhoneFilter: UserByNameEmailPhoneFilter? = null,
	)

	@GetMapping("/definitions")
	fun allFilterDefinitions() = FilterDefinitions()
}
