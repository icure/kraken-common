/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient

/**
 * Retrieves all the [Patient]s with a delegation for [dataOwnerId] that :
 *
 * - Have at least one tag with the specified [tagType] if the [tagCode] is null, or
 * - Have at list one tag with the specified [tagType] and [tagCode] if not null.
 *
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByDataOwnerTagFilter : Filter<String, Patient> {
	val dataOwnerId: String
	val tagType: String
	val tagCode: String?
}
