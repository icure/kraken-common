package org.taktik.icure.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.exceptions.IllegalEntityException

private fun hcp(
	id: String,
	parentId: String? = null,
	groups: List<DataOwnerGroupLink> = emptyList(),
	groupLinkType: DataOwnerGroupLinkType? = null,
) = HealthcareParty(id = id, parentId = parentId, dataOwnerGroups = groups, groupLinkType = groupLinkType)

private fun groupLink(id: String) = DataOwnerGroupLink(id)

private suspend fun resolve(child: HealthcareParty, vararg others: HealthcareParty): List<String> {
	val othersById = others.associateBy { it.id }
	return resolveHcpAncestors(child) { ids -> ids.mapNotNull { othersById[it] } }.map { it.id }
}

private suspend fun resolveTree(child: HealthcareParty, vararg others: HealthcareParty): DataOwnerHierarchyInfo {
	val othersById = others.associateBy { it.id }
	return resolveHcpHierarchyInfo(child) { ids -> ids.mapNotNull { othersById[it] } }
}

private class MembershipCheck(vararg hcps: HealthcareParty) {
	private val hcpsById = hcps.associateBy { it.id }
	val loadedIds = mutableListOf<String>()

	suspend fun membersOf(dataOwnerGroupId: String, vararg dataOwnerIds: String): Set<String> =
		filterDataOwnersMembersOf(dataOwnerIds.toSet(), dataOwnerGroupId) { ids ->
			loadedIds.addAll(ids)
			ids.mapNotNull { hcpsById[it] }
		}
}

private fun root(id: String, vararg links: DataOwnerHierarchyInfo.HierarchyNode) =
	DataOwnerHierarchyInfo(id, DataOwnerType.HCP, links.toList())

private fun link(linkType: DataOwnerGroupLinkType, id: String, vararg transitiveLinks: DataOwnerHierarchyInfo.HierarchyNode) =
	DataOwnerHierarchyInfo.HierarchyNode(id, linkType, transitiveLinks.toList())

