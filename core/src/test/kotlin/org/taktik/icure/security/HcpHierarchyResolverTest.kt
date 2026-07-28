package org.taktik.icure.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.exceptions.IllegalEntityException

private fun hcp(id: String, parentId: String? = null, groups: List<DataOwnerGroupLink> = emptyList()) =
	HealthcareParty(id = id, parentId = parentId, dataOwnerGroups = groups)

private fun parentLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.parent, id)
private fun organisationLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.organisation, id)
private fun locationLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.location, id)

private suspend fun resolve(child: HealthcareParty, vararg others: HealthcareParty): List<IdWithHierarchy> {
	val othersById = others.associateBy { it.id }
	return resolveHcpHierarchies(child) { ids -> ids.mapNotNull { othersById[it] } }.map { it.toHierarchyNode() }
}

class HcpHierarchyResolverTest : StringSpec({

	"an hcp without any group link should have an empty hierarchy" {
		resolve(hcp("a")) shouldBe emptyList()
	}

	"a legacy parentId chain should resolve to a single branch" {
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b", listOf(IdWithHierarchy("c"))))
	}

	"multiple parent links should produce one tree per link" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b"), IdWithHierarchy("c"))
	}

	"a legacy parentId should be combined with the group links" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(organisationLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b"), IdWithHierarchy("c"))
	}

	"a group reachable through multiple paths (diamond) should not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe listOf(
			IdWithHierarchy("b", listOf(IdWithHierarchy("d"))),
			IdWithHierarchy("c", listOf(IdWithHierarchy("d"))),
		)
	}

	"a group that is both a direct group and a further ancestor should not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("d"))),
			hcp("b", parentId = "d"),
			hcp("d"),
		) shouldBe listOf(
			IdWithHierarchy("b", listOf(IdWithHierarchy("d"))),
			IdWithHierarchy("d"),
		)
	}

	"a group linked both by the legacy parentId and a group link should be included only once" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(parentLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b", listOf(IdWithHierarchy("c"))))
	}

	"membership should propagate past groups joined through a transitive link" {
		resolve(
			hcp("a", groups = listOf(organisationLink("b"))),
			hcp("b", groups = listOf(parentLink("c"))),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b", listOf(IdWithHierarchy("c"))))
	}

	"membership should not propagate past a group joined through a non-transitive link" {
		resolve(
			hcp("a", groups = listOf(locationLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b"))
	}

	"a group joined through a non-transitive link at the end of a transitive path should be included without its own groups" {
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(locationLink("c"))),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe listOf(IdWithHierarchy("b", listOf(IdWithHierarchy("c"))))
	}

	"a same group joined through links of different transitivity should be treated as transitive" {
		resolve(
			hcp("a", groups = listOf(locationLink("b"), organisationLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf(IdWithHierarchy("b", listOf(IdWithHierarchy("c"))))
	}

	"direct self-references should be ignored" {
		resolve(hcp("a", parentId = "a")) shouldBe emptyList()
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(parentLink("b"))),
		) shouldBe listOf(IdWithHierarchy("b"))
	}

	"a circular reference should throw" {
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", parentId = "b"),
				hcp("b", parentId = "a"),
			)
		}
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", groups = listOf(parentLink("b"))),
				hcp("b", groups = listOf(organisationLink("c"))),
				hcp("c", groups = listOf(parentLink("b"))),
			)
		}
	}

	"a circular reference closed by a non-transitive link should throw" {
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", parentId = "b"),
				hcp("b", groups = listOf(locationLink("a"))),
			)
		}
	}

	"a blank link id should throw" {
		shouldThrow<IllegalEntityException> {
			resolve(hcp("a", parentId = " "))
		}
		shouldThrow<IllegalEntityException> {
			resolve(hcp("a", groups = listOf(parentLink(""))))
		}
	}

	"links to healthcare parties that cannot be loaded should be ignored" {
		resolve(
			hcp("a", parentId = "ghost", groups = listOf(parentLink("b"))),
			hcp("b", groups = listOf(organisationLink("other-ghost"))),
		) shouldBe listOf(IdWithHierarchy("b"))
	}

	"the loader should never be called twice for the same id" {
		val loadedIds = mutableListOf<String>()
		val othersById = listOf(
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		).associateBy { it.id }
		resolveHcpHierarchies(hcp("a", groups = listOf(parentLink("b"), parentLink("c")))) { ids ->
			loadedIds.addAll(ids)
			ids.mapNotNull { othersById[it] }
		}
		loadedIds.sorted() shouldBe listOf("b", "c", "d")
	}

	"containsId should find ids at any depth and nothing else" {
		val hierarchies = listOf(
			IdWithHierarchy("b", listOf(IdWithHierarchy("d", listOf(IdWithHierarchy("e"))))),
			IdWithHierarchy("c"),
		)
		hierarchies.containsId("b") shouldBe true
		hierarchies.containsId("c") shouldBe true
		hierarchies.containsId("d") shouldBe true
		hierarchies.containsId("e") shouldBe true
		hierarchies.containsId("a") shouldBe false
		emptyList<IdWithHierarchy>().containsId("b") shouldBe false
	}
})
