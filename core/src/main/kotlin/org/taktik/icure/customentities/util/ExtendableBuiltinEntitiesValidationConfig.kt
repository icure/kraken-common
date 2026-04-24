// TODO this is auto-generated but should be moved to a build folder and tied to a gradle task
package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.`annotation`.Bean
import org.springframework.context.`annotation`.Configuration
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.AddressType
import org.taktik.icure.entities.embed.Annotation
import org.taktik.icure.entities.embed.CareTeamMember
import org.taktik.icure.entities.embed.CareTeamMemberType
import org.taktik.icure.entities.embed.CareTeamMembership
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.DocumentStatus
import org.taktik.icure.entities.embed.DocumentType
import org.taktik.icure.entities.embed.Episode
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Insurability
import org.taktik.icure.entities.embed.Laterality
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.Medication
import org.taktik.icure.entities.embed.MembershipType
import org.taktik.icure.entities.embed.PersonName
import org.taktik.icure.entities.embed.PersonNameUse
import org.taktik.icure.entities.embed.PersonalStatus
import org.taktik.icure.entities.embed.PlanOfAction
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.entities.embed.TimeSeries
import org.taktik.icure.services.`external`.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.`external`.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.`external`.rest.v2.dto.base.LinkQualificationDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.AddressTypeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.AnnotationDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.CareTeamMemberDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.CareTeamMemberTypeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.CareTeamMembershipDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.ContentDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.DocumentStatusDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.DocumentTypeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.EpisodeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.GenderDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.InsurabilityDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.LateralityDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.MeasureDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.MedicationDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.MembershipTypeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.PersonNameDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.PersonNameUseDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.PersonalStatusDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.PlanOfActionDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.ServiceLinkDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.SubContactDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.TelecomDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.TelecomTypeDto
import org.taktik.icure.services.`external`.rest.v2.dto.embed.TimeSeriesDto
import org.taktik.icure.services.`external`.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.base.IdentifierV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.AnnotationV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.CareTeamMemberV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.CareTeamMembershipV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.ContentV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.EpisodeV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.InsurabilityV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.MeasureV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.MedicationV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.PersonNameV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.PlanOfActionV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.ServiceLinkV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.ServiceV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.SubContactV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.TelecomV2Mapper
import org.taktik.icure.services.`external`.rest.v2.mapper.embed.TimeSeriesV2Mapper

@Configuration
public class ExtendableBuiltinEntitiesValidationConfig {
    @Bean
    public fun provider(
        objectMapper: ObjectMapper,
        annotationV2Mapper: AnnotationV2Mapper,
        contentV2Mapper: ContentV2Mapper,
        timeSeriesV2Mapper: TimeSeriesV2Mapper,
        addressV2Mapper: AddressV2Mapper,
        serviceLinkV2Mapper: ServiceLinkV2Mapper,
        insurabilityV2Mapper: InsurabilityV2Mapper,
        medicationV2Mapper: MedicationV2Mapper,
        serviceV2Mapper: ServiceV2Mapper,
        personNameV2Mapper: PersonNameV2Mapper,
        measureV2Mapper: MeasureV2Mapper,
        careTeamMemberV2Mapper: CareTeamMemberV2Mapper,
        subContactV2Mapper: SubContactV2Mapper,
        careTeamMembershipV2Mapper: CareTeamMembershipV2Mapper,
        planOfActionV2Mapper: PlanOfActionV2Mapper,
        episodeV2Mapper: EpisodeV2Mapper,
        telecomV2Mapper: TelecomV2Mapper,
        codeStubV2Mapper: CodeStubV2Mapper,
        identifierV2Mapper: IdentifierV2Mapper,
    ): ExtendableBuiltinEntityValidatorMapperConfigsProvider =
        ExtendableBuiltinEntityValidatorMapperObjectProviderBuilder(
            objectMapper,
        ).apply {
            addMappersForExposedBuiltinObject<AnnotationDto, Annotation>(annotationV2Mapper::map,
                annotationV2Mapper::map)
            addMappersForSpecializableBuiltinObject<ContentDto, Content>(contentV2Mapper::map,
                contentV2Mapper::map)
            addMappersForExposedBuiltinObject<TimeSeriesDto, TimeSeries>(timeSeriesV2Mapper::map,
                timeSeriesV2Mapper::map)
            addMappersForExtendableBuiltinObject<AddressDto, Address>(addressV2Mapper::map,
                addressV2Mapper::map)
            addMappersForExposedBuiltinObject<ServiceLinkDto, ServiceLink>(serviceLinkV2Mapper::map,
                serviceLinkV2Mapper::map)
            addMappersForExtendableBuiltinObject<InsurabilityDto, Insurability>(insurabilityV2Mapper::map,
                insurabilityV2Mapper::map)
            addMappersForExposedBuiltinObject<MedicationDto, Medication>(medicationV2Mapper::map,
                medicationV2Mapper::map)
            addMappersForExtendableBuiltinObject<ServiceDto, Service>(serviceV2Mapper::map,
                serviceV2Mapper::map)
            addMappersForExposedBuiltinObject<PersonNameDto, PersonName>(personNameV2Mapper::map,
                personNameV2Mapper::map)
            addMappersForExposedBuiltinObject<MeasureDto, Measure>(measureV2Mapper::map,
                measureV2Mapper::map)
            addMappersForExtendableBuiltinObject<CareTeamMemberDto,
                CareTeamMember>(careTeamMemberV2Mapper::map, careTeamMemberV2Mapper::map)
            addMappersForExtendableBuiltinObject<SubContactDto, SubContact>(subContactV2Mapper::map,
                subContactV2Mapper::map)
            addMappersForExtendableBuiltinObject<CareTeamMembershipDto,
                CareTeamMembership>(careTeamMembershipV2Mapper::map, careTeamMembershipV2Mapper::map)
            addMappersForExtendableBuiltinObject<PlanOfActionDto, PlanOfAction>(planOfActionV2Mapper::map,
                planOfActionV2Mapper::map)
            addMappersForExtendableBuiltinObject<EpisodeDto, Episode>(episodeV2Mapper::map,
                episodeV2Mapper::map)
            addMappersForExtendableBuiltinObject<TelecomDto, Telecom>(telecomV2Mapper::map,
                telecomV2Mapper::map)
            addMappersForExposedBuiltinObject<CodeStubDto, CodeStub>(codeStubV2Mapper::map,
                codeStubV2Mapper::map)
            addMappersForExposedBuiltinObject<IdentifierDto, Identifier>(identifierV2Mapper::map,
                identifierV2Mapper::map)
            addMapperForBuiltinEnum<DocumentTypeDto, DocumentType>()
            addMapperForBuiltinEnum<DocumentStatusDto, DocumentStatus>()
            addMapperForBuiltinEnum<AddressTypeDto, AddressType>()
            addMapperForBuiltinEnum<LinkQualificationDto, LinkQualification>()
            addMapperForBuiltinEnum<PersonNameUseDto, PersonNameUse>()
            addMapperForBuiltinEnum<CareTeamMemberTypeDto, CareTeamMemberType>()
            addMapperForBuiltinEnum<MembershipTypeDto, MembershipType>()
            addMapperForBuiltinEnum<TelecomTypeDto, TelecomType>()
            addMapperForBuiltinEnum<LateralityDto, Laterality>()
            addMapperForBuiltinEnum<GenderDto, Gender>()
            addMapperForBuiltinEnum<PersonalStatusDto, PersonalStatus>()
        }.build()
}
