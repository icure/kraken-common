package org.taktik.icure.domain.customentities.util

import org.springframework.stereotype.Component
import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.domain.customentities.config.typing.BooleanTypeConfig
import org.taktik.icure.domain.customentities.config.typing.EnumDefinition
import org.taktik.icure.domain.customentities.config.typing.EnumTypeConfig
import org.taktik.icure.domain.customentities.config.typing.ExtendableBuiltinTypeConfig
import org.taktik.icure.domain.customentities.config.typing.FuzzyDateTypeConfig
import org.taktik.icure.domain.customentities.config.typing.GenericTypeConfig
import org.taktik.icure.domain.customentities.config.typing.IntTypeConfig
import org.taktik.icure.domain.customentities.config.typing.ListTypeConfig
import org.taktik.icure.domain.customentities.config.typing.MapTypeConfig
import org.taktik.icure.domain.customentities.config.typing.ObjectDefinition
import org.taktik.icure.domain.customentities.config.typing.SimpleBuiltinTypeConfig
import org.taktik.icure.domain.customentities.config.typing.StringTypeConfig

// TODO proper implementation with auto-generated types
@Component
class BuiltinDefinitionsProviderImpl : BuiltinDefinitionsProvider {

	override fun getBuiltinEnumDefinition(name: String): EnumDefinition? = when (name) {
		"Gender" -> EnumDefinition(
			entries = setOf("male", "female", "indeterminate", "changed", "changedToMale", "changedToFemale", "unknown")
		)
		"PersonalStatus" -> EnumDefinition(
			entries = setOf("single", "in_couple", "married", "separated", "divorced", "divorcing", "widowed", "widower", "complicated", "unknown", "contract", "other", "annulled", "polygamous")
		)
		"AddressType" -> EnumDefinition(
			entries = setOf("home", "work", "vacation", "hospital", "clinic", "hq", "other", "temporary", "postal", "diplomatic", "reference", "careaddress")
		)
		else -> null
	}

	override fun getBuiltinObjectDefinition(name: ExtendableEntityName): Map<String, ObjectDefinition.PropertyConfiguration> = when (name) {
		ExtendableEntityName.Patient -> patientDefinition
		ExtendableEntityName.Address -> addressDefinition
	}

	private companion object {
		private fun prop(type: GenericTypeConfig) = ObjectDefinition.PropertyConfiguration(type = type)

		private fun simpleBuiltin(canonicalName: String, nullable: Boolean = false) =
			SimpleBuiltinTypeConfig(typeName = canonicalName, nullable = nullable)

		private fun listOf(elementType: GenericTypeConfig, unique: Boolean = false) = ListTypeConfig(
			elementType = elementType,
			validation = if (unique) ListTypeConfig.ValidationConfig(uniqueValues = true) else null,
		)

		private val patientDefinition = mapOf(
			"identifier" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Identifier"))),
			"firstName" to prop(StringTypeConfig(nullable = true)),
			"lastName" to prop(StringTypeConfig(nullable = true)),
			"names" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.PersonName"))),
			"companyName" to prop(StringTypeConfig(nullable = true)),
			"languages" to prop(listOf(StringTypeConfig())),
			"addresses" to prop(listOf(ExtendableBuiltinTypeConfig(typeName = ExtendableEntityName.Address, nullable = false))),
			"civility" to prop(StringTypeConfig(nullable = true)),
			"gender" to prop(EnumTypeConfig(enumReference = "Gender", isBuiltIn = true, nullable = true)),
			"birthSex" to prop(EnumTypeConfig(enumReference = "Gender", isBuiltIn = true, nullable = true)),
			"mergeToPatientId" to prop(StringTypeConfig(nullable = true)),
			"mergedIds" to prop(listOf(StringTypeConfig(), unique = true)),
			"alias" to prop(StringTypeConfig(nullable = true)),
			"active" to prop(BooleanTypeConfig()),
			"deactivationReason" to prop(StringTypeConfig()),
			"deactivationDate" to prop(IntTypeConfig(nullable = true)),
			"ssin" to prop(StringTypeConfig(nullable = true)),
			"maidenName" to prop(StringTypeConfig(nullable = true)),
			"spouseName" to prop(StringTypeConfig(nullable = true)),
			"partnerName" to prop(StringTypeConfig(nullable = true)),
			"personalStatus" to prop(EnumTypeConfig(enumReference = "PersonalStatus", isBuiltIn = true, nullable = true)),
			"dateOfBirth" to prop(FuzzyDateTypeConfig(nullable = true)),
			"deceased" to prop(BooleanTypeConfig(nullable = true)),
			"dateOfDeath" to prop(FuzzyDateTypeConfig(nullable = true)),
			"timestampOfLatestEidReading" to prop(IntTypeConfig(nullable = true)),
			"placeOfBirth" to prop(StringTypeConfig(nullable = true)),
			"placeOfDeath" to prop(StringTypeConfig(nullable = true)),
			"education" to prop(StringTypeConfig(nullable = true)),
			"profession" to prop(StringTypeConfig(nullable = true)),
			"notes" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Annotation"))),
			"note" to prop(StringTypeConfig(nullable = true)),
			"administrativeNote" to prop(StringTypeConfig(nullable = true)),
			"race" to prop(StringTypeConfig(nullable = true)),
			"ethnicity" to prop(StringTypeConfig(nullable = true)),
			"nationality" to prop(StringTypeConfig(nullable = true)),
			"preferredUserId" to prop(StringTypeConfig(nullable = true)),
			"picture" to prop(StringTypeConfig(nullable = true)),
			"externalId" to prop(StringTypeConfig(nullable = true)),
			"insurabilities" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Insurability"))),
			"partnerships" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Partnership"))),
			"patientHealthCareParties" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.PatientHealthCareParty"))),
			"financialInstitutionInformation" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.FinancialInstitutionInformation"))),
			"medicalHouseContracts" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.MedicalHouseContract"))),
			"patientProfessions" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.base.CodeStub"))),
			"parameters" to prop(MapTypeConfig(valueType = listOf(StringTypeConfig()))),
			"properties" to prop(ListTypeConfig(elementType = simpleBuiltin("org.taktik.icure.entities.base.PropertyStub")))
		)

		private val addressDefinition = mapOf(
			"identifier" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Identifier"))),
			"addressType" to prop(EnumTypeConfig(enumReference = "AddressType", isBuiltIn = true, nullable = true)),
			"descr" to prop(StringTypeConfig(nullable = true)),
			"street" to prop(StringTypeConfig(nullable = true)),
			"houseNumber" to prop(StringTypeConfig(nullable = true)),
			"postboxNumber" to prop(StringTypeConfig(nullable = true)),
			"postalCode" to prop(StringTypeConfig(nullable = true)),
			"city" to prop(StringTypeConfig(nullable = true)),
			"state" to prop(StringTypeConfig(nullable = true)),
			"country" to prop(StringTypeConfig(nullable = true)),
			// Deprecated in favour of notes, included for migration compatibility
			"note" to prop(StringTypeConfig(nullable = true)),
			"notes" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Annotation"))),
			"telecoms" to prop(listOf(simpleBuiltin("org.taktik.icure.entities.embed.Telecom"))),
		)
	}
}