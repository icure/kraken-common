package org.taktik.icure.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.Article
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Device
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.DocumentTemplate
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.entities.EntityTemplate
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.Keyword
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.Place
import org.taktik.icure.entities.Receipt
import org.taktik.icure.entities.RecoveryData
import org.taktik.icure.entities.SecureDelegationKeyMap
import org.taktik.icure.entities.Tarification
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.Code

@Component
@Profile("app")
@ConfigurationProperties(prefix = "icure.entitiescache")
data class IcureEntitiesCacheProperties(
	var code: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.GLOBAL),
	var entityTemplate: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.GLOBAL),
	var healthcareParty: EntityConfiguration = EntityConfiguration(60, CacheType.GLOBAL),
	var user: EntityConfiguration = EntityConfiguration(15 * 60, CacheType.GLOBAL),
	var accessLog: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var agenda: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var article: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var calendarItem: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var calendarItemType: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var classification: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var classificationTemplate: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var contact: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var device: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var document: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var documentTemplate: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var entityReference: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var exchangeData: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var exchangeDataMap: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var form: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var formTemplate: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var healthElement: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var insurance: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var invoice: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var keyword: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var maintenanceTask: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var medicalLocation: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var message: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var patient: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var place: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var receipt: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var tarification: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var timeTable: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var recoveryData: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST),
	var secureDelegationKeyMap: EntityConfiguration = EntityConfiguration(30 * 60, CacheType.REQUEST)
) {
	data class EntityConfiguration(
		/**
		 * Time to live of the cache in seconds. Only applies if the cache is the global cache.
		 */
		var ttl: Int,
		/**
		 * Type of the cache. Can be "local", "global", or "none:.
		 */
		var type: CacheType
	) {
		init {
			require(ttl > 0) { "TTL must be greater than 0" }
		}
	}

	enum class CacheType {
		/**
		 * No caching will be done.
		 */
		NONE,

		/**
		 * The data will be cached within the scope of the request.
		 */
		REQUEST,

		/**
		 * The data will be cached "globally", rendering it available to all requests and other nodes.
		 */
		GLOBAL
	}
	
	fun getConfigurationForClass(clazz: Class<*>): EntityConfiguration = when (clazz) {
		Code::class.java -> code
		EntityTemplate::class.java -> entityTemplate
		HealthcareParty::class.java -> healthcareParty
		User::class.java -> user
		AccessLog::class.java -> accessLog
		Agenda::class.java -> agenda
		Article::class.java -> article
		CalendarItem::class.java -> calendarItem
		CalendarItemType::class.java -> calendarItemType
		Classification::class.java -> classification
		ClassificationTemplate::class.java -> classificationTemplate
		Contact::class.java -> contact
		Device::class.java -> device
		Document::class.java -> document
		DocumentTemplate::class.java -> documentTemplate
		EntityReference::class.java -> entityReference
		ExchangeData::class.java -> exchangeData
		ExchangeDataMap::class.java -> exchangeDataMap
		Form::class.java -> form
		FormTemplate::class.java -> formTemplate
		HealthElement::class.java -> healthElement
		Insurance::class.java -> insurance
		Invoice::class.java -> invoice
		Keyword::class.java -> keyword
		MaintenanceTask::class.java -> maintenanceTask
		MedicalLocation::class.java -> medicalLocation
		Message::class.java -> message
		Patient::class.java -> patient
		Place::class.java -> place
		Receipt::class.java -> receipt
		Tarification::class.java -> tarification
		TimeTable::class.java -> timeTable
		RecoveryData::class.java -> recoveryData
		SecureDelegationKeyMap::class.java -> secureDelegationKeyMap
		else -> throw IllegalArgumentException("Can't find cache configuration for class $clazz")
	}
}
