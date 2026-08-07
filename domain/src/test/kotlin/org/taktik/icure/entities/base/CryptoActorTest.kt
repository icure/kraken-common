package org.taktik.icure.entities.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthcareParty

private fun link(id: String) = DataOwnerGroupLink(id)

class CryptoActorTest : StringSpec({

	"an empty or fully distinct dataOwnerGroups list should not throw" {
		CryptoActor.validateDataOwnerGroupLinks(emptyList())
		CryptoActor.validateDataOwnerGroupLinks(listOf(link("a"), link("b")))
	}

	"the same data owner id linked twice should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.validateDataOwnerGroupLinks(listOf(link("a"), link("a")))
		}
	}

	"normalizedDataOwnerGroupLinks should fold a parentId with no corresponding entry in as a link" {
		CryptoActor.normalizedDataOwnerGroupLinks(listOf(link("b")), parentId = "a") shouldBe
			setOf(link("b"), link("a"))
	}

	"normalizedDataOwnerGroupLinks should not duplicate a parentId already represented as a link" {
		CryptoActor.normalizedDataOwnerGroupLinks(listOf(link("a")), parentId = "a") shouldBe setOf(link("a"))
	}

	"normalizedDataOwnerGroupLinks should leave dataOwnerGroups untouched when parentId is null" {
		CryptoActor.normalizedDataOwnerGroupLinks(listOf(link("a"), link("b")), parentId = null) shouldBe
			setOf(link("a"), link("b"))
	}

	"HealthcareParty should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			HealthcareParty(id = "hcp", dataOwnerGroups = listOf(link("a"), link("a")))
		}
	}

	"HealthcareParty should allow a dataOwnerGroups entry sharing its id with the legacy parentId" {
		HealthcareParty(id = "hcp", parentId = "a", dataOwnerGroups = listOf(link("a"))).parentId shouldBe "a"
	}

	"Device should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			Device(id = "device", dataOwnerGroups = listOf(link("a"), link("a")))
		}
	}

	"CryptoActorStub should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			CryptoActorStub(id = "stub", rev = "1-abc", dataOwnerGroups = listOf(link("a"), link("a")))
		}
	}

	"effectiveGroupLinkType should default by DataOwnerType when groupLinkType is null" {
		HealthcareParty(id = "hcp").effectiveGroupLinkType(DataOwnerType.HCP) shouldBe DataOwnerGroupLinkType.parent
		Device(id = "device").effectiveGroupLinkType(DataOwnerType.DEVICE) shouldBe DataOwnerGroupLinkType.notAllowed
		CryptoActorStub(id = "stub", rev = "1-abc").effectiveGroupLinkType(DataOwnerType.PATIENT) shouldBe DataOwnerGroupLinkType.notAllowed
	}

	"effectiveGroupLinkType should use the explicit groupLinkType when set, overriding the type default" {
		HealthcareParty(id = "hcp", groupLinkType = DataOwnerGroupLinkType.simple).effectiveGroupLinkType(DataOwnerType.HCP) shouldBe
			DataOwnerGroupLinkType.simple
		Device(id = "device", groupLinkType = DataOwnerGroupLinkType.parent).effectiveGroupLinkType(DataOwnerType.DEVICE) shouldBe
			DataOwnerGroupLinkType.parent
		HealthcareParty(id = "hcp", groupLinkType = DataOwnerGroupLinkType.notAllowed).effectiveGroupLinkType(DataOwnerType.HCP) shouldBe
			DataOwnerGroupLinkType.notAllowed
	}
})
