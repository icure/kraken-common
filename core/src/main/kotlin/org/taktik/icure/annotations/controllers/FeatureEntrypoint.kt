package org.taktik.icure.annotations.controllers

// Used by the Gradle task that compiles the catalog of views used by endpoints and filters. This needs to mark all the
// endpoints that are used for a single complex feature (e.g. agenda safe booking)
annotation class FeatureEntrypoint(val name: String)
