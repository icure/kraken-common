package org.taktik.icure.entities.designdoc

data class DesignDocSchemaStub(
	val id: String,
	val rev: String?,
	val applicationGroupId: String,
	val version: Int,
	val viewsByEntity: Map<String, Map<String, Int>>,
	val viewsNotByDataOwner: Map<String, Set<String>> = emptyMap(),
	val customViewsVersion: Int? = null,
) {

	companion object {
		fun fromSchema(schema: DesignDocSchema): DesignDocSchemaStub = DesignDocSchemaStub(
			id = schema.id,
			rev = schema.rev,
			applicationGroupId = schema.applicationGroupId,
			version = schema.version,
			viewsByEntity = schema.viewsByEntity,
			viewsNotByDataOwner = schema.viewsNotByDataOwner,
			customViewsVersion = schema.customViewsVersion
		)
	}

}