// h0 (the child) -> h1 -> ... -> h{length}, i.e. a linear chain of `length` ancestors.
private fun linearChain(length: Int): List<HealthcareParty> = (0..length).map { i ->
	hcp("h$i", parentId = if (i < length) "h${i + 1}" else null)
}

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
			hcp("a", groups = listOf(groupLink("b"), groupLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"a legacy parentId should be combined with the group links" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(groupLink("c"))),
			hcp("b"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"a group reachable through multiple paths (diamond) should be included once and not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(groupLink("b"), groupLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe listOf("b", "d", "c")
	}

	"a group that is both a direct group and a further ancestor should be included once and not be reported as a circular reference" {
		resolve(
			hcp("a", groups = listOf(groupLink("b"), groupLink("d"))),
			hcp("b", parentId = "d"),
			hcp("d"),
		) shouldBe listOf("b", "d")
	}

	"a group linked both by the legacy parentId and a group link should be included only once" {
		resolve(
			hcp("a", parentId = "b", groups = listOf(groupLink("b"))),
			hcp("b", parentId = "c"),
			hcp("c"),
		) shouldBe listOf("b", "c")
	}

	"a link may transitively follow a stronger (or equally strong, same-type) link: strength may only stay the same or decrease going up the hierarchy" {
		// b is parent-effective (default), c is simple-effective: parent(10) -> simple(0), decreasing, allowed
		resolve(
			hcp("a", groups = listOf(groupLink("b"))),
			hcp("b", groups = listOf(groupLink("c"))),
			hcp("c", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe listOf("b", "c")
		// legacy parentId (target's default effective type is parent) -> simple: same as above
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(groupLink("c"))),
			hcp("c", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe listOf("b", "c")
		// parent(10) -> simple(0) -> simple(0): decreasing then constant same-type, allowed
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(groupLink("c"))),
			hcp("c", groups = listOf(groupLink("d")), groupLinkType = DataOwnerGroupLinkType.simple),
			hcp("d", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe listOf("b", "c", "d")
		// simple(0) -> simple(0): constant same-type, allowed
		resolve(
			hcp("a", groups = listOf(groupLink("b"))),
			hcp("b", groups = listOf(groupLink("c")), groupLinkType = DataOwnerGroupLinkType.simple),
			hcp("c", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe listOf("b", "c")
	}

	"a link stronger than the one before it on the same path makes the transitive link ambiguous and should throw" {
		// simple(0) -> parent(10): increasing, not allowed
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", groups = listOf(groupLink("b"))),
				hcp("b", groups = listOf(groupLink("c")), groupLinkType = DataOwnerGroupLinkType.simple),
				hcp("c"),
			)
		}
		// simple(0) -> legacy parentId (target's default effective type is parent, 10): same as above
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", groups = listOf(groupLink("b"))),
				hcp("b", parentId = "c", groupLinkType = DataOwnerGroupLinkType.simple),
				hcp("c"),
			)
		}
		// parent(10) -> simple(0) -> parent(10): decreasing then increasing again, not allowed
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", parentId = "b"),
				hcp("b", groups = listOf(groupLink("c"))),
				hcp("c", parentId = "d", groupLinkType = DataOwnerGroupLinkType.simple),
				hcp("d"),
			)
		}
	}

	"a single ambiguous path is enough to fail the whole resolution, even if the same target is also reachable through another, unambiguous path" {
		// a -simple-> b -parent-> d : ambiguous (simple then parent)
		// a -parent-> c -parent-> d : unambiguous (parent then parent)
		// the ambiguous path through b is enough to fail, even though d is also reachable unambiguously through c
		shouldThrow<IllegalEntityException> {
			resolve(
				hcp("a", groups = listOf(groupLink("b"), groupLink("c"))),
				hcp("b", groups = listOf(groupLink("d")), groupLinkType = DataOwnerGroupLinkType.simple),
				hcp("c", groups = listOf(groupLink("d"))),
				hcp("d"),
			)
		}
	}

	"direct self-references should be ignored" {
		resolve(hcp("a", parentId = "a")) shouldBe emptyList()
		resolve(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(groupLink("b"))),
		) shouldBe listOf("b")
	}

	"the resolved ancestors should never include the child itself" {
		resolve(
			hcp("a", groups = listOf(groupLink("b"), groupLink("c"))),
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
				hcp("a", groups = listOf(groupLink("b"))),
				hcp("b", groups = listOf(groupLink("c"))),
				hcp("c", groups = listOf(groupLink("b"))),
			)
		}
	}

	"a hierarchy of exactly the maximum size (100 ancestors) should resolve without throwing" {
		val chain = linearChain(100)
		resolve(chain.first(), *chain.drop(1).toTypedArray()) shouldBe chain.drop(1).map { it.id }
	}

	"a hierarchy exceeding the maximum size (100 ancestors) should throw" {
		shouldThrow<IllegalEntityException> {
			val chain = linearChain(101)
			resolve(chain.first(), *chain.drop(1).toTypedArray())
		}
	}

	"the maximum size applies to the total number of distinct ancestors, not to how deep any single chain is: a wide, shallow hierarchy exceeding it should throw too" {
		// 10 top-level parents, each with 9 of its own distinct further parents (100 distinct ancestors so far, only
		// 2 levels deep), plus one extra ancestor of one of those grandparents: 101 distinct ancestors total.
		val topParentIds = (1..10).map { "p$it" }
		val grandparentIdsByParent = topParentIds.associateWith { p -> (1..9).map { "$p-g$it" } }
		val extraId = "extra"
		val firstGrandparentId = grandparentIdsByParent.getValue(topParentIds.first()).first()
		val child = hcp("a", groups = topParentIds.map { groupLink(it) })
		val topHcps = topParentIds.map { p -> hcp(p, groups = grandparentIdsByParent.getValue(p).map { groupLink(it) }) }
		val grandparentHcps = grandparentIdsByParent.values.flatten().map { id ->
			hcp(id, parentId = if (id == firstGrandparentId) extraId else null)
		}
		shouldThrow<IllegalEntityException> {
			resolve(child, *(topHcps + grandparentHcps + hcp(extraId)).toTypedArray())
		}
	}

	"deeper ancestors shared across branches (diamonds) should count once towards the maximum size, not once per branch" {
		// 10 parents all linked to the SAME 20 grandparents: 30 distinct ancestors total despite 200 raw links
		// (10 x 20), well within the limit that a naive per-link count would have exceeded.
		val topParentIds = (1..10).map { "p$it" }
		val sharedGrandparentIds = (1..20).map { "g$it" }
		val child = hcp("a", groups = topParentIds.map { groupLink(it) })
		val topHcps = topParentIds.map { p -> hcp(p, groups = sharedGrandparentIds.map { groupLink(it) }) }
		val grandparentHcps = sharedGrandparentIds.map { hcp(it) }
		resolve(child, *(topHcps + grandparentHcps).toTypedArray()).size shouldBe 30
	}

	"a blank dataOwnerGroups link id should throw" {
		shouldThrow<IllegalEntityException> {
			resolve(hcp("a", groups = listOf(groupLink(""))))
		}
	}

	"a blank legacy parentId should be tolerated and treated as absent, for compatibility with legacy data" {
		resolve(hcp("a", parentId = " ")) shouldBe emptyList()
		resolve(hcp("a", parentId = "")) shouldBe emptyList()
		resolve(
			hcp("a", parentId = " ", groups = listOf(groupLink("b"))),
			hcp("b"),
		) shouldBe listOf("b")
	}

	"links to healthcare parties that cannot be loaded should be ignored" {
		resolve(
			hcp("a", parentId = "ghost", groups = listOf(groupLink("b"))),
			hcp("b", groups = listOf(groupLink("other-ghost"))),
		) shouldBe listOf("b")
	}

	"the id hierarchy tree of an hcp without any group link should be a single node" {
		resolveTree(hcp("a")) shouldBe root("a")
	}

	"the id hierarchy tree should follow all links, with a group linked both by the legacy parentId and a group link appearing only once, and carry the target's own effective type" {
		resolveTree(
			hcp("a", parentId = "b", groups = listOf(groupLink("b"), groupLink("c"), groupLink("d"))),
			hcp("b", parentId = "e"),
			hcp("c"),
			hcp("d", groupLinkType = DataOwnerGroupLinkType.simple),
			hcp("e"),
		) shouldBe root(
			"a",
			link(DataOwnerGroupLinkType.parent, "b", link(DataOwnerGroupLinkType.parent, "e")),
			link(DataOwnerGroupLinkType.parent, "c"),
			link(DataOwnerGroupLinkType.simple, "d"),
		)
	}

	"a group reachable through multiple paths (diamond) should appear once per path in the id hierarchy tree, with the same type reported on every path" {
		resolveTree(
			hcp("a", groups = listOf(groupLink("b"), groupLink("c"))),
			hcp("b", parentId = "d"),
			hcp("c", parentId = "d"),
			hcp("d"),
		) shouldBe root(
			"a",
			link(DataOwnerGroupLinkType.parent, "b", link(DataOwnerGroupLinkType.parent, "d")),
			link(DataOwnerGroupLinkType.parent, "c", link(DataOwnerGroupLinkType.parent, "d")),
		)
	}

	"a target's effective type is the same regardless of which source reaches it, fixing the old cross-actor inconsistency" {
		// Before this fix, hcp b and hcp d could each declare a link of a different type to the same target c, with
		// nothing reconciling the two. Now c's own groupLinkType is the single source of truth: both paths report it
		// identically (here, explicitly simple, to make the point with a non-default value).
		resolveTree(
			hcp("a", groups = listOf(groupLink("b"), groupLink("d"))),
			hcp("b", groups = listOf(groupLink("c"))),
			hcp("d", groups = listOf(groupLink("c"))),
			hcp("c", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe root(
			"a",
			link(DataOwnerGroupLinkType.parent, "b", link(DataOwnerGroupLinkType.simple, "c")),
			link(DataOwnerGroupLinkType.parent, "d", link(DataOwnerGroupLinkType.simple, "c")),
		)
	}

	"the id hierarchy tree should ignore self-references and unloadable links, and a transitive link may weaken from parent to simple" {
		resolveTree(
			hcp("a", parentId = "ghost", groups = listOf(groupLink("b"))),
			hcp("b", groups = listOf(groupLink("b"), groupLink("c"))),
			hcp("c", groupLinkType = DataOwnerGroupLinkType.simple),
		) shouldBe root("a", link(DataOwnerGroupLinkType.parent, "b", link(DataOwnerGroupLinkType.simple, "c")))
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
		resolveHcpAncestors(hcp("a", groups = listOf(groupLink("b"), groupLink("c")))) { ids ->
			loadedIds.addAll(ids)
			ids.mapNotNull { othersById[it] }
		}
		loadedIds.sorted() shouldBe listOf("b", "c", "d")
	}

	"the membership check should find the group through any path, at any depth" {
		// a -> b -> clinic, c -> clinic, d -> e (nowhere near the clinic), f does not exist
		val check = MembershipCheck(
			hcp("a", parentId = "b"),
			hcp("b", groups = listOf(groupLink("clinic"))),
			hcp("c", groups = listOf(groupLink("clinic"), groupLink("other"))),
			hcp("d", parentId = "e"),
			hcp("e"),
			hcp("clinic"),
			hcp("other"),
		)
		check.membersOf("clinic", "a", "c", "d", "f") shouldBe setOf("a", "c")
	}

	"the membership check should stop at the group instead of resolving the rest of the hierarchy" {
		// a -> clinic -> region -> country: nothing above the clinic has to be loaded to answer for a.
		val check = MembershipCheck(
			hcp("a", groups = listOf(groupLink("clinic"))),
			hcp("clinic", parentId = "region"),
			hcp("region", parentId = "country"),
			hcp("country"),
		)
		check.membersOf("clinic", "a") shouldBe setOf("a")
		check.loadedIds shouldBe listOf("a")
	}

	"a data owner shared by several checked data owners should be loaded and followed once, and never reported itself" {
		// a -> c, b -> c -> clinic: c answers for both a and b, and is loaded once. c is the data owner that
		// declares the link to the clinic, but the result only ever holds ids that were checked, never the ones
		// walked through to answer for them.
		val check = MembershipCheck(
			hcp("a", groups = listOf(groupLink("c"))),
			hcp("b", groups = listOf(groupLink("c"))),
			hcp("c", groups = listOf(groupLink("clinic"))),
			hcp("clinic"),
		)
		check.membersOf("clinic", "a", "b") shouldBe setOf("a", "b")
		check.loadedIds.sorted() shouldBe listOf("a", "b", "c")
	}

	"a data owner that is both checked and walked through should be reported, and only once" {
		// a -> c and c -> clinic, with both a and c checked: c answers for itself and for a.
		val check = MembershipCheck(
			hcp("a", groups = listOf(groupLink("c"))),
			hcp("c", groups = listOf(groupLink("clinic"))),
			hcp("clinic"),
		)
		check.membersOf("clinic", "a", "c") shouldBe setOf("a", "c")
		check.loadedIds.sorted() shouldBe listOf("a", "c")
	}

	"a data owner reached again at a deeper level should be followed for the newcomer, but not loaded again" {
		// b reaches shared at the first level and a only at the second: shared has to be followed for a as well,
		// or a would be wrongly reported as not being a member.
		val check = MembershipCheck(
			hcp("a", groups = listOf(groupLink("intermediate"))),
			hcp("b", groups = listOf(groupLink("shared"))),
			hcp("intermediate", groups = listOf(groupLink("shared"))),
			hcp("shared", groups = listOf(groupLink("clinic"))),
			hcp("clinic"),
		)
		check.membersOf("clinic", "a", "b") shouldBe setOf("a", "b")
		check.loadedIds.sorted() shouldBe listOf("a", "b", "intermediate", "shared")
	}

	"the membership check should tolerate what makes a full resolution fail: cycles, self references and ambiguous link types" {
		// a -> b -> a is a cycle that leads nowhere, and c reaches the clinic through a simple group that is
		// itself linked to a parent-type one, which resolveHcpAncestors rejects as ambiguous.
		val check = MembershipCheck(
			hcp("a", parentId = "b"),
			hcp("b", parentId = "a"),
			hcp("c", groups = listOf(groupLink("c"), groupLink("simpleGroup"))),
			hcp("simpleGroup", groups = listOf(groupLink("clinic")), groupLinkType = DataOwnerGroupLinkType.simple),
			hcp("clinic", groupLinkType = DataOwnerGroupLinkType.parent),
		)
		check.membersOf("clinic", "a", "c") shouldBe setOf("c")
	}

	"the membership check should never report the group itself, or a data owner with no link at all" {
		MembershipCheck(hcp("clinic"), hcp("a")).membersOf("clinic", "clinic", "a") shouldBe emptySet()
		// Not even when a path leads from the group back to itself: a group is not a member of itself, so it is
		// dropped from the checked data owners before the walk starts.
		val cyclic = MembershipCheck(
			hcp("clinic", groups = listOf(groupLink("region"))),
			hcp("region", groups = listOf(groupLink("clinic"))),
			hcp("a", groups = listOf(groupLink("region"))),
		)
		cyclic.membersOf("clinic", "clinic") shouldBe emptySet()
		cyclic.loadedIds shouldBe emptyList()
		cyclic.membersOf("clinic", "clinic", "a") shouldBe setOf("a")
	}

	"the membership check of an empty batch should not load anything" {
		val check = MembershipCheck(hcp("clinic"))
		check.membersOf("clinic") shouldBe emptySet()
		check.loadedIds shouldBe emptyList()
	}

	"the membership check should throw when it has to load more than the maximum (100) data owners per checked data owner" {
		// One checked data owner at the bottom of a chain of 101 data owners that never reaches the group.
		val chain = linearChain(101)
		shouldThrow<IllegalEntityException> {
			MembershipCheck(*chain.toTypedArray()).membersOf("nowhere", "h0")
		}
		// The limit is per checked data owner, so the same chain is affordable for a batch of two.
		MembershipCheck(*chain.toTypedArray()).membersOf("nowhere", "h0", "h1") shouldBe emptySet()
	}
})
