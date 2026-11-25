package org.taktik.icure.entities.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class AuthenticationTokenTest : StringSpec({
	fun token(creationTime: Instant, validity: Long) = AuthenticationToken(token = "token", creationTime = creationTime, validity = validity)

	"a token is valid within its validity and expired after it" {
		token(Instant.now().minusSeconds(10), 60).isExpired() shouldBe false
		token(Instant.now().minusSeconds(120), 60).isExpired() shouldBe true
	}

	"a token is expired before its creation time" {
		token(Instant.now().plusSeconds(120), 600).isExpired() shouldBe true
	}

	"a token with non-positive validity is always expired, except long-living tokens" {
		token(Instant.now().minusSeconds(10), 0).isExpired() shouldBe true
		token(Instant.now().minusSeconds(10), -5).isExpired() shouldBe true
		token(Instant.now().minusSeconds(10), AuthenticationToken.LONG_LIVING_TOKEN_VALIDITY).isExpired() shouldBe false
	}

	"a huge validity doesn't overflow" {
		token(Instant.now().minusSeconds(10), Long.MAX_VALUE).isExpired() shouldBe false
		token(Instant.MAX, Long.MAX_VALUE).isExpired() shouldBe true
	}
})
