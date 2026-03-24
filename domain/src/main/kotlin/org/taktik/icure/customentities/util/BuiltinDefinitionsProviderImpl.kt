package org.taktik.icure.customentities.util

import org.springframework.stereotype.Component
import org.taktik.icure.customentities.config.typing.BooleanTypeConfig
import org.taktik.icure.customentities.config.typing.EnumTypeConfig
import org.taktik.icure.customentities.config.typing.FuzzyDateTypeConfig
import org.taktik.icure.customentities.config.typing.GenericTypeConfig
import org.taktik.icure.customentities.config.typing.IntTypeConfig
import org.taktik.icure.customentities.config.typing.ListTypeConfig
import org.taktik.icure.customentities.config.typing.MapTypeConfig
import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.customentities.config.typing.ObjectTypeConfig
import org.taktik.icure.customentities.config.typing.StringTypeConfig

// TODO proper implementation with auto-generated types
@Component
class BuiltinDefinitionsProviderImpl : BuiltinDefinitionsProvider {

	override fun getBuiltinEnumDefinition(name: String): BuiltinDefinitionsProvider.BuiltinEnumDefinition? = when (name) {
		"Gender" -> BuiltinDefinitionsProvider.BuiltinEnumDefinition(
			entries = setOf("male", "female", "indeterminate", "changed", "changedToMale", "changedToFemale", "unknown")
		)
		"PersonalStatus" -> BuiltinDefinitionsProvider.BuiltinEnumDefinition(
			entries = setOf("single", "in_couple", "married", "separated", "divorced", "divorcing", "widowed", "widower", "complicated", "unknown", "contract", "other", "annulled", "polygamous")
		)
		"AddressType" -> BuiltinDefinitionsProvider.BuiltinEnumDefinition(
			entries = setOf(
				"home",
				"work",
				"vacation",
				"hospital",
				"clinic",
				"hq",
				"other",
				"temporary",
				"postal",
				"diplomatic",
				"reference",
				"careaddress"
			)
		)
		"TelecomType" -> BuiltinDefinitionsProvider.BuiltinEnumDefinition(
			entries = setOf("mobile", "phone", "email", "fax", "skype", "im", "medibridge", "ehealthbox", "apicrypt", "web", "print", "disk", "other", "pager")
		)
		else -> null
	}

	override fun getBuiltinObjectDefinition(name: String): BuiltinDefinitionsProvider.BuiltinObjectDefinition? = when (name) {
		"Patient" -> patientDefinition
		"Address" -> addressDefinition
		"Telecom" -> telecomDefinition
		else -> null
	}

	private companion object {
		private fun prop(type: GenericTypeConfig) = ObjectDefinition.PropertyConfiguration(type = type)

		private fun simpleBuiltin(canonicalName: String, nullable: Boolean = false) =
			ObjectTypeConfig(objectReference = canonicalName, nullable = nullable, isBuiltin = true)

		private fun listOf(elementType: GenericTypeConfig, unique: Boolean = false) = ListTypeConfig(
			elementType = elementType,
			validation = if (unique) ListTypeConfig.ValidationConfig(uniqueValues = true) else null,
		)

		private val patientDefinition = mapOf(
			"responsible" to prop(StringTypeConfig(true)),
			"identifier" to prop(listOf(simpleBuiltin("Identifier"))),
			"firstName" to prop(StringTypeConfig(nullable = true)),
			"lastName" to prop(StringTypeConfig(nullable = true)),
			"names" to prop(listOf(simpleBuiltin("PersonName"))),
			"companyName" to prop(StringTypeConfig(nullable = true)),
			"languages" to prop(listOf(StringTypeConfig())),
			"addresses" to prop(listOf(simpleBuiltin("Address", nullable = false))),
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
			"notes" to prop(listOf(simpleBuiltin("Annotation"))),
			"note" to prop(StringTypeConfig(nullable = true)),
			"administrativeNote" to prop(StringTypeConfig(nullable = true)),
			"race" to prop(StringTypeConfig(nullable = true)),
			"ethnicity" to prop(StringTypeConfig(nullable = true)),
			"nationality" to prop(StringTypeConfig(nullable = true)),
			"preferredUserId" to prop(StringTypeConfig(nullable = true)),
			"picture" to prop(StringTypeConfig(nullable = true)),
			"externalId" to prop(StringTypeConfig(nullable = true)),
			"insurabilities" to prop(listOf(simpleBuiltin("Insurability"))),
			"partnerships" to prop(listOf(simpleBuiltin("Partnership"))),
			"patientHealthCareParties" to prop(listOf(simpleBuiltin("PatientHealthCareParty"))),
			"financialInstitutionInformation" to prop(listOf(simpleBuiltin("FinancialInstitutionInformation"))),
			"medicalHouseContracts" to prop(listOf(simpleBuiltin("MedicalHouseContract"))),
			"patientProfessions" to prop(listOf(simpleBuiltin("CodeStub"))),
			"parameters" to prop(MapTypeConfig(valueType = listOf(StringTypeConfig()))),
			"properties" to prop(ListTypeConfig(elementType = simpleBuiltin("PropertyStub")))
		).let {
			BuiltinDefinitionsProvider.BuiltinObjectDefinition(
				properties = it,
				isExtendable = true,
				isRoot = true
			)
		}

		private val telecomDefinition = mapOf(
			"telecomType" to prop(EnumTypeConfig(enumReference = "TelecomType", isBuiltIn = true, nullable = true)),
			"telecomNumber" to prop(StringTypeConfig(nullable = true)),
			"telecomDescription" to prop(StringTypeConfig(nullable = true)),
		).let {
			BuiltinDefinitionsProvider.BuiltinObjectDefinition(
				properties = it,
				isExtendable = true,
				isRoot = false
			)
		}

		private val addressDefinition = mapOf(
			"identifier" to prop(listOf(simpleBuiltin("Identifier"))),
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
			"notes" to prop(listOf(simpleBuiltin("Annotation"))),
			"telecoms" to prop(listOf(simpleBuiltin("Telecom"))),
		).let {
			BuiltinDefinitionsProvider.BuiltinObjectDefinition(
				properties = it,
				isExtendable = true,
				isRoot = false
			)
		}
	}
}