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
		CryptoActor.validateDataOwnerGroupLinks(emptyList(), parentId = null)
		CryptoActor.validateDataOwnerGroupLinks(listOf(parentLink("a"), otherLink("b")), parentId = null)
	}

	"the same data owner id linked twice with the same link type should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.validateDataOwnerGroupLinks(listOf(parentLink("a"), parentLink("a")), parentId = null)
		}
	}

	"the same data owner id linked twice with different link types should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.validateDataOwnerGroupLinks(listOf(parentLink("a"), otherLink("a")), parentId = null)
		}
	}

	"a parentId matching a parent-type link to the same data owner should not throw" {
		CryptoActor.validateDataOwnerGroupLinks(listOf(parentLink("a")), parentId = "a")
	}

	"a parentId with no corresponding dataOwnerGroups entry should not throw" {
		CryptoActor.validateDataOwnerGroupLinks(listOf(otherLink("b")), parentId = "a")
	}

	"a parentId matching a non-parent-type link to the same data owner should throw" {
		shouldThrow<IllegalArgumentException> {
			CryptoActor.validateDataOwnerGroupLinks(listOf(otherLink("a")), parentId = "a")
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

	"HealthcareParty should reject a parentId conflicting with a non-parent-type dataOwnerGroups link at construction" {
		shouldThrow<IllegalArgumentException> {
			HealthcareParty(id = "hcp", parentId = "a", dataOwnerGroups = listOf(otherLink("a")))
		}
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
