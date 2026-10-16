package org.taktik.icure.asyncdao.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AesExchangeKeyViewValueTest :
	StringSpec({
		// Same mapper and parsing method as the ones used by default by the couchdb client to read the view rows values
		val mapper = ObjectMapper().registerKotlinModule()

		fun read(json: String): AesExchangeKeyViewValue? = mapper.createParser(json).readValueAs(AesExchangeKeyViewValue::class.java)

		"A row value with the expected shape should be deserialized" {
			read("""["owner", "ownerFp", "delegateFp", "aa"]""") shouldBe AesExchangeKeyViewValue(
				ownerId = "owner",
				ownerPublicKeyFingerprint = "ownerFp",
				delegatePublicKeyFingerprint = "delegateFp",
				encryptedExchangeKey = "aa",
			)
		}

		"A row value emitted from the legacy hcPartyKeys should be deserialized" {
			read("""["owner", "ownerFp", "", "aa"]""") shouldBe AesExchangeKeyViewValue(
				ownerId = "owner",
				ownerPublicKeyFingerprint = "ownerFp",
				delegatePublicKeyFingerprint = "",
				encryptedExchangeKey = "aa",
			)
		}

		"Number and boolean elements of a row value should be kept using their textual representation" {
			read("""["owner", "ownerFp", true, 12]""") shouldBe AesExchangeKeyViewValue(
				ownerId = "owner",
				ownerPublicKeyFingerprint = "ownerFp",
				delegatePublicKeyFingerprint = "true",
				encryptedExchangeKey = "12",
			)
		}

		"A row value with a null element should be discarded" {
			read("""["owner", "ownerFp", "", null]""") shouldBe null
			read("""["owner", null, "delegateFp", "aa"]""") shouldBe null
		}

		"A row value with an element of the wrong shape should be discarded" {
			read("""["owner", "ownerFp", "delegateFp", { "x": "y" }]""") shouldBe null
			read("""["owner", "ownerFp", ["delegateFp"], "aa"]""") shouldBe null
		}

		"A row value with the wrong number of elements should be discarded" {
			read("""["owner", "ownerFp", ""]""") shouldBe null
			read("""["owner", "ownerFp", "delegateFp", "aa", "bb"]""") shouldBe null
		}

		"A row value that is not an array should be discarded" {
			read("""null""") shouldBe null
			read(""""owner"""") shouldBe null
			read("""{ "ownerId": "owner" }""") shouldBe null
		}

		"Row values should be grouped by owner, owner public key fingerprint and delegate public key fingerprint" {
			listOf(
				AesExchangeKeyViewValue("owner1", "ownerFp1", "delegateFp1", "k1"),
				AesExchangeKeyViewValue("owner1", "ownerFp1", "delegateFp2", "k2"),
				AesExchangeKeyViewValue("owner1", "ownerFp2", "", "k3"),
				AesExchangeKeyViewValue("owner2", "ownerFp3", "delegateFp1", "k4"),
			).toAesExchangeKeysMap() shouldBe mapOf(
				"owner1" to mapOf(
					"ownerFp1" to mapOf("delegateFp1" to "k1", "delegateFp2" to "k2"),
					"ownerFp2" to mapOf("" to "k3"),
				),
				"owner2" to mapOf(
					"ownerFp3" to mapOf("delegateFp1" to "k4"),
				),
			)
		}

		"Owner public keys should be reduced to their fingerprint" {
			val spki = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100c3a1b2" + "0be7c82b3098e1250de5a90203010001"
			listOf(
				AesExchangeKeyViewValue("owner", spki, "delegateFp", "k1"),
				AesExchangeKeyViewValue("owner", "0be7c82b3098e1250de5a90203010001", "", "k2"),
			).toAesExchangeKeysMap() shouldBe mapOf(
				"owner" to mapOf(
					"0be7c82b3098e1250de5a90203010001" to mapOf("delegateFp" to "k1", "" to "k2"),
				),
			)
		}

		"The last row value should be kept when the same entry is emitted more than once" {
			listOf(
				AesExchangeKeyViewValue("owner", "ownerFp", "delegateFp", "k1"),
				AesExchangeKeyViewValue("owner", "ownerFp", "delegateFp", "k2"),
			).toAesExchangeKeysMap() shouldBe mapOf(
				"owner" to mapOf("ownerFp" to mapOf("delegateFp" to "k2")),
			)
		}
	})
