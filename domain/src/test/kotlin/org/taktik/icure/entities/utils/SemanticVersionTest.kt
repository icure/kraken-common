package org.taktik.icure.entities.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class SemanticVersionTest : StringSpec({
	"core components should be compared numerically" {
		listOf(
			"1.0.0" to "2.0.0",
			"1.2.0" to "1.10.0",
			"1.0.2" to "1.0.10",
		).forEach { (lower, higher) ->
			SemanticVersion(lower) shouldBeLessThan SemanticVersion(higher)
			SemanticVersion(higher) shouldBeGreaterThan SemanticVersion(lower)
		}
	}

	"a version with a suffix should precede the same version without suffix" {
		SemanticVersion("3.0.0-PREVIEW-6") shouldBeLessThan SemanticVersion("3.0.0")
		SemanticVersion("3.0.0-PREVIEW.6") shouldBeLessThan SemanticVersion("3.0.0")
	}

	"numeric suffix identifiers should be compared numerically in both conventions" {
		listOf(
			"3.0.0-PREVIEW-6" to "3.0.0-PREVIEW-10",
			"3.0.0-PREVIEW.6" to "3.0.0-PREVIEW.11",
			"3.0.0-PREVIEW-6" to "3.0.0-PREVIEW.11",
			"3.0.0-PREVIEW.6" to "3.0.0-PREVIEW-10",
			"3.0.0-RC-2-1" to "3.0.0-RC.2.2",
		).forEach { (lower, higher) ->
			SemanticVersion(lower) shouldBeLessThan SemanticVersion(higher)
			SemanticVersion(higher) shouldBeGreaterThan SemanticVersion(lower)
		}
	}

	"alphanumeric suffix identifiers should be compared lexicographically and take precedence over numeric ones" {
		SemanticVersion("3.0.0-ALPHA.2") shouldBeLessThan SemanticVersion("3.0.0-BETA.1")
		SemanticVersion("3.0.0-PREVIEW.11") shouldBeLessThan SemanticVersion("3.0.0-PREVIEW.RC")
		SemanticVersion("3.0.0-PREVIEW.11") shouldBeLessThan SemanticVersion("3.0.0-PREVIEW.11.1")
	}

	"suffixes equivalent across the two conventions should fall back to a lexicographic comparison" {
		SemanticVersion("3.0.0-PREVIEW-11").compareTo(SemanticVersion("3.0.0-PREVIEW.11")) shouldBe
			"-PREVIEW-11".compareTo("-PREVIEW.11")
	}

	"versions with the same suffix should compare as equal" {
		SemanticVersion("3.0.0-PREVIEW.11").compareTo(SemanticVersion("3.0.0-PREVIEW.11")) shouldBe 0
		SemanticVersion("3.0.0").compareTo(SemanticVersion("3.0.0")) shouldBe 0
	}
})
