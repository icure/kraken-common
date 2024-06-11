package org.taktik.icure.asyncdao.components

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.taktik.couchdb.entity.View
import org.taktik.couchdb.support.views.ExternalViewRepository
import java.security.MessageDigest
import java.time.Duration
import java.util.*

abstract class ExternalViewLoader(
	private val objectMapper: ObjectMapper,
	rawPublicSigningKey: String
) {

	companion object {
		private data class SignedContent(
			val content: JsonNode,
			val signature: String
		)
	}

	private val manifestCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(10))
		.build<String, Map<String, String>>()

	private val publicSigningKey = rawPublicSigningKey.split(" ")[1].let {
		OpenSSHPublicKeyUtil.parsePublicKey(Base64.getDecoder().decode(it))
	}

	private val httpClient = HttpClient(CIO) {
		install(ContentNegotiation) {
			register(ContentType.Application.Json, JacksonConverter(objectMapper))
		}
	}

	private fun gitHubRawUrlForResource(baseUrl: String, resourcePath: String): String = baseUrl
		.replace("https://github.com/", "https://raw.githubusercontent.com/") + resourcePath.trim('/')


	private fun verifySignature(signedContent: SignedContent): Boolean {
		val content = objectMapper.writeValueAsString(signedContent.content)
		val signature = Base64.getDecoder().decode(signedContent.signature)
		val contentHash = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
		val verifier = Ed25519Signer()
		verifier.init(false, publicSigningKey)
		verifier.update(contentHash, 0, contentHash.size)
		return verifier.verifySignature(signature)
	}

	private suspend inline fun <reified T> downloadAndVerifyResource(baseUrl: String, resourcePath: String): T {
		val resourceUrl = gitHubRawUrlForResource(baseUrl, resourcePath)
		val signedContent = httpClient.get(resourceUrl).body<SignedContent>()
		check(verifySignature(signedContent)) {
			"Cannot verify the signature for the manifest"
		}
		return objectMapper.treeToValue(signedContent.content)
	}

	private suspend fun getOrDownloadManifest(repoUrl: String): Map<String, String> = manifestCache.getIfPresent(repoUrl) ?:
		downloadAndVerifyResource<Map<String, String>>(repoUrl, "output/manifest.json").also {
			manifestCache.put(repoUrl, it)
		}

	protected suspend fun loadExternalViews(repoUrl: String, entityClass: Class<*>, partition: String): ExternalViewRepository? =
		getOrDownloadManifest(repoUrl)[entityClass.simpleName.lowercase()]?.let { viewUrl ->
			val views = downloadAndVerifyResource<Map<String, View>>(repoUrl, viewUrl)
			ExternalViewRepository(
				secondaryPartition = partition,
				klass = entityClass,
				views = views
			)
		}

}