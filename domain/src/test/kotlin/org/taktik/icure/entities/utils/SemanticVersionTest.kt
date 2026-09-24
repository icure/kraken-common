package org.taktik.icure.entities.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.TreeMap

class SemanticVersionTest : StringSpec({
	fun v(version: String) = SemanticVersion(version)

	fun List<String>.shouldBeStrictlyIncreasing() = zipWithNext().forEach { (lower, higher) ->
		v(lower) shouldBeLessThan v(higher)
	}

	"major, minor and patch are compared numerically" {
		listOf("1.9.9", "1.10.0", "2.0.0", "2.0.10", "2.1.0", "10.0.0").shouldBeStrictlyIncreasing()
	}

	"a release is greater than its pre-releases" {
		listOf("2.13.0", "3.0.0-preview-1", "3.0.0-preview-6", "3.0.0").shouldBeStrictlyIncreasing()
	}

	"numeric pre-release identifiers are compared numerically" {
		listOf(
			"3.0.0-preview-2",
			"3.0.0-preview-6",
			"3.0.0-preview-9",
			"3.0.0-preview-10",
			"3.0.0-preview-59",
			"3.0.0-preview-100"
		).shouldBeStrictlyIncreasing()
		listOf("1.0.0-rc2", "1.0.0-rc9", "1.0.0-rc10").shouldBeStrictlyIncreasing()
	}

	"pre-releases follow the semantic versioning precedence" {
		listOf(
			"1.0.0-alpha",
			"1.0.0-alpha.1",
			"1.0.0-alpha.beta",
			"1.0.0-beta",
			"1.0.0-beta.2",
			"1.0.0-beta.11",
			"1.0.0-rc.1",
			"1.0.0"
		).shouldBeStrictlyIncreasing()
	}

	"separators and leading zeros do not change the precedence, build metadata is ignored" {
		v("3.0.0-preview-6") shouldBeEqualComparingTo v("3.0.0-preview.6")
		v("3.0.0-preview-06") shouldBeEqualComparingTo v("3.0.0-preview-6")
		v("3.0.0+build.5") shouldBeEqualComparingTo v("3.0.0")
		v("3.0.0-preview-6+abc") shouldBeEqualComparingTo v("3.0.0-preview-6")
	}

	"floor lookups pick the latest configuration whose minimum version is not above the requested one" {
		val configs = TreeMap<SemanticVersion, String>().apply {
			put(v("2.0.0"), "2.0.0")
			put(v("2.13.0"), "2.13.0")
			put(v("3.0.0-preview-6"), "3.0.0-preview-6")
		}
		configs.floorEntry(v("3.0.0-preview-5"))?.value shouldBe "2.13.0"
		configs.floorEntry(v("3.0.0-preview-6"))?.value shouldBe "3.0.0-preview-6"
		configs.floorEntry(v("3.0.0-preview-10"))?.value shouldBe "3.0.0-preview-6"
		configs.floorEntry(v("3.0.0-preview-59"))?.value shouldBe "3.0.0-preview-6"
		configs.floorEntry(v("3.0.0"))?.value shouldBe "3.0.0-preview-6"
		configs.floorEntry(v("1.9.0")) shouldBe null
	}

	"equality is based on the version string" {
		v("3.0.0-preview-6") shouldBe v("3.0.0-preview-6")
		v("3.0.0-preview-6").hashCode() shouldBe v("3.0.0-preview-6").hashCode()
		v("3.0.0-preview-6") shouldNotBe v("3.0.0-preview.6")
	}

	"invalid versions are rejected" {
		shouldThrow<IllegalArgumentException> { v("3.0").compareTo(v("3.0.0")) }
		shouldThrow<NumberFormatException> { v("a.b.c").compareTo(v("3.0.0")) }
	}
})
