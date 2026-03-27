package org.taktik.icure.customentities.config.migration

data class MigrationConfiguration(
	val owningGroup: String,
	val sourceConfigurationVersion: Int?,
	val targetConfigurationVersion: Int,
	val enumMigrations: List<EnumMigration> = emptyList(),
	val objectMigrations: List<ObjectMigration> = emptyList(),
)