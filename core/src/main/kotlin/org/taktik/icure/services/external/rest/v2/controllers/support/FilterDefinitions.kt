package org.taktik.icure.services.external.rest.v2.controllers.support

import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByDataOwnerPatientDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.accesslog.AccessLogByUserIdUserTypeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaByTypedPropertyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaByUserIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaReadableByUserIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaReadableByUserRightsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AgendaWithPropertyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.agenda.AllAgendasFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.calendarItem.CalendarItemByDataOwnerLifecycleBetween
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
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerFormIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerOpeningDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerServiceCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByDataOwnerServiceTagFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByExternalIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyPatientTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.contact.ContactByServiceIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.AllDevicesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.DeviceByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.device.DeviceByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.document.AllDocumentsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.document.DocumentByDataOwnerCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.document.DocumentByDataOwnerPatientDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.document.DocumentByDataOwnerTagFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.document.DocumentByTypeDataOwnerPatientFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.form.FormByDataOwnerParentIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.form.FormByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.form.FormByLogicalUuidFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.form.FormByUniqueUuidFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.formtemplate.FormTemplateBySpecialtyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.group.AllGroupsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.group.GroupBySuperGroupFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.group.GroupWithContentFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.AllHealthcarePartiesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByNameFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByNationalIdentifierFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByParentIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByTagCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.hcparty.HealthcarePartyByTypeSpecialtyPostCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByDataOwnerPatientOpeningDate
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyIdentifiersVersioningFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartySecretForeignKeysFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyStatusFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyStatusVersioningFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyTagCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByHcPartyTagFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.healthelement.HealthElementByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.insurance.AllInsurancesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.invoice.InvoiceByHcPartyCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskAfterDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask.MaintenanceTaskByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.medicallocation.AllMedicalLocationsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.medicallocation.MedicalLocationByPostCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.LatestMessageByHcPartyTransportGuidFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerCodeFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerFromAddressFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerLifecycleBetween
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerPatientSentDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerTagFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerToAddressFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByDataOwnerTransportGuidSentDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByHcPartyTransportGuidReceivedFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByInvoiceIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.message.MessageByParentIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByDataOwnerModifiedAfterFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.patient.PatientByDataOwnerTagFilter
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
import org.taktik.icure.services.external.rest.v2.dto.filter.pricing.AllPricingFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.pricing.PricingByRegionTypesLanguageLabelFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByAssociationIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByContactsAndSubcontactsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByDataOwnerPatientDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyCodePrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyCodesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyHealthElementIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyIdentifiersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyMonthCodePrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyMonthTagPrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyPatientCodePrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyPatientCodesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyPatientTagCodesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyPatientTagPrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyTagCodesFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByHcPartyTagPrefixFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceByQualifiedLinkFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.service.ServiceBySecretForeignKeys
import org.taktik.icure.services.external.rest.v2.dto.filter.timetable.TimeTableByAgendaIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.timetable.TimeTableByPeriodAndAgendaIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.topic.TopicByHcPartyFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.topic.TopicByParticipantFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.AllUsersFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UserByHealthcarePartyIdFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UserByIdsFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UserByNameEmailPhoneFilter
import org.taktik.icure.services.external.rest.v2.dto.filter.user.UsersByPatientIdFilter

/**
 * Metadata describing a single filter exposed by the API.
 *
 * @property filter present only to carry the concrete filter DTO type so that it appears in the generated API
 * schema; it is always null in the response.
 * @property deprecated whether this filter is deprecated and should no longer be used.
 * @property entity the simple name of the entity owning the configuration view(s) this filter relies on (null if none).
 * @property views the design-doc configuration view(s) this filter relies on, all defined on [entity] (empty if none).
 */
data class FilterDefinition<out T : AbstractFilterDto<*>>(
	val filter: T? = null,
	val deprecated: Boolean = false,
	val entity: String? = null,
	val views: List<String> = emptyList(),
)

/**
 * The complete set of filters available through the v2 API, each with its deprecation status and the configuration
 * view(s) it uses. Built by [FilterDefinitionsFactory] from the registered filter logic beans.
 */
