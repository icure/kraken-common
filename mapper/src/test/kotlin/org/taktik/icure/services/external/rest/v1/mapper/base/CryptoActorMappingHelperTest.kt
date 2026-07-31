package org.taktik.icure.services.external.rest.v1.mapper.base

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.taktik.icure.config.CardinalVersionConfig
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.entities.base.DataOwnerGroupLinkType
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkTypeDto
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2MapperImpl

private fun parentLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.parent, id)
private fun otherLink(id: String) = DataOwnerGroupLink(DataOwnerGroupLinkType.simple, id)
private fun parentLinkDto(id: String) = DataOwnerGroupLinkDto(DataOwnerGroupLinkTypeDto.parent, id)
private fun otherLinkDto(id: String) = DataOwnerGroupLinkDto(DataOwnerGroupLinkTypeDto.simple, id)

private fun cryptoActor(parentId: String? = null, dataOwnerGroups: List<DataOwnerGroupLink> = emptyList()) =
	CryptoActorStub(id = "ca", rev = "1-abc", parentId = parentId, dataOwnerGroups = dataOwnerGroups)

private fun cryptoActorDto(parentId: String? = null, dataOwnerGroups: List<DataOwnerGroupLinkDto> = emptyList()) =
	CryptoActorStubDto(
		id = "ca",
		rev = "1-abc",
		publicKeysForOaepWithSha256 = emptySet(),
		parentId = parentId,
		dataOwnerGroups = dataOwnerGroups,
	)

private class FakeCardinalVersionConfig(private val version: SemanticVersion?) : CardinalVersionConfig {
	override suspend fun getUserCardinalVersion(): SemanticVersion? = version
	override suspend fun useLegacyDataModelCompatibility(): Boolean = true // unused by this helper
}

private val legacyConfig = FakeCardinalVersionConfig(SemanticVersion("2.0.0"))
private val noVersionConfig = FakeCardinalVersionConfig(null)
private val newConfig = FakeCardinalVersionConfig(SemanticVersion("3.0.0-PREVIEW-1"))
private val linkMapper = DataOwnerGroupLinkV2MapperImpl()

class CryptoActorMappingHelperTest : StringSpec({

	"domain to dto: empty parentId and dataOwnerGroups map to an empty pair, on any version" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(), linkMapper, legacyConfig) shouldBe (null to emptyList())
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(), linkMapper, newConfig) shouldBe (null to emptyList())
		}
	}

	"domain to dto, legacy version: a parentId alone is kept as is, with an empty links list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, legacyConfig) shouldBe ("a" to emptyList())
		}
	}

	"domain to dto, legacy version: a single parent-type link alone is moved into a parentId, with an empty links list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(dataOwnerGroups = listOf(parentLink("a"))),
				linkMapper,
				legacyConfig,
			) shouldBe ("a" to emptyList())
		}
	}

	"domain to dto, legacy version: a parentId matching an equivalent parent-type link dedups to a single parentId" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(parentId = "a", dataOwnerGroups = listOf(parentLink("a"))),
				linkMapper,
				legacyConfig,
			) shouldBe ("a" to emptyList())
		}
	}

	"domain to dto, no version configured: behaves the same as an explicit legacy version (assumed legacy)" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, noVersionConfig) shouldBe ("a" to emptyList())
		}
	}

	"domain to dto, legacy version: two distinct links cannot be represented as a single parentId and throws" {
		runBlocking {
			shouldThrow<IllegalArgumentException> {
				CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
					cryptoActor(dataOwnerGroups = listOf(parentLink("a"), otherLink("b"))),
					linkMapper,
					legacyConfig,
				)
			}
		}
	}

	"domain to dto, legacy version: a lone simple-type link cannot be represented as a legacy parentId and throws" {
		runBlocking {
			shouldThrow<IllegalArgumentException> {
				CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
					cryptoActor(dataOwnerGroups = listOf(otherLink("a"))),
					linkMapper,
					legacyConfig,
				)
			}
		}
	}

	"domain to dto, new version: a parentId alone is fully moved into the links list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActor(parentId = "a"), linkMapper, newConfig) shouldBe
				(null to listOf(parentLinkDto("a")))
		}
	}

	"domain to dto, new version: multiple links are all kept in the list, with no throw" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActor(dataOwnerGroups = listOf(parentLink("a"), otherLink("b"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(parentLinkDto("a"), otherLinkDto("b")))
		}
	}

	"dto to domain: empty parentId and dataOwnerGroups map to an empty pair, regardless of version" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActorDto(), linkMapper, legacyConfig) shouldBe (null to emptyList())
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActorDto(), linkMapper, newConfig) shouldBe (null to emptyList())
		}
	}

	"dto to domain: a parentId alone is kept in parentId only, with an empty links list, regardless of version" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActorDto(parentId = "a"), linkMapper, legacyConfig) shouldBe
				("a" to emptyList())
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(cryptoActorDto(parentId = "a"), linkMapper, newConfig) shouldBe
				("a" to emptyList())
		}
	}

	"dto to domain: a single parent-type link alone is backfilled into parentId, with an empty links list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActorDto(dataOwnerGroups = listOf(parentLinkDto("a"))),
				linkMapper,
				newConfig,
			) shouldBe ("a" to emptyList())
		}
	}

	"dto to domain: a parentId matching an equivalent parent-type link dedups and is still backfilled with an empty list" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActorDto(parentId = "a", dataOwnerGroups = listOf(parentLinkDto("a"))),
				linkMapper,
				newConfig,
			) shouldBe ("a" to emptyList())
		}
	}

	"dto to domain: a lone simple-type link is not backfilled into parentId" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActorDto(dataOwnerGroups = listOf(otherLinkDto("a"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(otherLink("a")))
		}
	}

	"dto to domain: a parentId conflicting with a simple-type link to the same id is not backfilled, no throw" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActorDto(parentId = "a", dataOwnerGroups = listOf(otherLinkDto("a"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(otherLink("a")))
		}
	}

	"dto to domain: two distinct links are not backfilled into parentId, no throw" {
		runBlocking {
			CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
				cryptoActorDto(dataOwnerGroups = listOf(parentLinkDto("a"), otherLinkDto("b"))),
				linkMapper,
				newConfig,
			) shouldBe (null to listOf(parentLink("a"), otherLink("b")))
		}
	}
})
