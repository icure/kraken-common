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

private const val DEFAULT_CACHE_TTL_SECONDS = 30 * 60
private const val DEFAULT_HCP_CACHE_TTL_SECONDS = 1 * 60
private const val DEFAULT_USER_CACHE_TTL_SECONDS = 15 * 60

@Component
@Profile("app")
@ConfigurationProperties(prefix = "icure.entitiescache")
final data class IcureEntitiesCacheProperties(
	var code: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var entityTemplate: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var healthcareParty: EntityConfiguration = EntityConfiguration(DEFAULT_HCP_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var user: EntityConfiguration = EntityConfiguration(DEFAULT_USER_CACHE_TTL_SECONDS, CacheType.GLOBAL),
	var accessLog: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var agenda: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var article: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var calendarItem: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var calendarItemType: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var classification: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var classificationTemplate: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var contact: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var device: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var document: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var documentTemplate: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var entityReference: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var exchangeData: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var exchangeDataMap: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var form: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var formTemplate: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var healthElement: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var insurance: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var invoice: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var keyword: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var maintenanceTask: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var medicalLocation: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var message: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var patient: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var place: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var receipt: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var tarification: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var timeTable: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var recoveryData: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	var secureDelegationKeyMap: EntityConfiguration = EntityConfiguration(DEFAULT_CACHE_TTL_SECONDS, CacheType.REQUEST),
	// When adding also add to allConfigurationsByClassName
) {
	data class EntityConfiguration(
		/**
		 * Time to live of the cache in seconds. Only applies if the cache is the global cache.
		 */
		var ttl: Int,
		/**
		 * Type of the cache. Can be "local", "global", or "none:.
		 */
		var type: CacheType,
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
		GLOBAL,
	}

	val allConfigurationsByClassName: Map<String, EntityConfiguration> by lazy {
		mapOf(
			Code::class.java.name to code,
			EntityTemplate::class.java.name to entityTemplate,
			HealthcareParty::class.java.name to healthcareParty,
			User::class.java.name to user,
			AccessLog::class.java.name to accessLog,
			Agenda::class.java.name to agenda,
			Article::class.java.name to article,
			CalendarItem::class.java.name to calendarItem,
			CalendarItemType::class.java.name to calendarItemType,
			Classification::class.java.name to classification,
			ClassificationTemplate::class.java.name to classificationTemplate,
			Contact::class.java.name to contact,
			Device::class.java.name to device,
			Document::class.java.name to document,
			DocumentTemplate::class.java.name to documentTemplate,
			EntityReference::class.java.name to entityReference,
			ExchangeData::class.java.name to exchangeData,
			ExchangeDataMap::class.java.name to exchangeDataMap,
			Form::class.java.name to form,
			FormTemplate::class.java.name to formTemplate,
			HealthElement::class.java.name to healthElement,
			Insurance::class.java.name to insurance,
			Invoice::class.java.name to invoice,
			Keyword::class.java.name to keyword,
			MaintenanceTask::class.java.name to maintenanceTask,
			MedicalLocation::class.java.name to medicalLocation,
			Message::class.java.name to message,
			Patient::class.java.name to patient,
			Place::class.java.name to place,
			Receipt::class.java.name to receipt,
			Tarification::class.java.name to tarification,
			TimeTable::class.java.name to timeTable,
			RecoveryData::class.java.name to recoveryData,
			SecureDelegationKeyMap::class.java.name to secureDelegationKeyMap,
		)
	}

	fun getConfigurationForClass(clazz: Class<*>): EntityConfiguration = allConfigurationsByClassName[clazz.name] ?: throw IllegalArgumentException("Can't find cache configuration for class $clazz")

	fun getConfigurationForName(name: String): EntityConfiguration = allConfigurationsByClassName[name] ?: throw IllegalArgumentException("Can't find cache configuration for name $name")
}
