/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.Patient
import org.taktik.icure.services.external.rest.v1.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v1.dto.PatientDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AnnotationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.EmploymentInfoMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.FinancialInstitutionInformationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.InsurabilityMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.MedicalHouseContractMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PartnershipMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PatientHealthCarePartyMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PersonNameMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SchoolingInfoMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(
	componentModel = "spring",
	uses = [
		IdentifierMapper::class,
		FinancialInstitutionInformationMapper::class,
		SchoolingInfoMapper::class,
		AddressMapper::class,
		EmploymentInfoMapper::class,
		MedicalHouseContractMapper::class,
		PatientHealthCarePartyMapper::class,
		PropertyStubMapper::class,
		CodeStubMapper::class,
		DelegationMapper::class,
		InsurabilityMapper::class,
		PartnershipMapper::class,
		PersonNameMapper::class,
		SecurityMetadataMapper::class,
		AnnotationMapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
abstract class PatientMapper{
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "extensions", ignore = true),
		Mapping(target = "extensionsVersion", ignore = true),
	)
	abstract fun map(patientDto: PatientDto): Patient

	fun map(patient: Patient): PatientDto {
		require (patient.extensions == null)  { "Patient has extensions and can't be used with v1 endpoints" }
		return doMap(patient)
	}

	protected abstract fun doMap(calendarItem: Patient): PatientDto
}
