/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.applications.utils

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.jar.JarFile
import java.util.jar.Manifest

/*
 * Important: leave instantiation to concrete iCure backend implementations, this way we get the jar of the concrete
 * implementation instead of the jar of the core library.
 */
abstract class JarUtils {
	private val jarPath by lazy {
		// Note : This will only work when packaged as JAR
		try {
			val jarPath =
				URI(
					this::class.java.protectionDomain.codeSource.location.path.replace("nested:", "").replace(
						"jar/\\!.+".toRegex(),
						"jar",
					),
				).path

			// Make sure we found a jar path
			if (jarPath != null && jarPath.lowercase(Locale.getDefault()).endsWith(".jar")) {
				jarPath
			} else {
				null
			}
		} catch (ignored: URISyntaxException) {
			null
		}
	}
	val manifest by lazy {
		jarPath?.let { JarFile(it) }?.use { jar ->
			try {
				jar.manifest
			} catch (ignored: IOException) {
				null
			}
		}
	}
}
