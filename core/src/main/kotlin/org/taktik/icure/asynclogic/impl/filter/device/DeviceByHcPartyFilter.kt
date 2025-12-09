package org.taktik.icure.asynclogic.impl.filter.device

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.DeviceDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.device.DeviceByHcPartyFilter
import org.taktik.icure.entities.Device
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class DeviceByHcPartyFilter(
	private val deviceDAO: DeviceDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Device, DeviceByHcPartyFilter> {
	override fun resolve(
		filter: DeviceByHcPartyFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			deviceDAO
				.listDeviceIdsByResponsible(
					datastoreInformation = datastoreInformation,
					healthcarePartyId = filter.responsibleId ?: sessionLogic.getCurrentDataOwnerIdOrNull()
						?: throw IllegalArgumentException("A DeviceByHcPartyFilter must either provide an explicit responsibleId or must be used by a data owner user"),
				).also { emitAll(it) }
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
