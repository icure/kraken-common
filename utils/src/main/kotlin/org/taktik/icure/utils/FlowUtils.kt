@file:Suppress("unused")

package org.taktik.icure.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.LinkedList


/**
 * Injects the reactor context in a [Flow] and converts it to a [Flux].
 */
fun <T : Any> Flow<T>.injectReactorContext(): Flux<T> =
    Mono.deferContextual { Mono.just(it) }.flatMapMany { ctxView ->
        this.flowOn(Context.of(ctxView).asCoroutineContext() + Dispatchers.Default).asFlux()
    }

suspend fun <K, V> Flow<Pair<K, V>>.toMap(): Map<K, V> {
    val res = mutableMapOf<K, V>()
    collect { res[it.first] = it.second }
    return res
}

suspend fun Flow<ByteBuffer>.toInputStream(): InputStream = withContext(IO) {
    val buffers = toList()

    object : InputStream() {
        var idx = 0
        override fun available(): Int = buffers.subList(idx, buffers.size).fold(0) { sum, bb -> sum + bb.remaining() }

        @Throws(java.io.IOException::class)
        override fun read(): Int = if (buffers[idx].hasRemaining()) (buffers[idx].get().toUInt() and 0xffu).toInt() else {
            if (idx < buffers.size - 1) {
                idx++
                read()
            } else -1
        }

        @Throws(java.io.IOException::class)
        override fun read(bytes: ByteArray, off: Int, len: Int): Int = buffers[idx].let { buf ->
            when {
                len == 0 -> 0
                !buf.hasRemaining() -> {
                    if (idx < buffers.size - 1) {
                        idx++
                        read(bytes, off, len)
                    } else -1
                }
                else -> {
                    val read = len.coerceAtMost(buf.remaining())
                    buf.get(bytes, off, read)
                    if (len == read) read else read + read(bytes, off + read, len - read).coerceAtLeast(0)
                }
            }
        }
    }
}

fun <T> Flow<T>.distinctIf(condition: Boolean) =
    if (condition) this.distinct()
    else this

fun <T> Flow<T>.distinct(): Flow<T> = flow {
    val previous = HashSet<T>()
    collect { value: T ->
        if (!previous.contains(value)) {
            previous.add(value)
            emit(value)
        }
    }
}

fun <T> Flow<T>.distinctBy(function: (T) -> Any?): Flow<T> = flow {
    val previous = HashSet<Any>()
    collect { value: T ->
        val fnVal = function(value)
        if (!previous.contains(fnVal)) {
            fnVal?.let { previous.add(it) }
            emit(value)
        }
    }
}

fun <T> Flow<T>.subsequentDistinctBy(function: (T) -> Any?): Flow<T> = flow {
    var previousId: Any? = null
    collect { value: T ->
        val id = function(value)
        if (previousId == null || id != previousId) {
            emit(value)
            previousId = id
        }
    }
}

fun <T> Flow<T>.bufferedChunks(min: Int, max: Int): Flow<List<T>> = channelFlow {
    require(min >= 1 && max >= 1 && max >= min) {
        "Min and max chunk sizes should be greater than 0, and max >= min"
    }
    var buffer = ArrayList<T>(max)
    collect {
        buffer.add(it)
        if (buffer.size >= max) {
            send(buffer.toList())
            buffer.clear()
        } else if (min <= buffer.size) {
            // Avoid creating unused copies of the list if consumer not ready
            val offered = this.trySend(buffer).isSuccess
            if (offered) {
                buffer = ArrayList(max)
            }
        }
    }
    if (buffer.size > 0) send(buffer.toList())
}.buffer(0)

suspend fun Flow<ByteBuffer>.writeTo(os: OutputStream) {
    this.collect { it.writeTo(os) }
}

fun InputStream.toFlow() = flow {
    do {
        val buffer = ByteArray(512)
        val read = this@toFlow.read(buffer, 0, 512)
        if (read > 0) {
            emit(ByteBuffer.wrap(buffer, 0, read))
        }
    } while (read >= 0)
}.flowOn(IO)

/**
 * Drop the first [n] bytes from this flow.
 */
fun Flow<DataBuffer>.dropBytes(n: Long): Flow<DataBuffer> =
    if (n > 0) DataBufferUtils.skipUntilByteCount(asPublisher(), n).asFlow() else this

/* TODO check if other implementation is more efficient and is also correct (does never leave trailing zeroes)
DataBufferUtils.join(asPublisher()).awaitFirst().asByteBuffer().array()
 */
suspend fun Flow<DataBuffer>.toByteArray(releaseBuffers: Boolean): ByteArray =
    ByteArrayOutputStream().use { os ->
        collect {
            it.asByteBuffer().writeTo(os)
            if (releaseBuffers) DataBufferUtils.release(it)
        }
        os.toByteArray()
    }

suspend fun Flow<ByteBuffer>.toByteArray(): ByteArray =
    ByteArrayOutputStream().use { os ->
        collect { it.writeTo(os) }
        os.toByteArray()
    }

private fun ByteBuffer.writeTo(os: OutputStream): Unit =
    if (hasArray() && hasRemaining()) {
        os.write(array(), position() + arrayOffset(), remaining())
    } else {
        os.write(ByteArray(remaining()).also { get(it) })
    }

/**
 * Merges multiple data buffers together until the flow is fully exhausted or at least [size] bytes of
 * data was collected. The returned flow will consist of a single [DataBuffer] with size strictly smaller
 * than [size] or it will be a flow of one or more [DataBuffer]s where the first has size greater than
 * or equal to [size]
 */
suspend fun Flow<DataBuffer>.bufferFirstSize(size: Int): Flow<DataBuffer> = flow {
    var accumulatedSize = 0
    val buffers = mutableListOf<DataBuffer>()
    var didEmitBuffers = false

    suspend fun emitAccumulatedBuffers() {
        emit(DataBufferUtils.join(buffers.asFlow().asPublisher()).awaitFirst())
        didEmitBuffers = true
    }

    this@bufferFirstSize.collect {
        if (didEmitBuffers) {
            emit(it)
        } else {
            buffers += it
            accumulatedSize += it.readableByteCount()
            if (accumulatedSize >= size) emitAccumulatedBuffers()
        }
    }
    if (!didEmitBuffers && buffers.isNotEmpty()) emitAccumulatedBuffers()
}

fun <T> interleaveFlows(flows: Iterable<Flow<T>>): Flow<T> = channelFlow {
    val channels = flows.mapTo(LinkedList()) { flow ->
        Channel<T>(Channel.RENDEZVOUS).also { channel ->
            launch {
                flow.collect {
                    channel.send(it)
                }
                channel.close()
            }
        }
    }

    while (channels.isNotEmpty()) {
        val channelsIterator = channels.iterator()
        while (channelsIterator.hasNext()) {
            val value = channelsIterator.next().receiveCatching()
            if (value.isClosed) {
                channelsIterator.remove()
            } else {
                send(value.getOrThrow())
            }
        }
    }
}.buffer(1)