package org.taktik.icure.entities.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.HealthcareParty

private fun parentLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.parent, id)
private fun otherLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.simple, id)

class CryptoActorTest : StringSpec({

	"an empty or fully distinct dataOwnerGroups list should not throw" {
		CryptoActor.requireNoDuplicateDataOwnerGroupLinks(emptyList())
		CryptoActor.requireNoDuplicateDataOwnerGroupLinks(listOf(parentLink("a"), otherLink("b")))
	}

	"the same data owner id linked twice with the same link type should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.requireNoDuplicateDataOwnerGroupLinks(listOf(parentLink("a"), parentLink("a")))
		}
	}

	"the same data owner id linked twice with different link types should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.requireNoDuplicateDataOwnerGroupLinks(listOf(parentLink("a"), otherLink("a")))
		}
	}

	"HealthcareParty should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			HealthcareParty(id = "hcp", dataOwnerGroups = listOf(parentLink("a"), otherLink("a")))
		}
	}

	"HealthcareParty should allow a dataOwnerGroups entry sharing its id with the legacy parentId" {
		HealthcareParty(id = "hcp", parentId = "a", dataOwnerGroups = listOf(parentLink("a"))).parentId shouldBe "a"
	}

	"Device should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			Device(id = "device", dataOwnerGroups = listOf(parentLink("a"), otherLink("a")))
		}
	}

	"CryptoActorStub should reject a dataOwnerGroups list with a duplicate id at construction" {
		shouldThrow<IllegalArgumentException> {
			CryptoActorStub(id = "stub", rev = "1-abc", dataOwnerGroups = listOf(parentLink("a"), otherLink("a")))
		}
	}
})