data class FilterDefinitions(
	val accessLogByDataOwnerPatientDateFilter: FilterDefinition<AccessLogByDataOwnerPatientDateFilter> = FilterDefinition(),
	val accessLogByDateFilter: FilterDefinition<AccessLogByDateFilter> = FilterDefinition(),
	val accessLogByUserIdUserTypeDateFilter: FilterDefinition<AccessLogByUserIdUserTypeDateFilter> = FilterDefinition(),
	val agendaByTypedPropertyFilter: FilterDefinition<AgendaByTypedPropertyFilter> = FilterDefinition(),
	val agendaByUserIdFilter: FilterDefinition<AgendaByUserIdFilter> = FilterDefinition(),
	val agendaReadableByUserIdFilter: FilterDefinition<AgendaReadableByUserIdFilter> = FilterDefinition(),
	val agendaReadableByUserRightsFilter: FilterDefinition<AgendaReadableByUserRightsFilter> = FilterDefinition(),
	val agendaWithPropertyFilter: FilterDefinition<AgendaWithPropertyFilter> = FilterDefinition(),
	val allAgendasFilter: FilterDefinition<AllAgendasFilter> = FilterDefinition(),
	val allCodesFilter: FilterDefinition<AllCodesFilter> = FilterDefinition(),
	val allDevicesFilter: FilterDefinition<AllDevicesFilter> = FilterDefinition(),
	val allDocumentsFilter: FilterDefinition<AllDocumentsFilter> = FilterDefinition(),
	val allGroupsFilter: FilterDefinition<AllGroupsFilter> = FilterDefinition(),
	val allHealthcarePartiesFilter: FilterDefinition<AllHealthcarePartiesFilter> = FilterDefinition(),
	val allInsurancesFilter: FilterDefinition<AllInsurancesFilter> = FilterDefinition(),
	val allMedicalLocationsFilter: FilterDefinition<AllMedicalLocationsFilter> = FilterDefinition(),
	val allPricingFilter: FilterDefinition<AllPricingFilter> = FilterDefinition(),
	val allUsersFilter: FilterDefinition<AllUsersFilter> = FilterDefinition(),
	val calendarItemByDataOwnerLifecycleBetween: FilterDefinition<CalendarItemByDataOwnerLifecycleBetween> = FilterDefinition(),
	val calendarItemByDataOwnerPatientStartTimeFilter: FilterDefinition<CalendarItemByDataOwnerPatientStartTimeFilter> = FilterDefinition(),
	val calendarItemByPeriodAndAgendaIdFilter: FilterDefinition<CalendarItemByPeriodAndAgendaIdFilter> = FilterDefinition(),
	val calendarItemByPeriodAndDataOwnerIdFilter: FilterDefinition<CalendarItemByPeriodAndDataOwnerIdFilter> = FilterDefinition(),
	val calendarItemByRecurrenceIdFilter: FilterDefinition<CalendarItemByRecurrenceIdFilter> = FilterDefinition(),
	val classificationByDataOwnerPatientCreatedDateFilter: FilterDefinition<ClassificationByDataOwnerPatientCreatedDateFilter> = FilterDefinition(),
	val codeByIdsFilter: FilterDefinition<CodeByIdsFilter> = FilterDefinition(),
	val codeByQualifiedLinkFilter: FilterDefinition<CodeByQualifiedLinkFilter> = FilterDefinition(),
	val codeByRegionTypeCodeVersionFilters: FilterDefinition<CodeByRegionTypeCodeVersionFilters> = FilterDefinition(),
	val codeByRegionTypeLabelLanguageFilter: FilterDefinition<CodeByRegionTypeLabelLanguageFilter> = FilterDefinition(),
	val codeByRegionTypesLanguageLabelVersionFilters: FilterDefinition<CodeByRegionTypesLanguageLabelVersionFilters> = FilterDefinition(),
	val codeIdsByTypeCodeVersionIntervalFilter: FilterDefinition<CodeIdsByTypeCodeVersionIntervalFilter> = FilterDefinition(),
	val contactByDataOwnerFormIdsFilter: FilterDefinition<ContactByDataOwnerFormIdsFilter> = FilterDefinition(),
	val contactByDataOwnerOpeningDateFilter: FilterDefinition<ContactByDataOwnerOpeningDateFilter> = FilterDefinition(),
	val contactByDataOwnerPatientOpeningDateFilter: FilterDefinition<ContactByDataOwnerPatientOpeningDateFilter> = FilterDefinition(),
	val contactByDataOwnerServiceCodeFilter: FilterDefinition<ContactByDataOwnerServiceCodeFilter> = FilterDefinition(),
	val contactByDataOwnerServiceTagFilter: FilterDefinition<ContactByDataOwnerServiceTagFilter> = FilterDefinition(),
	val contactByExternalIdFilter: FilterDefinition<ContactByExternalIdFilter> = FilterDefinition(),
	val contactByHcPartyFilter: FilterDefinition<ContactByHcPartyFilter> = FilterDefinition(),
	val contactByHcPartyIdentifiersFilter: FilterDefinition<ContactByHcPartyIdentifiersFilter> = FilterDefinition(),
	val contactByHcPartyPatientTagCodeDateFilter: FilterDefinition<ContactByHcPartyPatientTagCodeDateFilter> = FilterDefinition(),
	val contactByHcPartyTagCodeDateFilter: FilterDefinition<ContactByHcPartyTagCodeDateFilter> = FilterDefinition(),
	val contactByServiceIdsFilter: FilterDefinition<ContactByServiceIdsFilter> = FilterDefinition(),
	val deviceByHcPartyFilter: FilterDefinition<DeviceByHcPartyFilter> = FilterDefinition(),
	val deviceByIdsFilter: FilterDefinition<DeviceByIdsFilter> = FilterDefinition(),
	val documentByDataOwnerCodeFilter: FilterDefinition<DocumentByDataOwnerCodeFilter> = FilterDefinition(),
	val documentByDataOwnerPatientDateFilter: FilterDefinition<DocumentByDataOwnerPatientDateFilter> = FilterDefinition(),
	val documentByDataOwnerTagFilter: FilterDefinition<DocumentByDataOwnerTagFilter> = FilterDefinition(),
	val documentByTypeDataOwnerPatientFilter: FilterDefinition<DocumentByTypeDataOwnerPatientFilter> = FilterDefinition(),
	val formByDataOwnerParentIdFilter: FilterDefinition<FormByDataOwnerParentIdFilter> = FilterDefinition(),
	val formByDataOwnerPatientOpeningDateFilter: FilterDefinition<FormByDataOwnerPatientOpeningDateFilter> = FilterDefinition(),
	val formByLogicalUuidFilter: FilterDefinition<FormByLogicalUuidFilter> = FilterDefinition(),
	val formByUniqueUuidFilter: FilterDefinition<FormByUniqueUuidFilter> = FilterDefinition(),
	val formTemplateBySpecialtyFilter: FilterDefinition<FormTemplateBySpecialtyFilter> = FilterDefinition(),
	val groupBySuperGroupFilter: FilterDefinition<GroupBySuperGroupFilter> = FilterDefinition(),
	val groupWithContentFilter: FilterDefinition<GroupWithContentFilter> = FilterDefinition(),
	val healthElementByDataOwnerPatientOpeningDate: FilterDefinition<HealthElementByDataOwnerPatientOpeningDate> = FilterDefinition(),
	val healthElementByHcPartyCodeFilter: FilterDefinition<HealthElementByHcPartyCodeFilter> = FilterDefinition(),
	val healthElementByHcPartyFilter: FilterDefinition<HealthElementByHcPartyFilter> = FilterDefinition(),
	val healthElementByHcPartyIdentifiersFilter: FilterDefinition<HealthElementByHcPartyIdentifiersFilter> = FilterDefinition(),
	val healthElementByHcPartyIdentifiersVersioningFilter: FilterDefinition<HealthElementByHcPartyIdentifiersVersioningFilter> = FilterDefinition(),
	val healthElementByHcPartySecretForeignKeysFilter: FilterDefinition<HealthElementByHcPartySecretForeignKeysFilter> = FilterDefinition(),
	val healthElementByHcPartyStatusFilter: FilterDefinition<HealthElementByHcPartyStatusFilter> = FilterDefinition(),
	val healthElementByHcPartyStatusVersioningFilter: FilterDefinition<HealthElementByHcPartyStatusVersioningFilter> = FilterDefinition(),
	val healthElementByHcPartyTagCodeFilter: FilterDefinition<HealthElementByHcPartyTagCodeFilter> = FilterDefinition(),
	val healthElementByHcPartyTagFilter: FilterDefinition<HealthElementByHcPartyTagFilter> = FilterDefinition(),
	val healthElementByIdsFilter: FilterDefinition<HealthElementByIdsFilter> = FilterDefinition(),
	val healthcarePartyByIdentifiersFilter: FilterDefinition<HealthcarePartyByIdentifiersFilter> = FilterDefinition(),
	val healthcarePartyByIdsFilter: FilterDefinition<HealthcarePartyByIdsFilter> = FilterDefinition(),
	val healthcarePartyByNameFilter: FilterDefinition<HealthcarePartyByNameFilter> = FilterDefinition(),
	val healthcarePartyByNationalIdentifierFilter: FilterDefinition<HealthcarePartyByNationalIdentifierFilter> = FilterDefinition(),
	val healthcarePartyByParentIdFilter: FilterDefinition<HealthcarePartyByParentIdFilter> = FilterDefinition(),
	val healthcarePartyByTagCodeFilter: FilterDefinition<HealthcarePartyByTagCodeFilter> = FilterDefinition(),
	val healthcarePartyByTypeSpecialtyPostCodeFilter: FilterDefinition<HealthcarePartyByTypeSpecialtyPostCodeFilter> = FilterDefinition(),
	val invoiceByHcPartyCodeDateFilter: FilterDefinition<InvoiceByHcPartyCodeDateFilter> = FilterDefinition(),
	val latestMessageByHcPartyTransportGuidFilter: FilterDefinition<LatestMessageByHcPartyTransportGuidFilter> = FilterDefinition(),
	val maintenanceTaskAfterDateFilter: FilterDefinition<MaintenanceTaskAfterDateFilter> = FilterDefinition(),
	val maintenanceTaskByHcPartyAndIdentifiersFilter: FilterDefinition<MaintenanceTaskByHcPartyAndIdentifiersFilter> = FilterDefinition(),
	val maintenanceTaskByHcPartyAndTypeFilter: FilterDefinition<MaintenanceTaskByHcPartyAndTypeFilter> = FilterDefinition(),
	val maintenanceTaskByIdsFilter: FilterDefinition<MaintenanceTaskByIdsFilter> = FilterDefinition(),
	val medicalLocationByPostCodeFilter: FilterDefinition<MedicalLocationByPostCodeFilter> = FilterDefinition(),
	val messageByDataOwnerCodeFilter: FilterDefinition<MessageByDataOwnerCodeFilter> = FilterDefinition(),
	val messageByDataOwnerFromAddressFilter: FilterDefinition<MessageByDataOwnerFromAddressFilter> = FilterDefinition(),
	val messageByDataOwnerLifecycleBetween: FilterDefinition<MessageByDataOwnerLifecycleBetween> = FilterDefinition(),
	val messageByDataOwnerPatientSentDateFilter: FilterDefinition<MessageByDataOwnerPatientSentDateFilter> = FilterDefinition(),
	val messageByDataOwnerTagFilter: FilterDefinition<MessageByDataOwnerTagFilter> = FilterDefinition(),
	val messageByDataOwnerToAddressFilter: FilterDefinition<MessageByDataOwnerToAddressFilter> = FilterDefinition(),
	val messageByDataOwnerTransportGuidSentDateFilter: FilterDefinition<MessageByDataOwnerTransportGuidSentDateFilter> = FilterDefinition(),
	val messageByHcPartyFilter: FilterDefinition<MessageByHcPartyFilter> = FilterDefinition(),
	val messageByHcPartyTransportGuidReceivedFilter: FilterDefinition<MessageByHcPartyTransportGuidReceivedFilter> = FilterDefinition(),
	val messageByInvoiceIdsFilter: FilterDefinition<MessageByInvoiceIdsFilter> = FilterDefinition(),
	val messageByParentIdsFilter: FilterDefinition<MessageByParentIdsFilter> = FilterDefinition(),
	val patientByDataOwnerModifiedAfterFilter: FilterDefinition<PatientByDataOwnerModifiedAfterFilter> = FilterDefinition(),
	val patientByDataOwnerTagFilter: FilterDefinition<PatientByDataOwnerTagFilter> = FilterDefinition(),
	val patientByHcPartyAndActiveFilter: FilterDefinition<PatientByHcPartyAndActiveFilter> = FilterDefinition(),
	val patientByHcPartyAndAddressFilter: FilterDefinition<PatientByHcPartyAndAddressFilter> = FilterDefinition(),
	val patientByHcPartyAndExternalIdFilter: FilterDefinition<PatientByHcPartyAndExternalIdFilter> = FilterDefinition(),
	val patientByHcPartyAndIdentifiersFilter: FilterDefinition<PatientByHcPartyAndIdentifiersFilter> = FilterDefinition(),
	val patientByHcPartyAndSsinFilter: FilterDefinition<PatientByHcPartyAndSsinFilter> = FilterDefinition(),
	val patientByHcPartyAndSsinsFilter: FilterDefinition<PatientByHcPartyAndSsinsFilter> = FilterDefinition(),
	val patientByHcPartyAndTelecomFilter: FilterDefinition<PatientByHcPartyAndTelecomFilter> = FilterDefinition(),
	val patientByHcPartyDateOfBirthBetweenFilter: FilterDefinition<PatientByHcPartyDateOfBirthBetweenFilter> = FilterDefinition(),
	val patientByHcPartyDateOfBirthFilter: FilterDefinition<PatientByHcPartyDateOfBirthFilter> = FilterDefinition(),
	val patientByHcPartyFilter: FilterDefinition<PatientByHcPartyFilter> = FilterDefinition(),
	val patientByHcPartyGenderEducationProfession: FilterDefinition<PatientByHcPartyGenderEducationProfession> = FilterDefinition(),
	val patientByHcPartyNameContainsFuzzyFilter: FilterDefinition<PatientByHcPartyNameContainsFuzzyFilter> = FilterDefinition(),
	val patientByHcPartyNameFilter: FilterDefinition<PatientByHcPartyNameFilter> = FilterDefinition(),
	val patientByIdsFilter: FilterDefinition<PatientByIdsFilter> = FilterDefinition(),
	val pricingByRegionTypesLanguageLabelFilter: FilterDefinition<PricingByRegionTypesLanguageLabelFilter> = FilterDefinition(),
	val serviceByAssociationIdFilter: FilterDefinition<ServiceByAssociationIdFilter> = FilterDefinition(),
	val serviceByContactsAndSubcontactsFilter: FilterDefinition<ServiceByContactsAndSubcontactsFilter> = FilterDefinition(),
	val serviceByDataOwnerPatientDateFilter: FilterDefinition<ServiceByDataOwnerPatientDateFilter> = FilterDefinition(),
	val serviceByHcPartyCodePrefixFilter: FilterDefinition<ServiceByHcPartyCodePrefixFilter> = FilterDefinition(),
	val serviceByHcPartyCodesFilter: FilterDefinition<ServiceByHcPartyCodesFilter> = FilterDefinition(),
	val serviceByHcPartyFilter: FilterDefinition<ServiceByHcPartyFilter> = FilterDefinition(),
	val serviceByHcPartyHealthElementIdsFilter: FilterDefinition<ServiceByHcPartyHealthElementIdsFilter> = FilterDefinition(),
	val serviceByHcPartyIdentifiersFilter: FilterDefinition<ServiceByHcPartyIdentifiersFilter> = FilterDefinition(),
	val serviceByHcPartyMonthCodePrefixFilter: FilterDefinition<ServiceByHcPartyMonthCodePrefixFilter> = FilterDefinition(),
	val serviceByHcPartyMonthTagPrefixFilter: FilterDefinition<ServiceByHcPartyMonthTagPrefixFilter> = FilterDefinition(),
	val serviceByHcPartyPatientCodePrefixFilter: FilterDefinition<ServiceByHcPartyPatientCodePrefixFilter> = FilterDefinition(),
	val serviceByHcPartyPatientCodesFilter: FilterDefinition<ServiceByHcPartyPatientCodesFilter> = FilterDefinition(),
	val serviceByHcPartyPatientTagCodesFilter: FilterDefinition<ServiceByHcPartyPatientTagCodesFilter> = FilterDefinition(),
	val serviceByHcPartyPatientTagPrefixFilter: FilterDefinition<ServiceByHcPartyPatientTagPrefixFilter> = FilterDefinition(),
	val serviceByHcPartyTagCodeDateFilter: FilterDefinition<ServiceByHcPartyTagCodeDateFilter> = FilterDefinition(),
	val serviceByHcPartyTagCodesFilter: FilterDefinition<ServiceByHcPartyTagCodesFilter> = FilterDefinition(),
	val serviceByHcPartyTagPrefixFilter: FilterDefinition<ServiceByHcPartyTagPrefixFilter> = FilterDefinition(),
	val serviceByIdsFilter: FilterDefinition<ServiceByIdsFilter> = FilterDefinition(),
	val serviceByQualifiedLinkFilter: FilterDefinition<ServiceByQualifiedLinkFilter> = FilterDefinition(),
	val serviceBySecretForeignKeys: FilterDefinition<ServiceBySecretForeignKeys> = FilterDefinition(),
	val timeTableByAgendaIdFilter: FilterDefinition<TimeTableByAgendaIdFilter> = FilterDefinition(),
	val timeTableByPeriodAndAgendaIdFilter: FilterDefinition<TimeTableByPeriodAndAgendaIdFilter> = FilterDefinition(),
	val topicByHcPartyFilter: FilterDefinition<TopicByHcPartyFilter> = FilterDefinition(),
	val topicByParticipantFilter: FilterDefinition<TopicByParticipantFilter> = FilterDefinition(),
	val userByHealthcarePartyIdFilter: FilterDefinition<UserByHealthcarePartyIdFilter> = FilterDefinition(),
	val userByIdsFilter: FilterDefinition<UserByIdsFilter> = FilterDefinition(),
	val userByNameEmailPhoneFilter: FilterDefinition<UserByNameEmailPhoneFilter> = FilterDefinition(),
	val usersByPatientIdFilter: FilterDefinition<UsersByPatientIdFilter> = FilterDefinition(),
)
