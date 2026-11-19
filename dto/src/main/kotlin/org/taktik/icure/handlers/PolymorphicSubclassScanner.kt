package org.taktik.icure.handlers

import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import java.lang.reflect.Modifier

/**
 * Scans the classpath location of [root] for subclasses annotated with [JsonPolymorphismRoot] and maps each of them by
 * its discriminator value ([JsonDiscriminated.value], or the simple class name if not annotated).
 *
 * This performs a full classpath scan and is expensive: call it once per root class (e.g. from a companion object),
 * never from a deserializer constructor, since Jackson may instantiate deserializers repeatedly.
 */
fun <T> scanPolymorphicSubclasses(
	root: Class<T>,
	requireAbstractRoot: Boolean = false,
): Map<String, Class<T>> {
	require(!requireAbstractRoot || Modifier.isAbstract(root.modifiers)) { "Superclass must be abstract" }
	return Reflections(root, TypeAnnotationsScanner(), SubTypesScanner())
		.getTypesAnnotatedWith(JsonPolymorphismRoot::class.java)
		.filter { root.isAssignableFrom(it) }
		.associate { subClass ->
			@Suppress("UNCHECKED_CAST")
			(subClass.getAnnotation(JsonDiscriminated::class.java)?.value ?: subClass.simpleName) to subClass as Class<T>
		}
}
