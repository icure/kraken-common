package org.taktik.icure.asyncdao.components

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactive.awaitFirst
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.taktik.couchdb.entity.View
import org.taktik.couchdb.support.views.ExternalViewRepository
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.*

@Component
@ConditionalOnProperty(prefix = "icure.couchdb.external.loading", name = ["publicSigningKey"], matchIfMissing = false)
class ExternalViewsLoader(
	private val objectMapper: ObjectMapper,
	@Value("\${icure.couchdb.external.loading.publicSigningKey}") rawPublicSigningKey: String
) {

	companion object {
		private const val ONE_MEGABYTE = 1_048_576L

		data class SignedContent(
			val jsonContent: JsonNode,
			val signature: String
		)
	}

	// A very small local cache that stores the manifest for a short time, to avoid reloading it multiple times
	// when forcing the indexation of the design documents for all the entities.
	// Since this will happen in a single call, there is no need for it to be replicated among nodes.
	private val manifestCache = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(5))
		.build<String, Map<String, String>>()

	private val publicSigningKey = rawPublicSigningKey.split(" ")[1].let {
		OpenSSHPublicKeyUtil.parsePublicKey(Base64.getDecoder().decode(it))
	}

	private val httpClient = HttpClient.create()

	private fun preprocessResourcePath(baseUrl: String, resourcePath: String): String = baseUrl
		.replace("https://github.com/", "https://raw.githubusercontent.com/")
		.trimEnd('/') + "/" + resourcePath.trim('/')

	private fun verifySignature(signedContent: SignedContent): Boolean {
		val content = objectMapper.writeValueAsString(signedContent.jsonContent)
		val signature = Base64.getDecoder().decode(signedContent.signature)
		val contentHash = MessageDigest.getInstance("SHA-256").digest(content.toByteArray())
		val verifier = Ed25519Signer()
		verifier.init(false, publicSigningKey)
		verifier.update(contentHash, 0, contentHash.size)
		return verifier.verifySignature(signature)
	}

	private suspend inline fun <reified T> downloadAndVerifyResource(baseUrl: String, resourcePath: String): T {
		val resourceUrl = preprocessResourcePath(baseUrl, resourcePath)
		val signedContent = httpClient.get()
			.uri(URI(resourceUrl))
			.responseSingle { response, body ->
				if (!response.status().code().let { it in 200..299 }) {
					throw IllegalStateException("Resource not found: $resourceUrl")
				}
				val contentLength = response.responseHeaders().get("Content-Length")?.toLongOrNull()
				if (contentLength == null || contentLength > ONE_MEGABYTE) {
					throw IllegalStateException("Content length is missing or exceeding 1MB")
				}
				body.asString(StandardCharsets.UTF_8)
			}.awaitFirst().let {
				objectMapper.readValue<SignedContent>(it)
			}
		check(verifySignature(signedContent)) {
			"Cannot verify the signature for the manifest"
		}
		return objectMapper.treeToValue(signedContent.jsonContent)
	}

	private suspend fun getOrDownloadManifest(repoUrl: String): Map<String, String> = manifestCache.getIfPresent(repoUrl) ?:
		downloadAndVerifyResource<Map<String, String>>(repoUrl, "output/manifest.json").also {
			manifestCache.put(repoUrl, it)
		}

	suspend fun loadExternalViews(entityClass: Class<*>, repoUrl: String, partition: String): ExternalViewRepository? =
		getOrDownloadManifest(repoUrl)[entityClass.simpleName.lowercase()]?.let { viewUrl ->
			val views = downloadAndVerifyResource<Map<String, View>>(repoUrl, viewUrl)
			ExternalViewRepository(
				secondaryPartition = partition,
				klass = entityClass,
				views = views
			)
		}

}