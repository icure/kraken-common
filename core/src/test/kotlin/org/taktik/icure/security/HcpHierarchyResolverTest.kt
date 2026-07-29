package org.taktik.icure.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerIdWithHierarchy
import org.taktik.icure.exceptions.IllegalEntityException

private fun hcp(id: String, parentId: String? = null, groups: List<DataOwnerGroupLink> = emptyList()) =
	HealthcareParty(id = id, parentId = parentId, dataOwnerGroups = groups)

private fun parentLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.parent, id)
private fun organisationLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.organisation, id)
private fun locationLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.location, id)

private suspend fun resolve(child: HealthcareParty, vararg others: HealthcareParty): List<String> {
	val othersById = others.associateBy { it.id }
	return resolveHcpAncestors(child) { ids -> ids.mapNotNull { othersById[it] } }.map { it.id }
}

private suspend fun resolveTree(child: HealthcareParty, vararg others: HealthcareParty): DataOwnerIdWithHierarchy {
	val othersById = others.associateBy { it.id }
	return resolveHcpHierarchyIds(child) { ids -> ids.mapNotNull { othersById[it] } }
}

private fun node(id: String, vararg parents: DataOwnerIdWithHierarchy) = DataOwnerIdWithHierarchy(id, parents.toList())

class HcpHierarchyResolverTest : StringSpec({

	"an hcp without any group link should have no ancestors" {
		resolve(hcp("a")) shouldBe emptyList()
	}

	"a legacy parentId chain should resolve to the chain ancestors" {
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"multiple parent links should all be resolved" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"a legacy parentId should be combined with the group links" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(organisationLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"a group reachable through multiple paths (diamond) should be included once and not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe listOf("b", "d", "c")
	}

	"a group that is both a direct group and a further ancestor should be included once and not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("d"))),
			hcp("b", parentId = "d"),
			hcp("d"),
		) shouldBe listOf("b", "d")
	}

	"a group linked both by the legacy parentId and a group link should be included only once" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(parentLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"membership should propagate past linked groups whatever the link type" {
		resolve(
			hcp("a", groups = listOf(organisationLink("b"))),
			hcp("b", groups = listOf(parentLink("c"))),
			hcp("c"),
		) shouldBe listOf("b", "c")
		resolve(
			hcp("a", groups = listOf(locationLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"membership should propagate through paths mixing link types" {
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(locationLink("c"))),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe listOf("b", "c", "d")
	}

	"a same group joined through links of different types should be included only once" {
		resolve(
			hcp("a", groups = listOf(locationLink("b"), organisationLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"direct self-references should be ignored" {
		resolve(hcp("a", parentId = "a")) shouldBe emptyList()
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(parentLink("b"))),
		) shouldBe listOf("b")
	}

	"the resolved ancestors should never include the child itself" {
		resolve(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		).contains("a") shouldBe false
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

	"a circular reference closed by a location link should throw" {
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
		) shouldBe listOf("b")
	}

	"the id hierarchy tree of an hcp without any group link should be a single node" {
		resolveTree(hcp("a")) shouldBe node("a")
	}

	"the id hierarchy tree should follow all links, with a group linked both by the legacy parentId and a group link appearing only once" {
		resolveTree(
			hcp("a", parentId = "b", groups = listOf(parentLink("b"), parentLink("c"), locationLink("d"))),
			hcp("b", parentId = "d"),
			hcp("c"),
			hcp("d"),
		) shouldBe node(
			"a",
			node("b", node("d")),
			node("c"),
			node("d"),
		)
	}

	"a group reachable through multiple paths (diamond) should appear once per path in the id hierarchy tree" {
		resolveTree(
			hcp("a", groups = listOf(parentLink("b"), parentLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe node(
			"a",
			node("b", node("d")),
			node("c", node("d")),
		)
	}

	"the id hierarchy tree should ignore self-references and unloadable links" {
		resolveTree(
			hcp("a", parentId = "ghost", groups = listOf(parentLink("b"))),
			hcp("b", groups = listOf(parentLink("b"), organisationLink("c"))),
			hcp("c"),
		) shouldBe node("a", node("b", node("c")))
	}

	"a circular reference should make the id hierarchy tree resolution throw" {
		shouldThrow<IllegalEntityException> {
			resolveTree(
				hcp("a", parentId = "b"),
				hcp("b", parentId = "a"),
			)
		}
	}

	"the loader should never be called twice for the same id" {
		val loadedIds = mutableListOf<String>()
		val othersById = listOf(
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		).associateBy { it.id }
		resolveHcpAncestors(hcp("a", groups = listOf(parentLink("b"), parentLink("c")))) { ids ->
			loadedIds.addAll(ids)
			ids.mapNotNull { othersById[it] }
		}
		loadedIds.sorted() shouldBe listOf("b", "c", "d")
	}
})
