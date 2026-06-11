package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.ConfigurationView
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyCodeFilter
import org.taktik.icure.entities.HealthElement
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByHcPartyCodeFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, HealthElement, HealthElementByHcPartyCodeFilter> {
	override val configurationViews = listOf(
		ConfigurationView("HealthElement", "by_all_delegates_code_date_map"),
		ConfigurationView("HealthElement", "by_all_delegates_code_map"),
	)

	override fun resolve(
		filter: HealthElementByHcPartyCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			emitAll(
				healthElementDAO.listHealthElementIdsByHcPartyAndCodesAndValueDateAndVersioning(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId),
					codeType = filter.codeType,
					codeCode = filter.codeCode,
					startValueDate = filter.startOfHealthElementDate,
					endValueDate = filter.endOfHealthElementDate,
					filterVersion = filter.versionFiltering ?: VersionFiltering.ANY,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}

