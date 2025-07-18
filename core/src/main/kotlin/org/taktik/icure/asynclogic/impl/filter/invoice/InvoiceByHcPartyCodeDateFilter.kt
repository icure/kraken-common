/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.invoice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.InvoiceDAO
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.invoice.InvoiceByHcPartyCodeDateFilter
import org.taktik.icure.entities.Invoice

@Service
@Profile("app")
class InvoiceByHcPartyCodeDateFilter(
	private val invoiceDAO: InvoiceDAO,
	private val healthcarePartyLogic: HealthcarePartyLogic
) : Filter<String, Invoice, InvoiceByHcPartyCodeDateFilter> {

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun resolve(
        filter: InvoiceByHcPartyCodeDateFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ): Flow<String> = (filter.healthcarePartyId?.let {
		flowOf(it)
	} ?: healthcarePartyLogic.getEntityIds()).flatMapConcat { hcpId ->
		invoiceDAO.listInvoiceIdsByTarificationsByCode(
			datastoreInformation = datastoreInformation,
			hcPartyId = hcpId,
			codeCode = filter.code,
			startValueDate = filter.startInvoiceDate,
			endValueDate = filter.endInvoiceDate
		)
	}
}
