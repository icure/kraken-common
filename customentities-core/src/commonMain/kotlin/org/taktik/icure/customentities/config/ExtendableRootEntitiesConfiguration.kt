package org.taktik.icure.customentities.config

interface ExtendableRootEntitiesConfiguration<T : Any> {
	val accessLog: T?
	val agenda: T?
	val calendarItem: T?
	val calendarItemType: T?
	val contact: T?
	val device: T?
	val document: T?
	val healthcareParty: T?
	val healthElement: T?
	val message: T?
	val patient: T?
	val place: T?
	val user: T?
	val topic: T?
}

val <T : Any> ExtendableRootEntitiesConfiguration<T>.allDefined get(): List<Pair<String, T>> = listOfNotNull(
	accessLog?.let { "AccessLog" to it },
	agenda?.let { "Agenda" to it },
	calendarItem?.let { "CalendarItem" to it },
	calendarItemType?.let { "CalendarItemType" to it },
	contact?.let { "Contact" to it },
	device?.let { "Device" to it },
	document?.let { "Document" to it },
	healthcareParty?.let { "HealthcareParty" to it },
	healthElement?.let { "HealthElement" to it },
	message?.let { "Message" to it },
	patient?.let { "Patient" to it },
	place?.let { "Place" to it },
	user?.let { "User" to it },
	topic?.let { "Topic" to it },
)

