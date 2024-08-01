/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.Client
import org.taktik.couchdb.Offset
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.annotation.Views
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.base.Code
import org.taktik.icure.utils.LATEST_VERSION
import kotlin.math.min

@Repository("codeDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.base.Code' && !doc.deleted) emit( null, doc._id )}")
 class CodeDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<Code>(Code::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), CodeDAO {

	companion object {
		private const val SMALLEST_CHAR = "\u0000"
		private val semVerRegex = "^[0-9]+(\\.[0-9]+)*$".toRegex()

		private val semanticComparator: (a: String, b: String) -> Int = { a, b ->
			if(semVerRegex.matches(a) && semVerRegex.matches(b)) {
				val aComponents = a.split(".").map { it.toInt() }
				val bComponents = b.split(".").map { it.toInt() }
				var compareResult: Int? = null
				var i = 0
				while(compareResult == null && i < min(aComponents.size, bComponents.size)) {
					compareResult = (aComponents[i].compareTo(bComponents[i])).takeIf { it != 0 }
					i++
				}
				when {
					compareResult != null -> compareResult
					aComponents.size > bComponents.size -> 1
					aComponents.size < bComponents.size -> -1
					else -> 0
				}
			} else a.compareTo(b)
		}

		/**
		 * Utility class to store partial results during version filtering.
		 * Everything is mutable because the volume of codes can be huge and making a copy of the object everytime may
		 * have an impact on the overall performance.
		 */
		private class QueryResultAccumulator {
			var seenElements: Int = 0
				private set
			var sentElements: Int = 0
				private set
			var elementsFound: Int? = null
				private set
			var toEmit: ViewQueryResultEvent? = null
				private set
			var lastVisited: ViewRowWithDoc<*, *, *>? = null
				private set
			var offset: Int? = null
				private set

			fun setLatestRow(row: ViewRowWithDoc<*, *, *>, limit: Int) {
				val lastCode = lastVisited?.doc as? Code
				val newCode = row.doc as Code
				if (lastCode != null && sentElements < limit && // If I have something to emit and I still have space on the page
					(lastCode.code != newCode.code || lastCode.type != newCode.type)
				) { // The codes are sorted, If this one is different for something
					seenElements++
					sentElements++
					toEmit = lastVisited
					lastVisited = row
				} else if (lastCode?.version != null  // If the new code is an instance of the same code, but of a previous or equal version
					&& newCode.version != null && semanticComparator(lastCode.version!!, newCode.version!!) >= 0
				) {
					seenElements++
					toEmit = null
				} else {
					lastVisited = row
					seenElements++
					toEmit = null
				}
			}

			fun setRow(row: ViewRowWithDoc<*, *, *>, skip: Boolean) {
				if (lastVisited != null || !skip) { // If it is the second or later call, I have to skip the first result (otherwise is repeated)
					toEmit = row
					lastVisited = row
					seenElements++
					sentElements++
				} else {
					toEmit = null
					lastVisited = row
					seenElements++
				}
			}

			fun setTotalAndResetEmission(total: Int) {
				elementsFound = total
				toEmit = null
			}

			fun setOffsetAndResetEmission(newOffset: Int) {
				offset = newOffset
				toEmit = null
			}

			fun resetEmission(seen: Int = 0) {
				toEmit = null
				seenElements += seen
			}

		}

		data class CodeAccumulator(
			val code: Code? = null,
			val toEmit: Code? = null
		)

		private fun Code.verifyVersion(version: String) = (this.version == version)
		private fun Code.matchesAllLabelParts(language: String, labelParts: Set<String>): Boolean {
			if (labelParts.isEmpty()) return true

			val remainingParts = (label?.get(language)?.split("[ |/'`]+".toRegex())?.mapNotNull {
				sanitizeString(it)?.takeIf { s -> s.length > 2 }
			}?.toSet()?.let { labelParts - it } ?: labelParts).toMutableSet()

			if(remainingParts.isEmpty()) return true

			for (term in searchTerms.getOrDefault(language, emptySet())) {
				val sanitizedTerms = term.split("[ |/'`]+".toRegex()).mapNotNull {
					sanitizeString(it)?.takeIf { s -> s.length > 2 }
				}.toSet()
				remainingParts.removeIf { label ->
					sanitizedTerms.none { it.startsWith(label) }
				}
				if(remainingParts.isEmpty()) return true
			}
			return false
		}
		private fun Code.matchesRegion(region: String?): Boolean = region == null || regions.contains(region)

		/**
		 * Given a query for the label view, it splits it, sanitizes each part and removes all the parts which length
		 * is lower or equal than two.
		 * @return a [Pair]: the first element is the label part that should be used as query i.e. the longest, the
		 * second is a [Set] of all the other sanitizes parts of the label with a length greater than 2 that can be
		 * used to match the full label.
		 */
		private fun String.splitAndSanitizeLabel(): Pair<String, Set<String>> {
			val sanitizedLabel = split("[ |/'`]+".toRegex()).mapNotNull {
				sanitizeString(it)
			}.filter { it.length > 2 }.maxByOrNull { it.length } ?: sanitizeString(this) ?: ""
			val otherParts = split("[ |/'`]+".toRegex()).mapNotNull { sanitizeString(it) }.filter { it.length > 2 && it != sanitizedLabel }.toSet()
			return Pair(sanitizedLabel, otherParts)
		}
	}

	@View(name = "by_type_code_version", map = "classpath:js/code/By_type_code_version.js")
	override fun listCodesBy(datastoreInformation: IDatastoreInformation, type: String?, code: String?, version: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocsNoValue<Array<String>, Code>(
				createQuery(datastoreInformation, "by_type_code_version")
					.includeDocs(true)
					.reduce(false)
					.startKey(
						ComplexKey.of(
							type,
							code,
							version
						)
					)
					.endKey(
						ComplexKey.of(
							type ?: ComplexKey.emptyObject(),
							code ?: ComplexKey.emptyObject(),
							version ?: ComplexKey.emptyObject()
						)
					)
			).map { it.doc }
		)
	}

	@View(name = "by_region_type_code_version", map = "classpath:js/code/By_region_type_code_version.js", reduce = "_count")
	override fun listCodesBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val startKey = ComplexKey.of(
			region ?: SMALLEST_CHAR,
			type ?: SMALLEST_CHAR,
			code ?: SMALLEST_CHAR,
			if (version == null || version == LATEST_VERSION) SMALLEST_CHAR else version
		)

		val endKey = ComplexKey.of(
			region ?: ComplexKey.emptyObject(),
			type ?: ComplexKey.emptyObject(),
			code ?: ComplexKey.emptyObject(),
			if (version == null || version == LATEST_VERSION) ComplexKey.emptyObject() else version
		)

		var lastCode: Code? = null
		emitAll(
			client.queryViewIncludeDocsNoValue<Array<String>, Code>(
				createQuery(datastoreInformation, "by_region_type_code_version")
					.includeDocs(true)
					.reduce(false)
					.startKey(startKey)
					.endKey(endKey)
			).map {
				it.doc
			}.let { flw ->
				when {
					version == "latest" -> { // If the version is latest
						flw.scan(CodeAccumulator()) { acc, code ->
							lastCode = code // I save the last code I visit
							acc.code?.let { // If I have a previous code
								when {
									// If I reached a different code, then I have to emit the previous one
									code.type != it.type || code.code != it.code -> CodeAccumulator(code, it)
									// If the code is the same, I return the new one only if the version is a greater one
									// Note: the versions are ordered lexicographically in CouchDB, so semantic versions are
									// not correctly sorted.
									code.version != null
										&& it.version != null
										&& semanticComparator(code.version!!, it.version!!) > 0 -> CodeAccumulator(code)
									// Otherwise, I keep the current one
									else -> acc
								}
							} ?: CodeAccumulator(code)
						}.mapNotNull{
							it.toEmit
						}
					}
					// If I have a specific version, but I cannot query the view because one of the other parameters is null
					// I have to filter the flow
					version != null && (region == null || type == null || code == null) -> flw.filter {
						it.version == version
					}
					else -> flw // If the version is not latest, I emit everything I queried
				}
			}.onCompletion {
				// If last code is not null, I have to emit it
				// This can happen only with the latest filter
				if (lastCode != null) emit(lastCode!!)
			}
		)
	}

	override fun listCodeTypesByRegionAndType(datastoreInformation: IDatastoreInformation, region: String?, type: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView<List<String>, String>(
				createQuery(datastoreInformation, "by_region_type_code_version")
					.includeDocs(false)
					.group(true)
					.groupLevel(2)
					.startKey(ComplexKey.of(region, type, null, null))
					.endKey(ComplexKey.of(
						region ?: ComplexKey.emptyObject(),
						type ?: ComplexKey.emptyObject(),
						ComplexKey.emptyObject(), ComplexKey.emptyObject()))
			).mapNotNull { it.key?.get(1) }
		)
	}

	override fun findCodesBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			findCodesByRecursively(datastoreInformation, client, region, type, code, version, paginationOffset, null)
		)
	}

	private fun getFindCodesQueryLimits(region: String?, type: String?, code: String?, version: String?): Pair<ComplexKey, ComplexKey> {
		val from = ComplexKey.of(
			region ?: SMALLEST_CHAR,
			type ?: SMALLEST_CHAR,
			code ?: SMALLEST_CHAR,
			if (version == null || version == LATEST_VERSION) SMALLEST_CHAR else version
		)
		val to = ComplexKey.of(
			region ?: ComplexKey.emptyObject(),
			type ?: ComplexKey.emptyObject(),
			if (code == null) ComplexKey.emptyObject() else if (version == null) code + "\ufff0" else code,
			if (version == null || version == LATEST_VERSION) ComplexKey.emptyObject() else version + "\ufff0"
		)
		return from to to
	}

	private fun findCodesByRecursively(datastoreInformation: IDatastoreInformation, client: Client, region: String?, type: String?, code: String?, version: String?, paginationOffset: PaginationOffset<List<String?>>, extensionFactor: Float?): Flow<ViewQueryResultEvent> = flow {
		val (from, to) = getFindCodesQueryLimits(region, type, code, version)
		val extendedLimit = (paginationOffset.limit * ( extensionFactor ?: 1f) ).toInt()

		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_region_type_code_version",
			from,
			to,
			paginationOffset.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) }.copy(limit = extendedLimit),
			false
		)

		val versionAccumulator = QueryResultAccumulator()
		emitAll(
			client.queryView(viewQuery, Array<String>::class.java, String::class.java, Code::class.java).let { flw ->
				if (version == null || version != LATEST_VERSION) flw
				else flw.scan(versionAccumulator) { acc, it ->
					when (it) {
						is ViewRowWithDoc<*, *, *> -> acc.apply { setLatestRow(it, paginationOffset.limit) }
						is TotalCount -> acc.apply { setTotalAndResetEmission(it.total)}
						is Offset -> acc.apply { setOffsetAndResetEmission(it.offset) }
						else -> acc.apply { resetEmission() }
					}
				}.transform {
					if (it.toEmit != null) emit(it.toEmit!!) // If I have something to emit, I emit it
				}.onCompletion {
					// If it viewed all the elements there can be more
					// AND it did not fill the page
					// it does the recursive call
					if (versionAccumulator.seenElements >= extendedLimit && versionAccumulator.sentElements < paginationOffset.limit)
						emitAll(
							@Suppress("UNCHECKED_CAST")
							findCodesByRecursively(
								datastoreInformation,
								client,
								region,
								type,
								code,
								version,
								paginationOffset.copy(startKey = (versionAccumulator.lastVisited?.key as? Array<String>)?.toList(), startDocumentId = versionAccumulator.lastVisited?.id, limit = paginationOffset.limit - versionAccumulator.sentElements),
								(if (versionAccumulator.seenElements == 0) ( extensionFactor ?: 1f) * 2 else (versionAccumulator.seenElements.toFloat() / versionAccumulator.sentElements)).coerceAtMost(100f)
							)
						)
					else {
						// If the version filter is latest and there are no more elements to visit and the page is not full, I emit the last element
						if (versionAccumulator.lastVisited != null && versionAccumulator.sentElements < paginationOffset.limit)
							emit(versionAccumulator.lastVisited as ViewQueryResultEvent) //If the version filter is "latest" then the last code must be always emitted
						emit(TotalCount(versionAccumulator.elementsFound ?: 0))
					}
				}
			}
		)
	}

	override fun listCodeIdsBy(datastoreInformation: IDatastoreInformation, region: String?, type: String?, code: String?, version: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val (from, to) = getFindCodesQueryLimits(region, type, code, version)

		val viewQuery = createQuery(datastoreInformation, "by_region_type_code_version")
			.startKey(from)
			.endKey(to)
			.includeDocs(false)

		var lastCode: Code? = null
		emitAll(
			client.queryView<ComplexKey, String>(viewQuery).let { flw ->
				if (version == null || version != LATEST_VERSION) flw.map { it.id }
				else flw.scan(CodeAccumulator()) { acc, row ->
					val (_, keyType, keyCode, keyVersion) = checkNotNull(row.key).components
					lastCode = Code(
						id = row.id,
						type = keyType as? String,
						code = keyCode as? String,
						version = keyVersion as? String
					) // I save the last code I visit
					acc.code?.let { // If I have a previous code
						when {
							// If I reached a different code, then I have to emit the previous one
							keyType != it.type ||keyCode != it.code -> CodeAccumulator(lastCode, it)
							// If the code is the same, I return the new one only if the version is a greater one
							// Note: the versions are ordered lexicographically in CouchDB, so semantic versions are
							// not correctly sorted.
							keyVersion != null
								&& it.version != null
								&& semanticComparator(keyVersion as String, it.version!!) > 0 -> CodeAccumulator(lastCode)
							// Otherwise, I keep the current one
							else -> acc
						}
					} ?: CodeAccumulator(lastCode)
				}.mapNotNull{
					it.toEmit?.id
				}.onCompletion {
					// If last code is not null, I have to emit it
					// This can happen only with the latest filter
					if (lastCode != null) emit(checkNotNull(lastCode?.id))
				}
			}
		)
	}

	private suspend fun buildByLabelPageQuery(
		datastoreInformation: IDatastoreInformation,
		language: String,
		type: String,
		sanitizedLabel: String,
		limit: Int,
		paginationOffset: PaginationOffset<List<String?>>,
	) = pagedViewQuery(
		datastoreInformation,
		"by_language_type_label",
		ComplexKey.of(language, type, sanitizedLabel, SMALLEST_CHAR),
		ComplexKey.of(language, type, sanitizedLabel + "\ufff0", ComplexKey.emptyObject()),
		paginationOffset.copy(limit = limit).toPaginationOffset { sk -> ComplexKey.of(*sk.mapIndexed { i, s -> if (i == 3) s?.let { sanitizeString(it) } else s }.toTypedArray()) },
		false,
		MAURICE_PARTITION
	)

	private fun findSpecificVersionOfCodesByLabel(
		datastoreInformation: IDatastoreInformation,
		client: Client,
		region: String?,
		language: String,
		type: String,
		label: String,
		version: String,
		paginationOffset: PaginationOffset<List<String?>>,
		extensionFactor: Float = 1f,
		prevTotalCount: Int = 0,
		isContinue: Boolean = false
	): Flow<ViewQueryResultEvent> = flow {
		val (sanitizedLabel, otherParts) = label.splitAndSanitizeLabel()
		val extendedLimit = (paginationOffset.limit * extensionFactor).toInt()
		val viewQuery = buildByLabelPageQuery(
			datastoreInformation,
			language,
			type,
			sanitizedLabel,
			extendedLimit,
			paginationOffset
		)
		val versionAccumulator = QueryResultAccumulator()
		client.queryView(viewQuery, Array<String>::class.java, Array<String>::class.java, Code::class.java).scan(versionAccumulator) { acc, it ->
			// For each element returned by the query, emits only the ones that match exactly the version passed as
			// parameter, if they match all the label parts and have the region, if a region is specified.
			when (it) {
				is ViewRowWithDoc<*, *, *> ->
					if (acc.sentElements < paginationOffset.limit && (it.doc as Code).let {
						it.verifyVersion(version) && it.matchesAllLabelParts(language, otherParts) && it.matchesRegion(region)
					}) acc.apply { setRow(it, isContinue) }
					else acc.apply { resetEmission(1) }
				is TotalCount -> acc.apply { setTotalAndResetEmission(it.total) }
				else -> acc.apply { resetEmission() }
			}
		}.transform {
			if (it.toEmit != null) emit(it.toEmit!!) //If I have something to emit, I emit it
		}.onCompletion {
			// If it viewed all the elements there can be more
			// AND it did not fill the page
			// it does the recursive call
			if (versionAccumulator.seenElements >= extendedLimit && versionAccumulator.sentElements < paginationOffset.limit)
				emitAll(
					findSpecificVersionOfCodesByLabel(
						datastoreInformation,
						client,
						region,
						language,
						type,
						label,
						version,
						paginationOffset.copy(
							startKey = (versionAccumulator.lastVisited?.key as? Array<*>)?.mapNotNull { it as? String },
							startDocumentId = versionAccumulator.lastVisited?.id,
							limit = paginationOffset.limit - versionAccumulator.sentElements
						),
						versionAccumulator.seenElements.let {
							if(it == 0) extensionFactor * 2
							else versionAccumulator.seenElements.toFloat() / versionAccumulator.sentElements
						}.coerceAtMost(100f),
						versionAccumulator.sentElements + prevTotalCount,
						true
					)
				)
			else emit(TotalCount((versionAccumulator.elementsFound ?: 0) + prevTotalCount))
		}.also { emitAll(it) }
	}

	private fun  findLatestVersionOfCodesByLabel(
		datastoreInformation: IDatastoreInformation,
		client: Client,
		region: String?,
		language: String,
		type: String,
		label: String,
		offset: PaginationOffset<List<String?>>,
	): Flow<ViewQueryResultEvent> = flow {

		// Utility function that will return a set of type code pair for each code that matches the label, language, and
		// region passed as parameters. It will run recursively until it retrieves enough codes to fill a page.
		suspend fun findKeyByTypeCode(
			datastoreInformation: IDatastoreInformation,
			client: Client,
			region: String?,
			language: String,
			type: String,
			label: String,
			offset: PaginationOffset<List<String?>>,
			extensionFactor: Float = 1f,
			alreadyEmitted: LinkedHashSet<List<String>> = LinkedHashSet()
		): Flow<Pair<List<String>, Array<String>>> = flow {
			val (sanitizedLabel, otherParts) = label.splitAndSanitizeLabel()
			val extendedLimit = if (offset.limit > 0) (offset.limit * extensionFactor).toInt() else 10
			val viewQuery = buildByLabelPageQuery(
				datastoreInformation,
				language,
				type,
				sanitizedLabel,
				extendedLimit,
				offset
			)
			var seenElements = 0
			var sentElements = 0
			var lastVisited: ViewRowWithDoc<*, *, *>? = null
			emitAll(client.queryView(viewQuery, Array<String>::class.java, Array<String>::class.java, Code::class.java)
				.filterIsInstance<ViewRowWithDoc<*, *, *>>()
				.transform {
					val currentCode = it.doc as Code
					seenElements++
					if (currentCode.type != null
						&& currentCode.code != null
						&& currentCode.matchesAllLabelParts(language, otherParts)
						&& currentCode.matchesRegion(region)
					) {
						@Suppress("UNCHECKED_CAST")
						(it.key as? Array<String>)?.also { k ->
							val emissionKey = listOf(currentCode.type!!, currentCode.code!!)
							if (!alreadyEmitted.contains(emissionKey)) {
								sentElements++
								alreadyEmitted.add(emissionKey)
								emit(Pair(emissionKey, k))
							}
						}
					}
					lastVisited = it
				}.onCompletion {
					(lastVisited?.doc as? Code)?.also { lastCode ->
						if (seenElements >= extendedLimit &&
							(sentElements < offset.limit || listOf(
								lastCode.type,
								lastCode.code
							) == alreadyEmitted.last())
						) {
							@Suppress("UNCHECKED_CAST")
							emitAll(
								findKeyByTypeCode(
									datastoreInformation,
									client,
									region,
									language,
									type,
									label,
									offset.copy(
										startKey = (lastVisited?.key as? Array<String>)?.toList(),
										startDocumentId = lastVisited?.id,
										limit = (offset.limit - sentElements).coerceAtLeast(1)
									),
									(if (seenElements == 0) extensionFactor * 2 else (seenElements.toFloat() / sentElements)).coerceAtMost(
										100f
									),
									alreadyEmitted
								)
							)
						}
					}
				}
			)
		}

		val validCodes = findKeyByTypeCode(datastoreInformation, client, region, language, type, label, offset).toList().toMap()
		var toEmit: Code? = null

		// Given the type and code pairs of all the codes that match the label, it will use the by_type_code view
		// to get all the versions of each code, returning always the one that will be the next key for the page, if any.
		emitAll(
			client.queryViewIncludeDocsNoValue<Array<String>, Code>(
				createQuery(datastoreInformation, "by_type_code", MAURICE_PARTITION)
					.includeDocs(true)
					.reduce(false)
					.keys(validCodes.keys)
			).transform {
				// The old code is emitted when a new type is found
				if(toEmit != null && (toEmit?.type != it.doc.type || toEmit?.code != it.doc.code)) {
					val code = toEmit!!
					emit(ViewRowWithDoc(code.id, validCodes.getValue(listOf(code.type!!, code.code!!)), code.id, code))
				}
				when {
					toEmit == null -> {
						toEmit = it.doc
					}
					toEmit != null && (toEmit?.type != it.doc.type || toEmit?.code != it.doc.code) -> {
						toEmit = it.doc
					}
					toEmit?.version != null && it.doc.version != null && semanticComparator(it.doc.version!!, toEmit?.version!!) > 0 -> {
						toEmit = it.doc
					}
				}
			}.onCompletion {
				toEmit?.also { code ->
					emit(ViewRowWithDoc(code.id, validCodes.getValue(listOf(code.type!!, code.code!!)), code.id, code))
				}
			}
		)
	}

	// Returns all the versions for the codes that match the label, calling itself recursively until the page is filled.
	private fun findCodesByLabel(
		datastoreInformation: IDatastoreInformation,
		client: Client,
		region: String?,
		language: String,
		type: String,
		label: String,
		paginationOffset: PaginationOffset<List<String?>>,
		extensionFactor: Float = 1f,
		prevTotalCount: Int = 0,
	): Flow<ViewQueryResultEvent> = flow {
		val (sanitizedLabel, otherParts) = label.splitAndSanitizeLabel()
		val extendedLimit = (paginationOffset.limit * extensionFactor).toInt()
		val viewQuery = buildByLabelPageQuery(
			datastoreInformation,
			language,
			type,
			sanitizedLabel,
			extendedLimit,
			paginationOffset
		)
		var seenElements = 0
		var sentElements = 0
		var lastVisited: ViewRowWithDoc<*, *, *>? = null
		client.queryView(viewQuery, Array<String>::class.java, Array<String>::class.java, Code::class.java).transform {
			when(it) {
				is ViewRowWithDoc<*, *, *> -> {
					seenElements++
					lastVisited = it
					val currentCode = (it.doc as Code)
					if(currentCode.matchesAllLabelParts(language, otherParts) && currentCode.matchesRegion(region)) {
						sentElements++
						emit(it)
					}
				}
				else -> {}
			}
		}.onCompletion {
			// If it viewed all the elements there can be more
			// AND it did not fill the page
			// it does the recursive call
			if (seenElements >= extendedLimit && sentElements < paginationOffset.limit)
				emitAll(
					findCodesByLabel(
						datastoreInformation,
						client,
						region,
						language,
						type,
						label,
						paginationOffset.copy(
							startKey = (lastVisited?.key as? Array<*>)?.mapNotNull { it as? String },
							startDocumentId = lastVisited?.id,
							limit = paginationOffset.limit - sentElements
						),
						seenElements.let {
							if(it == 0) extensionFactor * 2
							else seenElements.toFloat() / sentElements
						}.coerceAtMost(100f),
						sentElements + prevTotalCount,
					)
				)
			else emit(TotalCount(sentElements + prevTotalCount))
		}.also { emitAll(it) }
	}

	@Views(
		View(name = "by_language_type_label", map = "classpath:js/code/By_language_type_label.js", secondaryPartition = MAURICE_PARTITION),
		View(name = "by_type_code", map = "classpath:js/code/By_type_code.js", secondaryPartition = MAURICE_PARTITION),
	)
	override fun findCodesByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String, type: String, label: String, version: String?, paginationOffset: PaginationOffset<List<String?>>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		when(version) {
			LATEST_VERSION -> findLatestVersionOfCodesByLabel(
				datastoreInformation,
				client,
				region,
				language,
				type,
				label,
				paginationOffset
			)
			null -> findCodesByLabel(datastoreInformation, client, region, language, type, label, paginationOffset)
			else -> findSpecificVersionOfCodesByLabel(
				datastoreInformation,
				client,
				region,
				language,
				type,
				label,
				version,
				paginationOffset
			)
		}.also { emitAll(it) }
	}

	@View(name = "by_qualifiedlink_id", map = "classpath:js/code/By_qualifiedlink_id.js")
	override fun findCodesByQualifiedLinkId(datastoreInformation: IDatastoreInformation, linkType: String, linkedId: String?, paginationOffset: PaginationOffset<List<String>>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from =
			ComplexKey.of(
				linkType,
				linkedId
			)
		val to = ComplexKey.of(
			linkType,
			linkedId ?: ComplexKey.emptyObject()
		)

		val viewQuery = pagedViewQuery(
			datastoreInformation,
			"by_qualifiedlink_id",
			from,
			to,
			paginationOffset.toPaginationOffset { ComplexKey.of(*it.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, Array<String>::class.java, String::class.java, Code::class.java))
	}

	override fun listCodeIdsByLabel(datastoreInformation: IDatastoreInformation, region: String?, language: String, type: String, label: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val sanitizedLabel = label?.let { sanitizeString(it) }
		val from =
			ComplexKey.of(
				language,
				type,
				sanitizedLabel ?: SMALLEST_CHAR,
				SMALLEST_CHAR
			)
		val to = ComplexKey.of(
			language,
			if (sanitizedLabel == null) type + "\ufff0" else type,
			sanitizedLabel?.let { it + "\ufff0" } ?: ComplexKey.emptyObject(),
			ComplexKey.emptyObject()
		)

		emitAll(
			client.queryView<Array<String>, Array<String>?>(
				createQuery(datastoreInformation, "by_language_type_label", MAURICE_PARTITION)
					.includeDocs(false)
					.reduce(false)
					.startKey(from)
					.endKey(to)
			).mapNotNull { row ->
				row.id.takeIf { row.value?.contains(region) != false }
			}
		)
	}

	override fun listCodeIdsByTypeCodeVersionInterval(datastoreInformation: IDatastoreInformation, startType: String?, startCode: String?, startVersion: String?, endType: String?, endCode: String?, endVersion: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			startType ?: SMALLEST_CHAR,
			startCode ?: SMALLEST_CHAR,
			startVersion ?: SMALLEST_CHAR,
		)
		val to = ComplexKey.of(
			endType ?: ComplexKey.emptyObject(),
			endCode ?: ComplexKey.emptyObject(),
			endVersion ?: ComplexKey.emptyObject(),
		)
		emitAll(
			client.queryView<Array<String>, String>(
				createQuery(datastoreInformation, "by_type_code_version")
					.includeDocs(false)
					.reduce(false)
					.startKey(from)
					.endKey(to)
			).mapNotNull { it.id }
		)
	}

	override fun listCodeIdsByQualifiedLinkId(datastoreInformation: IDatastoreInformation, linkType: String, linkedId: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val from = ComplexKey.of(
			linkType,
			linkedId
		)
		val to = ComplexKey.of(
			linkType,
			linkedId ?: ComplexKey.emptyObject()
		)

		emitAll(
			client.queryView<String, String>(
				createQuery(datastoreInformation, "by_qualifiedlink_id")
					.includeDocs(false)
					.startKey(from)
					.endKey(to)
			).mapNotNull { it.id }
		)
	}

	override fun getCodesByIdsForPagination(datastoreInformation: IDatastoreInformation, ids: List<String>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(ids, Code::class.java))
	}

	override suspend fun isValid(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String, codeVersion: String?) = listCodesBy(datastoreInformation, codeType, codeCode, codeVersion).firstOrNull() != null

	// Note: languages has a default value in the interface
	override suspend fun getCodeByLabel(datastoreInformation: IDatastoreInformation, region: String?, label: String, type: String, languages: List<String>): Code? {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val (sanitizedLabel, otherParts) = label.splitAndSanitizeLabel()
		return languages.firstNotNullOfOrNull { lang ->
			client.queryViewIncludeDocsNoValue<Array<String>, Code>(
				createQuery(datastoreInformation, "by_language_type_label", MAURICE_PARTITION)
					.includeDocs(true)
					.reduce(false)
					.startKey(ComplexKey.of(lang, type, sanitizedLabel, SMALLEST_CHAR))
					.endKey(ComplexKey.of(lang, type, sanitizedLabel, ComplexKey.emptyObject()))
			).mapNotNull { row ->
				row.doc.takeIf { it.matchesRegion(region) && it.matchesAllLabelParts(lang, otherParts) }
			}.firstOrNull()
		}
	}

	@View(name = "conflicts", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.base.Code' && !doc.deleted && doc._conflicts) emit(doc._id )}", secondaryPartition = MAURICE_PARTITION)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(datastoreInformation, "conflicts", MAURICE_PARTITION).includeDocs(true)
		emitAll(client.queryViewIncludeDocsNoValue<String, Code>(viewQuery).map { it.doc })
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.Maurice -> warmup(datastoreInformation, "by_language_type_label" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
