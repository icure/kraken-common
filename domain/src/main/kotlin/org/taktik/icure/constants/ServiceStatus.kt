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
package org.taktik.icure.constants

enum class ServiceStatus(val value: Int) {
	NONE(0),
	INACTIVE(1),
	IRRELEVANT(2),
	ABSENT(4),
	;

	companion object {
		private fun getValue(value: Int?, valueIfNull: Int): Int = value ?: valueIfNull

		/**
		 * Check if last bit is 0
		 */
		fun isActive(status: Int?): Boolean = isActive(status, NONE.value)

		/**
		 * Check if last bit is 0
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isActive(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and INACTIVE.value == 0

		/**
		 * Check if last bit is 1
		 */
		fun isInactive(status: Int?): Boolean = isInactive(status, NONE.value)

		/**
		 * Check if last bit is 1
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isInactive(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and INACTIVE.value != 0

		/**
		 * Check if last-but-one bit is 0
		 */
		fun isRelevant(status: Int?): Boolean = isRelevant(status, NONE.value)

		/**
		 * Check if last-but-one bit is 0
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isRelevant(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and IRRELEVANT.value == 0

		/**
		 * Check if last-but-one bit is 1
		 */
		fun isIrrelevant(status: Int?): Boolean = isIrrelevant(status, NONE.value)

		/**
		 * Check if last-but-one bit is 1
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isIrrelevant(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and IRRELEVANT.value != 0

		/**
		 * Check if last-but-two bit is 0
		 */
		fun isPresent(status: Int?): Boolean = isPresent(status, NONE.value)

		/**
		 * Check if last-but-two bit is 0
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isPresent(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and ABSENT.value == 0

		/**
		 * Check if last-but-two bit is 1
		 */
		fun isAbsent(status: Int?): Boolean = isAbsent(status, NONE.value)

		/**
		 * Check if last-but-two bit is 1
		 * @param valueIfNull value used if status is null (default 0)
		 */
		fun isAbsent(status: Int?, valueIfNull: Int): Boolean = getValue(status, valueIfNull) and ABSENT.value != 0
	}
}
