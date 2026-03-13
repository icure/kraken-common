package org.taktik.icure.entities.embed

enum class PricingDomain {
	ambulatory, // Tarification for ambulatory care (ex: nomenclature INAMI)
	hospital, // Tarification for hospital care (ex: nomenclature INAMI)
	both // Tarification for both ambulatory and hospital care (ex: nomenclature INAMI)
}
