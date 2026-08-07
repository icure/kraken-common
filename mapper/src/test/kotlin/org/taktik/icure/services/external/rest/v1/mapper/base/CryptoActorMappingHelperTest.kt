package org.taktik.icure.services.external.rest.v1.mapper.base

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.taktik.icure.config.CardinalVersionConfig
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2MapperImpl

private fun link(id: String) = DataOwnerGroupLink(id)
private fun linkDto(id: String) = DataOwnerGroupLinkDto(id)

private fun cryptoActor(parentId: String? = null, dataOwnerGroups: List<DataOwnerGroupLink> = emptyList()) =
	CryptoActorStub(id = "ca", rev = "1-abc", parentId = parentId, dataOwnerGroups = dataOwnerGroups)

private class FakeCardinalVersionConfig(private val version: SemanticVersion?) : CardinalVersionConfig {
	override suspend fun getUserCardinalVersion(): SemanticVersion? = version
	override suspend fun useLegacyDataModelCompatibility(): Boolean = true // unused by this helper
}

private val legacyConfig = FakeCardinalVersionConfig(SemanticVersion("2.0.0"))
private val noVersionConfig = FakeCardinalVersionConfig(null)
private val newConfig = FakeCardinalVersionConfig(SemanticVersion("3.0.0-PREVIEW-1"))
private val linkMapper = DataOwnerGroupLinkV2MapperImpl()

class CryptoActorMappingHelperTest : StringSpec({

	"empty parentId and dataOwnerGroups map to an empty pair, on any version" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(), linkMapper, legacyConfig) shouldBe (null to emptyList())
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(), linkMapper, newConfig) shouldBe (null to emptyList())
		}
	}

	"legacy version: parentId and dataOwnerGroups are passed through verbatim, with no folding" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, legacyConfig) shouldBe
				("a" to emptyList())
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(dataOwnerGroups = listOf(link("a"))),
				linkMapper,
				legacyConfig,
			) shouldBe (null to listOf(linkDto("a")))
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(parentId = "a", dataOwnerGroups = listOf(link("b"))),
				linkMapper,
				legacyConfig,
			) shouldBe ("a" to listOf(linkDto("b")))
		}
	}

	"legacy version: multiple links are passed through verbatim, with no throw" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(dataOwnerGroups = listOf(link("a"), link("b"))),
				linkMapper,
				legacyConfig,
			) shouldBe (null to listOf(linkDto("a"), linkDto("b")))
		}
	}

	"no version configured: behaves the same as an explicit legacy version (assumed legacy)" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, noVersionConfig) shouldBe
				("a" to emptyList())
		}
	}

	"new version: a parentId alone is folded into the links list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, newConfig) shouldBe
				(null to listOf(linkDto("a")))
		}
	}

	"new version: a parentId matching an equivalent dataOwnerGroups entry dedups to a single link" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(parentId = "a", dataOwnerGroups = listOf(link("a"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(linkDto("a")))
		}
	}

	"new version: multiple links are all kept in the list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(dataOwnerGroups = listOf(link("a"), link("b"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(linkDto("a"), linkDto("b")))
		}
	}
})
