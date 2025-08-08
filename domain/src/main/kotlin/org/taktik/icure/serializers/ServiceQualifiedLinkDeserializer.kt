package org.taktik.icure.serializers

import org.taktik.icure.entities.base.LinkQualification

class ServiceQualifiedLinkDeserializer : EnumToMapOrListDeserializer<LinkQualification>(LinkQualification::class.java)
