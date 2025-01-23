package org.taktik.icure.asynclogic.impl

import jakarta.validation.Validation
import jakarta.validation.ValidatorFactory
import org.hibernate.validator.internal.engine.path.NodeImpl
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.DataOwnerProvider
import org.taktik.icure.validation.aspect.Fixer
import kotlin.reflect.KFunction
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

@Service
@Profile("app")
class FixerImpl (
    private val dataOwnerProvider: DataOwnerProvider,
) : Fixer {
    private val factory: ValidatorFactory = Validation
        .byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()

    private data class Fix(val fixPath: List<FixPointSelector>, val value: Any?) {
        fun behead() = this.copy(fixPath = fixPath.drop(1))
    }

    private data class FixPointSelector(val name: String, val leaf: Boolean, val iterable: Boolean, val beanInIteration: Any?)

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any> applyFixes(doc: K, fixes: List<Fix>): K {
        val docFixes = fixes.groupBy { f -> f.fixPath.first().let { Triple(it.name, it.leaf, it.iterable) } }.map { (sel, groupedFixes) ->
            val (name, leaf, iterable) = sel
            if (iterable) {
                val collection = doc::class.memberProperties.find { it.name == name }?.getter?.call(doc)
                Pair(
                    name,
                    (collection as? MutableSet<*>)?.let { applyFixOnCollection(it, groupedFixes).toMutableSet() }
                        ?: (collection as? MutableList<*>)?.let { applyFixOnCollection(it, groupedFixes).toMutableList() }
                        ?: (collection as? Set<*>)?.let { applyFixOnCollection(it, groupedFixes).toSet() }
                        ?: (collection as? Collection<*>)?.let { applyFixOnCollection(it, groupedFixes) }
                )
            } else if (!leaf) {
                val item = doc::class.memberProperties.find { it.name == name }?.getter?.call(doc)
                Pair(name, item?.let { applyFixes(it, groupedFixes.filter { f -> f.fixPath.first().beanInIteration === it }.map { f -> f.behead() }) })
            } else Pair(name, groupedFixes.first().value)
        }
        return doc::class.memberFunctions.find { it.name == "copy" }?.let { copy ->
            val args = (listOf(copy.instanceParameter!! to doc) + docFixes.mapNotNull { it: Pair<String, Any?> -> copy.findParameterByName(it.first)?.let { p -> p to it.second } }).toMap()
            (copy as? KFunction<K>)?.callBy(args) ?: doc
        } ?: doc
    }

    private fun applyFixOnCollection(items: Collection<*>, groupedFixes: List<Fix>): List<Any?> {
        return items.map {
            it?.let {
                if (groupedFixes.any { f -> f.fixPath.first().beanInIteration === it })
                    applyFixes(it, groupedFixes.filter { f -> f.fixPath.first().beanInIteration === it }.map { f -> f.behead() })
                else it
            }
        }
    }

    override suspend fun <E : Any> fix(doc: E, isCreate: Boolean): E {
        val violations = factory.validator.validate(doc)

        return violations.fold(listOf<Fix>()) { fixes, cv ->
            val annotation = cv.constraintDescriptor.annotation
            try {
                val members = annotation.annotationClass.members
                members.find {
                    it.name == "autoFix"
                }?.takeIf {
                    isCreate || (members.find { it.name == "applyOnModify" }?.call(annotation) as Boolean? != false)
                }?.let {
                    it.call(annotation) as? AutoFix
                }?.let { autoFix ->
                    if (autoFix != AutoFix.NOFIX) {
                        try {
                            val pp = cv.propertyPath.toList()
                            fixes + Fix(
                                pp.mapIndexed { idx, it ->
                                    val isLeaf = pp.size == idx + 1
                                    FixPointSelector(it.name, isLeaf, !isLeaf && pp[idx + 1].isInIterable, (it as NodeImpl).value)
                                },
                                autoFix.fix(cv.leafBean, cv.invalidValue, dataOwnerProvider)
                            )
                        } catch (e: Exception) {
                            fixes
                        }
                    } else fixes
                } ?: fixes
            } catch (e: NoSuchMethodException) { //Skip
                fixes
            }
        }.let { fixes ->
            applyFixes(doc, fixes)
        }
    }
}
