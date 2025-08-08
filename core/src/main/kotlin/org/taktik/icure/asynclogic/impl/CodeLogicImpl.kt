/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.commons.beanutils.PropertyUtilsBean
import org.apache.commons.logging.LogFactory
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.EnumVersion
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.aspect.Fixer
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.*
import javax.xml.parsers.SAXParserFactory
import kotlin.coroutines.coroutineContext

open class CodeLogicImpl(
	protected val codeDAO: CodeDAO,
	filters: Filters,
	datastoreInstanceProvider: org.taktik.icure.datastore.DatastoreInstanceProvider,
	fixer: Fixer,
) : GenericLogicImpl<Code, CodeDAO>(fixer, datastoreInstanceProvider, filters),
	CodeLogic {
	companion object {
		private val log = LogFactory.getLog(this::class.java)
	}

	val objectMapper: ObjectMapper by lazy {
		ObjectMapper().registerModule(
			KotlinModule
				.Builder()
				.configure(KotlinFeature.NullIsSameAsDefault, enabled = false)
				.withReflectionCacheSize(reflectionCacheSize = 512)
				.configure(KotlinFeature.NullToEmptyMap, enabled = false)
				.configure(KotlinFeature.NullToEmptyCollection, enabled = false)
				.configure(KotlinFeature.SingletonSupport, false)
				.configure(KotlinFeature.StrictNullChecks, false)
				.build(),
		)
	}

	override fun getTagTypeCandidates(): List<String> = listOf("CD-ITEM", "CD-PARAMETER", "CD-CAREPATH", "CD-SEVERITY", "CD-URGENCY", "CD-GYNECOLOGY")

	override fun getRegions(): List<String> = listOf("fr", "be")

	override suspend fun get(id: String) = getEntity(id)

	override suspend fun get(
		type: String,
		code: String,
		version: String,
	): Code? {
		val datastoreInformation = getInstanceAndGroup()
		return codeDAO.get(datastoreInformation, "$type|$code|$version")
	}

	override fun getCodes(ids: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(codeDAO.getEntities(datastoreInformation, ids))
	}

	override suspend fun create(code: Code) = fix(code, isCreate = true) { fixedCode ->
		if (fixedCode.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		fixedCode.code ?: error("Code field is null")
		fixedCode.type ?: error("Type field is null")
		fixedCode.version ?: error("Version field is null")

		val datastoreInformation = getInstanceAndGroup()
		codeDAO.create(
			datastoreInformation,
			fixedCode.copy(id = fixedCode.type + "|" + fixedCode.code + "|" + fixedCode.version),
		)
	}

	protected fun validateForCreation(batch: List<Code>) = batch.fold(setOf<Code>()) { acc, code ->
		// First, I check that all the codes are valid
		code.code ?: error("Code field is null")
		code.type ?: error("Type field is null")
		code.version ?: error("Version field is null")

		if (acc.contains(code)) error("Batch contains duplicate elements. id: ${code.type}|${code.code}|${code.version}")

		acc + code.copy(id = code.type + "|" + code.code + "|" + code.version)
	}

	// Do we need fix? No annotations on code
	override suspend fun create(batch: List<Code>) = validateForCreation(batch)
		.also { codeList ->
			this.getCodes(codeList.map { it.id }).firstOrNull()?.let { duplicatedCode ->
				error("Code with id ${duplicatedCode.id} already exists")
			}
		}.let { codes ->
			createEntities(codes)
		}.toList()

	override suspend fun modify(code: Code) = fix(code, isCreate = false) { fixedCode ->
		modifyEntities(setOf(fixedCode)).firstOrNull()
	}

	// Do we need fix? No annotations on code
	override fun modify(batch: List<Code>) = flow {
		emitAll(
			modifyEntities(
				validateForModification(batch)
					.also { codeList ->
						if (getCodes(codeList.map { it.id }).count() !=
							batch.size
						) {
							error("You are trying to modify a code that does not exists")
						}
					}.toSet(),
			),
		)
	}

	protected fun validateForModification(batch: List<Code>) = batch
		.fold(mapOf<String, Code>()) { acc, code ->
			// First, I check that all the codes are valid
			code.code ?: error("Code field is null")
			code.type ?: error("Type field is null")
			code.version ?: error("Version field is null")
			code.rev ?: error("rev field is null")

			if (code.id !=
				"${code.type}|${code.code}|${code.version}"
			) {
				error("Code id does not match the code, type or version value")
			}
			if (acc.contains(code.id)) error("The batch contains a duplicate")

			acc + (code.id to code)
		}.map {
			it.value
		}

	override fun listCodeTypesBy(
		region: String?,
		type: String?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(codeDAO.listCodeTypesByRegionAndType(datastoreInformation, region, type))
	}

	override fun findCodesBy(
		type: String?,
		code: String?,
		version: String?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(codeDAO.listCodesBy(datastoreInformation, type, code, version))
	}

	override fun findCodesBy(
		region: String?,
		type: String?,
		code: String?,
		version: String?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(codeDAO.listCodesBy(datastoreInformation, region, type, code, version))
	}

	override fun findCodesBy(
		region: String?,
		type: String?,
		code: String?,
		version: String?,
		paginationOffset: PaginationOffset<List<String?>>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			codeDAO
				.findCodesBy(
					datastoreInformation,
					region,
					type,
					code,
					version,
					paginationOffset.limitIncludingKey(),
				).toPaginatedFlow<Code>(paginationOffset.limit),
		)
	}

	override fun findCodesByLabel(
		region: String?,
		language: String,
		types: Set<String>,
		label: String,
		version: String?,
		paginationOffset: PaginationOffset<List<String?>>,
	) = flow {
		val codeTypeStartKey =
			paginationOffset.startKey?.let { requireNotNull(it[1]) { "Code type in pagination offset can't be null" } }
		val datastoreInformation = getInstanceAndGroup()
		var emitted = 0

		types
			.dropWhile {
				// Drop all types that we have already fully emitted according to pagination offset
				// Only works if between pages the order of types is preserved
				codeTypeStartKey != null && it != codeTypeStartKey
			}.forEach { type ->
				if (emitted < (paginationOffset.limit + 1)) {
					val offset =
						if (codeTypeStartKey == null || codeTypeStartKey == type) {
							paginationOffset
						} else {
							paginationOffset.copy(startKey = null, startDocumentId = null)
						}
					emitAll(
						codeDAO
							.findCodesByLabel(
								datastoreInformation,
								region,
								language,
								type,
								label,
								version,
								offset.limitIncludingKey(),
							).onEach {
								if (it is ViewRowWithDoc<*, *, *>) emitted++
							},
					)
				}
			}
	}.toPaginatedFlow<Code>(paginationOffset.limit)

	override fun listCodeIdsByTypeCodeVersionInterval(
		startType: String?,
		startCode: String?,
		startVersion: String?,
		endType: String?,
		endCode: String?,
		endVersion: String?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			codeDAO.listCodeIdsByTypeCodeVersionInterval(
				datastoreInformation,
				startType,
				startCode,
				startVersion,
				endType,
				endCode,
				endVersion,
			),
		)
	}

	override fun findCodesByQualifiedLinkId(
		linkType: String,
		linkedId: String?,
		pagination: PaginationOffset<List<String>>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			codeDAO
				.findCodesByQualifiedLinkId(
					datastoreInformation,
					linkType,
					linkedId,
					pagination.limitIncludingKey(),
				).toPaginatedFlow<Code>(pagination.limit),
		)
	}

	override suspend fun <T : Enum<*>> importCodesFromEnum(e: Class<T>) {
		// TODO: rewrite this
		val version = "" + e.getAnnotation(EnumVersion::class.java).value

		val regions = getRegions().toSet()
		val codes = HashMap<String, Code>()
		findCodesBy(e.name, null, null).filter { c -> c.version == version }.onEach { c -> codes[c.id] = c }.collect()

		try {
			@Suppress("UNCHECKED_CAST")
			for (t in e.getMethod("values").invoke(null) as Array<T>) {
				val newCode =
					Code
						.from(e.name, t.name, version)
						.copy(regions = regions, label = ImmutableMap.of("en", t.name.replace("_".toRegex(), " ")))
				if (!codes.values.contains(newCode)) {
					if (!codes.containsKey(newCode.id)) {
						create(newCode)
					} else {
						val modCode = newCode.copy(rev = codes[newCode.id]!!.rev)
						if (codes[newCode.id] != modCode) {
							try {
								modify(modCode)
							} catch (ex2: Exception) {
								log.info("Could not create code " + e.name, ex2)
							}
						}
					}
				}
			}
		} catch (ex: IllegalAccessException) {
			throw IllegalStateException(ex)
		} catch (ex: InvocationTargetException) {
			throw IllegalStateException(ex)
		} catch (ex: NoSuchMethodException) {
			throw IllegalStateException(ex)
		}
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun importCodesFromXml(
		md5: String,
		type: String,
		stream: InputStream,
	) {
		val check = getCodes(listOf(Code.from("ICURE-SYSTEM", md5, version = "1").id)).toList()

		if (check.isEmpty()) {
			val coroutineScope = CoroutineScope(coroutineContext)

			val factory = SAXParserFactory.newInstance()
			val saxParser = factory.newSAXParser()

			val stack = LinkedList<Code>()

			val batchSave: suspend (Code?, Boolean?) -> Unit = { code, flush ->
				code?.let { stack.add(it) }
				if (stack.size == 100 || flush == true) {
					val existings =
						getCodes(stack.map { it.id }).fold(HashMap<String, Code>()) { map, c ->
							map[c.id] = c
							map
						}
					val datastoreInformation = getInstanceAndGroup()
					codeDAO
						.saveBulk(
							datastoreInformation,
							stack.map { xc ->
								existings[xc.id]?.let { xc.copy(rev = it.rev) } ?: xc
							},
						).onEach {
							if (it is BulkSaveResult.Success) {
								log.debug("Code: ${it.entity.id} from file $type.$md5.xml is saved")
							}
						}.filter { it !is BulkSaveResult.Success }
						.count()
						.also {
							log.error("$it conflicts")
						}
					stack.clear()
				}
			}

			val handler =
				object : DefaultHandler() {
					var initialized = false
					var version: String = "1.0"
					var charsHandler: ((chars: String) -> Unit)? = null
					var code: MutableMap<String, Any> = mutableMapOf()
					var characters: String = ""

					override fun characters(
						ch: CharArray?,
						start: Int,
						length: Int,
					) {
						ch?.let { characters += String(it, start, length) }
					}

					override fun startElement(
						uri: String?,
						localName: String?,
						qName: String?,
						attributes: Attributes?,
					) {
						if (!initialized && qName != "kmehr-cd") {
							throw IllegalArgumentException("Not supported")
						}
						initialized = true
						characters = ""
						qName?.let {
							when (it.uppercase()) {
								"VERSION" ->
									charsHandler = {
										version = it
									}

								"VALUE" -> {
									code =
										mutableMapOf(
											"type" to type,
											"version" to version,
											"label" to mapOf<String, String>(),
											"regions" to setOf<String>(),
										)
								}

								"CODE" -> charsHandler = { code["code"] = it }
								"PARENT" ->
									charsHandler = {
										code["qualifiedLinks"] =
											mapOf(LinkQualification.parent.name to listOf("$type|$it|$version"))
									}

								"DESCRIPTION" ->
									charsHandler = {
										attributes?.getValue("L")?.let { attributesValue ->
											code["label"] =
												(code["label"] as Map<String, String>) + (attributesValue to it.trim())
										}
									}

								"REGIONS" ->
									charsHandler =
										{ code["regions"] = (code["regions"] as Set<String>) + it.trim() }

								else -> {
									charsHandler = null
								}
							}
						}
					}

					override fun endElement(
						uri: String?,
						localName: String?,
						qName: String?,
					) {
						charsHandler?.let { it(characters) }
						qName?.let {
							when (it.uppercase()) {
								"VALUE" -> {
									runBlocking(coroutineScope.coroutineContext) {
										code["id"] =
											"${code["type"] as String}|${code["code"] as String}|${code["version"] as String}"
										batchSave(Code(args = code), false)
									}
								}

								else -> null
							}
						}
					}
				}

			val beThesaurusHandler =
				object : DefaultHandler() {
					var initialized = false
					var version: String = "1.0"
					var charsHandler: ((chars: String) -> Unit)? = null
					var code: MutableMap<String, Any> = mutableMapOf()
					var characters: String = ""

					override fun characters(
						ch: CharArray?,
						start: Int,
						length: Int,
					) {
						ch?.let { characters += String(it, start, length) }
					}

					override fun startElement(
						uri: String?,
						localName: String?,
						qName: String?,
						attributes: Attributes?,
					) {
						if (!initialized && qName != "Root") {
							throw IllegalArgumentException("XML not supported : $type")
						}
						if (!initialized) {
							version = attributes?.getValue("version")
								?: throw IllegalArgumentException("Unknown version in : $type")
						}

						initialized = true
						characters = ""
						qName?.let {
							when (it.uppercase()) {
								"CLINICAL_LABEL" -> {
									code =
										mutableMapOf(
											"type" to type,
											"version" to version,
											"label" to mutableMapOf<String, String>(),
											"searchTerms" to mutableMapOf<String, Set<String>>(),
											"links" to mutableSetOf<String>(),
										)
								}

								"IBUI" -> charsHandler = { ch -> code["code"] = ch }
								"ICPC_2_CODE_1", "ICPC_2_CODE_1X", "ICPC_2_CODE_1Y",
								"ICPC_2_CODE_2", "ICPC_2_CODE_2X", "ICPC_2_CODE_2Y",
								->
									charsHandler = { ch ->
										if (ch.isNotBlank()) code["links"] = (code["links"] as Set<*>) + ("ICPC|$ch|2")
									}

								"ICD_10_CODE_1", "ICD_10_CODE_1X", "ICD_10_CODE_1Y",
								"ICD_10_CODE_2", "ICD_10_CODE_2X", "ICD_10_CODE_2Y",
								->
									charsHandler = { ch ->
										if (ch.isNotBlank()) code["links"] = (code["links"] as Set<*>) + ("ICD|$ch|10")
									}

								"FR_CLINICAL_LABEL" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["label"] = (code["label"] as Map<*, *>) +
												("fr" to ch.replace("&apos;", "'"))
										}
									}

								"NL_CLINICAL_LABEL" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["label"] = (code["label"] as Map<*, *>) + ("nl" to ch)
										}
									}

								"CLEFS_RECHERCHE_FR" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["searchTerms"] = (code["searchTerms"] as Map<*, *>) +
												("fr" to ch.split(" ").map { it.trim() }.toSet())
										}
									}

								"ZOEKTERMEN_NL" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["searchTerms"] = (code["searchTerms"] as Map<*, *>) +
												("nl" to ch.split(" ").map { it.trim() }.toSet())
										}
									}

								else -> charsHandler = null
							}
						}
					}

					override fun endElement(
						uri: String?,
						localName: String?,
						qName: String?,
					) {
						charsHandler?.let { it(characters) }
						qName?.let {
							when (it.uppercase()) {
								"CLINICAL_LABEL" -> {
									runBlocking(coroutineScope.coroutineContext) {
										code["id"] =
											"${code["type"] as String}|${code["code"] as String}|${code["version"] as String}"
										batchSave(Code(args = code), false)
									}
								}

								else -> null
							}
						}
					}
				}

			val beThesaurusProcHandler =
				object : DefaultHandler() {
					var initialized = false
					var version: String = "1.0"
					var charsHandler: ((chars: String) -> Unit)? = null
					var code: MutableMap<String, Any> = mutableMapOf()
					var characters: String = ""

					override fun characters(
						ch: CharArray?,
						start: Int,
						length: Int,
					) {
						ch?.let { characters += String(it, start, length) }
					}

					override fun startElement(
						uri: String?,
						localName: String?,
						qName: String?,
						attributes: Attributes?,
					) {
						if (!initialized && qName != "Root") {
							throw IllegalArgumentException("XML not supported : $type")
						}
						if (!initialized) {
							version = attributes?.getValue("version")
								?: throw IllegalArgumentException("Unknown version in : $type")
						}

						initialized = true
						characters = ""
						qName?.let {
							when (it.uppercase()) {
								"PROCEDURE" -> {
									code =
										mutableMapOf(
											"type" to type,
											"version" to version,
											"label" to mutableMapOf<String, String>(),
											"searchTerms" to mutableMapOf<String, Set<String>>(),
										)
								}

								"CISP" -> charsHandler = { ch -> code["code"] = ch }
								"IBUI" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) code["links"] = setOf("BE-THESAURUS|$ch|$version")
									}

								"IBUI_NOT_EXACT" ->
									charsHandler = { ch ->
										if (ch.isNotBlank() && !code.containsKey("links")) {
											code["links"] = setOf("BE-THESAURUS|$ch|$version")
										}
									}

								"LABEL_FR" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) code["label"] = (code["label"] as Map<*, *>) + ("fr" to ch)
									}

								"LABEL_NL" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) code["label"] = (code["label"] as Map<*, *>) + ("nl" to ch)
									}

								"SYN_FR" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["searchTerms"] = (code["searchTerms"] as Map<*, *>) +
												("fr" to ch.split(" ").map { it.trim() }.toSet())
										}
									}

								"SYN_NL" ->
									charsHandler = { ch ->
										if (ch.isNotBlank()) {
											code["searchTerms"] = (code["searchTerms"] as Map<*, *>) +
												("nl" to ch.split(" ").map { it.trim() }.toSet())
										}
									}

								else -> charsHandler = null
							}
						}
					}

					override fun endElement(
						uri: String?,
						localName: String?,
						qName: String?,
					) {
						charsHandler?.let { it(characters) }
						qName?.let {
							when (it.uppercase()) {
								"PROCEDURE" -> {
									runBlocking(coroutineScope.coroutineContext) {
										code["id"] =
											"${code["type"] as String}|${code["code"] as String}|${code["version"] as String}"
										batchSave(Code(args = code), false)
									}
								}

								else -> null
							}
						}
					}
				}

			val iso6391Handler =
				object : DefaultHandler() {
					var initialized = false
					var version: String = "1.0"
					var charsHandler: ((chars: String) -> Unit)? = null
					var code: MutableMap<String, Any> = mutableMapOf()
					var characters: String = ""

					override fun characters(
						ch: CharArray?,
						start: Int,
						length: Int,
					) {
						ch?.let { characters += String(it, start, length) }
					}

					override fun startElement(
						uri: String?,
						localName: String?,
						qName: String?,
						attributes: Attributes?,
					) {
						if (!initialized && qName != "ISO639-1") {
							throw IllegalArgumentException("XML not supported : $type")
						}

						initialized = true
						characters = ""
						qName?.let {
							when (it.uppercase()) {
								"VERSION" ->
									charsHandler = { ch ->
										version = ch
									}

								"VALUE" -> {
									code =
										mutableMapOf(
											"type" to type,
											"version" to version,
											"label" to mapOf<String, String>(),
										)
								}

								"CODE" -> charsHandler = { ch -> code["code"] = ch }
								"DESCRIPTION" ->
									charsHandler = {
										attributes?.getValue("L")?.let { attributesValue ->
											code["label"] = (code["label"] as Map<*, *>) + (attributesValue to it.trim())
										}
									}

								else -> charsHandler = null
							}
						}
					}

					override fun endElement(
						uri: String?,
						localName: String?,
						qName: String?,
					) {
						charsHandler?.let { it(characters) }
						qName?.let {
							when (it.uppercase()) {
								"VALUE" -> {
									runBlocking(coroutineScope.coroutineContext) {
										code["id"] =
											"${code["type"] as String}|${code["code"] as String}|${code["version"] as String}"
										batchSave(Code(args = code), false)
									}
								}

								else -> null
							}
						}
					}
				}

			try {
				when (type.uppercase()) {
					"BE-THESAURUS-PROCEDURES" -> saxParser.parse(stream, beThesaurusProcHandler)
					"BE-THESAURUS" -> saxParser.parse(stream, beThesaurusHandler)
					"ISO-639-1" -> saxParser.parse(stream, iso6391Handler)
					else -> saxParser.parse(stream, handler)
				}
				batchSave(null, true)
				create(Code.from("ICURE-SYSTEM", md5, "1"))
			} catch (e: IllegalArgumentException) {
				// Skip
			} finally {
				stream.close()
			}
		} else {
			stream.close()
		}
	}

	override suspend fun importCodesFromJSON(stream: InputStream) {
		val datastoreInformation = getInstanceAndGroup()
		val codeList = objectMapper.readValue<List<Code>>(stream)

		val existing = getCodes(codeList.map { it.id }).fold(mapOf<String, Code>()) { map, c -> map + (c.id to c) }
		codeDAO
			.saveBulk(
				datastoreInformation,
				codeList.map { newCode ->
					existing[newCode.id]?.let { newCode.copy(rev = it.rev) } ?: newCode
				},
			).onEach {
				if (it is BulkSaveResult.Success) {
					log.debug("Code: ${it.entity.id} is saved")
				}
			}.filter { it !is BulkSaveResult.Success }
			.count()
			.also {
				log.error("$it conflicts")
			}
	}

	override fun listCodes(
		paginationOffset: PaginationOffset<*>?,
		filterChain: FilterChain<Code>,
		sort: String?,
		desc: Boolean?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filterChain.filter, datastoreInformation).toList().sorted()
		val codes = codeDAO.getCodesByIdsForPagination(datastoreInformation, ids)
		if (filterChain.predicate != null || sort != null && sort != "id") {
			filterChain.predicate?.let {
				codes.filter {
					if (it is ViewRowWithDoc<*, *, *>) {
						val code = it.doc as Code
						filterChain.predicate!!.apply(code)
					} else {
						true
					}
				}
			}

			sort?.let { sortProperty ->
				val pub = PropertyUtilsBean()
				var codesList = codes.toList()
				codesList =
					codesList
						.mapNotNull {
							if (it is ViewRowWithDoc<*, *, *>) {
								it
							} else {
								emit(it)
								null
							}
						}.toList()
						.sortedBy { it ->
							val itCode = it.doc as Code
							try {
								pub.getProperty(itCode, sort) as? String
							} catch (e: Exception) {
								""
							} ?: ""
						}
				emitAll(codesList.asFlow())
			} ?: emitAll(codes)
		} else {
			emitAll(codes)
		}
	}

	override suspend fun getOrCreateCode(
		type: String,
		code: String,
		version: String,
	): Code? {
		val codes = findCodesBy(type, code, null).toList()
		if (codes.isNotEmpty()) {
			codes
				.stream()
				.sorted { a, b -> a.version?.let { b.version?.compareTo(it) }!! }
				.findFirst()
				.get()
		}

		return this.create(Code.from(type, code, version))
	}

	override suspend fun isValid(
		type: String?,
		code: String?,
		version: String?,
	): Boolean = isValid(CodeStub(id = "$type|$code|$version", code = code, type = type, version = version), null)

	override suspend fun isValid(
		code: Code,
		ofType: String?,
	): Boolean = isValid(CodeStub(id = code.id, code = code.code, type = code.type, version = code.version), ofType)

	override suspend fun isValid(
		code: CodeStub,
		ofType: String?,
	): Boolean {
		val datastoreInformation = getInstanceAndGroup()
		val codeType = ofType ?: code.type
		return if (codeType != null && code.code != null) {
			codeDAO.isValid(
				datastoreInformation,
				codeType,
				code.code!!,
				code.version,
			)
		} else {
			false
		}
	}

	override suspend fun getCodeByLabel(
		region: String?,
		label: String,
		type: String,
		languages: List<String>,
	): Code? {
		val datastoreInformation = getInstanceAndGroup()
		return codeDAO.getCodeByLabel(datastoreInformation, region, label, type, languages)
	}

	override fun getEntities() = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(getGenericDAO().getEntities(datastoreInformation))
	}

	override fun getEntityIds() = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(getGenericDAO().getEntityIds(datastoreInformation))
	}

	override suspend fun hasEntities(): Boolean {
		val datastoreInformation = getInstanceAndGroup()
		return getGenericDAO().hasAny(datastoreInformation)
	}

	override suspend fun exists(id: String): Boolean {
		val datastoreInformation = getInstanceAndGroup()
		return getGenericDAO().contains(datastoreInformation, id)
	}

	override suspend fun getEntity(id: String): Code? {
		val datastoreInformation = getInstanceAndGroup()
		return getGenericDAO().get(datastoreInformation, id)
	}

	override fun getGenericDAO(): CodeDAO = codeDAO

	override fun solveConflicts(
		limit: Int?,
		ids: List<String>?,
	) = flow {
		emitAll(
			doSolveConflicts(
				ids,
				limit,
				getInstanceAndGroup(),
			),
		)
	}

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val flow =
			ids?.asFlow()?.mapNotNull { codeDAO.get(datastoreInformation, it, Option.CONFLICTS) }
				?: codeDAO
					.listConflicts(datastoreInformation)
					.mapNotNull { codeDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { code ->
				code.conflicts
					?.mapNotNull { conflictingRevision ->
						codeDAO.get(
							datastoreInformation,
							code.id,
							conflictingRevision,
						)
					}?.fold(code to emptyList<Code>()) { (kept, toBePurged), conflict ->
						kept.merge(conflict) to toBePurged + conflict
					}?.let { (mergedCode, toBePurged) ->
						codeDAO.save(datastoreInformation, mergedCode).also {
							toBePurged.forEach {
								if (it.rev != null && it.rev != mergedCode.rev) {
									codeDAO.purge(datastoreInformation, listOf(it)).single()
								}
							}
						}
					}
			}.collect { emit(IdAndRev(it.id, it.rev)) }
	}
}
