package com.smartisanos.music.ui.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ScratchPlaybackCycleMs = 3_500f
private const val ScratchMinMotionDegrees = 0.18f
private const val ScratchIdleTimeoutMs = 110L
private const val ScratchDecodeTimeoutUs = 10_000L
private const val ScratchMinDeltaTimeMs = 8L
private const val ScratchMaxDeltaTimeMs = 72L
private const val ScratchOutputFrames = 384
private const val ScratchOutputChannels = 2
private const val ScratchOutputGain = 1.08f
private const val ScratchRateSmoothing = 0.35f
private const val ScratchMaxPlaybackRatePermille = 6_000
private const val ScratchMaxDecodedDurationMs = 12 * 60 * 1_000L

internal class ScratchSoundController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private val playbackLock = java.lang.Object()
    private val playbackThread = Thread(::playbackLoop, "ScratchAudioTrack").apply {
        isDaemon = true
        start()
    }

    @Volatile
    private var released = false

    @Volatile
    private var sourceGeneration = 0

    @Volatile
    private var preparedSourceKey: String? = null

    @Volatile
    private var loadingSourceKey: String? = null

    @Volatile
    private var scratchBuffer: ScratchBuffer? = null

    @Volatile
    private var scratchActive = false

    @Volatile
    private var scratchPositionMs = 0L

    @Volatile
    private var requestedDirection = 0

    @Volatile
    private var smoothedPlaybackRatePermille = 0f

    @Volatile
    private var motionGeneration = 0L

    @Volatile
    private var lastMotionRealtimeMs = 0L

    private var lastDirection = 0
    private var audioTrack: AudioTrack? = null
    private var audioTrackSampleRate = 0
    private var playbackCursor = 0.0
    private var appliedMotionGeneration = -1L
    private var appliedSourceKey: String? = null
    private val stereoWriteBuffer = ShortArray(ScratchOutputFrames * ScratchOutputChannels)

    fun prepareSource(sourceUri: Uri?) {
        val sourceKey = sourceUri?.toString()
        if (sourceKey.isNullOrEmpty()) {
            sourceGeneration += 1
            preparedSourceKey = null
            loadingSourceKey = null
            scratchBuffer = null
            synchronized(playbackLock) {
                playbackLock.notifyAll()
            }
            return
        }
        if (preparedSourceKey == sourceKey || loadingSourceKey == sourceKey || released) {
            return
        }
        val generation = sourceGeneration + 1
        sourceGeneration = generation
        loadingSourceKey = sourceKey
        decodeExecutor.execute {
            val decodedBuffer = decodeScratchBuffer(appContext, sourceUri)
            if (released || generation != sourceGeneration) {
                return@execute
            }
            scratchBuffer = decodedBuffer
            preparedSourceKey = sourceKey
            loadingSourceKey = null
            synchronized(playbackLock) {
                playbackLock.notifyAll()
            }
        }
    }

    fun onScratchStart(
        sourceUri: Uri?,
        positionMs: Long,
    ) {
        prepareSource(sourceUri)
        scratchPositionMs = positionMs
        scratchActive = true
        requestedDirection = 0
        smoothedPlaybackRatePermille = 0f
        lastDirection = 0
        lastMotionRealtimeMs = 0L
        motionGeneration += 1
        synchronized(playbackLock) {
            playbackLock.notifyAll()
        }
    }

    fun onScratchMotion(
        positionMs: Long,
        deltaAngleDegrees: Float,
    ) {
        val magnitude = abs(deltaAngleDegrees)
        if (magnitude < ScratchMinMotionDegrees || released) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        val deltaTimeMs = when {
            lastMotionRealtimeMs == 0L -> 16L
            else -> (now - lastMotionRealtimeMs).coerceIn(
                ScratchMinDeltaTimeMs,
                ScratchMaxDeltaTimeMs,
            )
        }
        lastMotionRealtimeMs = now

        val direction = if (deltaAngleDegrees >= 0f) 1 else -1
        val rawRatePermille = scratchPlaybackRatePermille(magnitude, deltaTimeMs).toFloat()
        smoothedPlaybackRatePermille = if (direction != lastDirection || smoothedPlaybackRatePermille == 0f) {
            rawRatePermille
        } else {
            smoothedPlaybackRatePermille +
                ((rawRatePermille - smoothedPlaybackRatePermille) * ScratchRateSmoothing)
        }
        lastDirection = direction
        requestedDirection = direction
        scratchPositionMs = positionMs
        scratchActive = true
        motionGeneration += 1
        synchronized(playbackLock) {
            playbackLock.notifyAll()
        }
    }

    fun stop() {
        scratchActive = false
        requestedDirection = 0
        smoothedPlaybackRatePermille = 0f
        lastDirection = 0
        lastMotionRealtimeMs = 0L
        pauseAndFlushTrack()
        synchronized(playbackLock) {
            playbackLock.notifyAll()
        }
    }

    fun release() {
        stop()
        released = true
        sourceGeneration += 1
        decodeExecutor.shutdownNow()
        synchronized(playbackLock) {
            playbackLock.notifyAll()
        }
        playbackThread.join(200)
        releaseAudioTrack()
    }

    private fun playbackLoop() {
        while (!released) {
            val buffer = scratchBuffer
            val idleForTooLong = lastMotionRealtimeMs != 0L &&
                (SystemClock.elapsedRealtime() - lastMotionRealtimeMs) > ScratchIdleTimeoutMs
            val shouldPlay = scratchActive &&
                buffer != null &&
                requestedDirection != 0 &&
                smoothedPlaybackRatePermille > 0f &&
                !idleForTooLong

            if (!shouldPlay || buffer.monoSamples.isEmpty()) {
                pauseAndFlushTrack()
                synchronized(playbackLock) {
                    if (!released) {
                        playbackLock.wait(24L)
                    }
                }
                continue
            }

            val track = ensureAudioTrack(buffer.sampleRate)
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }

            val localMotionGeneration = motionGeneration
            if (buffer.sourceKey != appliedSourceKey || localMotionGeneration != appliedMotionGeneration) {
                playbackCursor = buffer.positionMsToSample(scratchPositionMs)
                appliedSourceKey = buffer.sourceKey
                appliedMotionGeneration = localMotionGeneration
            }

            val frameStep = (smoothedPlaybackRatePermille / 1_000f) * requestedDirection.toDouble()
            fillStereoBuffer(buffer, frameStep)
            track.write(
                stereoWriteBuffer,
                0,
                stereoWriteBuffer.size,
                AudioTrack.WRITE_BLOCKING,
            )
        }
    }

    private fun fillStereoBuffer(
        buffer: ScratchBuffer,
        frameStep: Double,
    ) {
        val lastIndex = buffer.monoSamples.lastIndex
        if (lastIndex < 0) {
            stereoWriteBuffer.fill(0)
            return
        }
        var outputIndex = 0
        repeat(ScratchOutputFrames) {
            val sample = if (
                (frameStep >= 0.0 && playbackCursor > lastIndex.toDouble()) ||
                (frameStep < 0.0 && playbackCursor < 0.0)
            ) {
                0
            } else {
                interpolateMonoSample(buffer.monoSamples, playbackCursor)
            }
            val amplified = (sample * ScratchOutputGain)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            stereoWriteBuffer[outputIndex++] = amplified
            stereoWriteBuffer[outputIndex++] = amplified
            playbackCursor += frameStep
        }
    }

    private fun ensureAudioTrack(sampleRate: Int): AudioTrack {
        audioTrack?.takeIf { audioTrackSampleRate == sampleRate }?.let { return it }
        releaseAudioTrack()

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val safeBufferSize = maxOf(
            minBufferSize.coerceAtLeast(0),
            stereoWriteBuffer.size * Short.SIZE_BYTES * 4,
        )
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(safeBufferSize)
            .build()
            .also {
                audioTrack = it
                audioTrackSampleRate = sampleRate
            }
    }

    private fun pauseAndFlushTrack() {
        val track = audioTrack ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
            track.flush()
        }
    }

    private fun releaseAudioTrack() {
        audioTrack?.release()
        audioTrack = null
        audioTrackSampleRate = 0
    }
}

internal fun scratchPlaybackRatePermille(
    deltaAngleDegrees: Float,
    deltaTimeMs: Long,
): Int {
    val scaledRate = (
        ScratchPlaybackCycleMs * deltaAngleDegrees /
            (360f * deltaTimeMs.coerceIn(ScratchMinDeltaTimeMs, ScratchMaxDeltaTimeMs).toFloat())
    ) * 1_000f
    return scaledRate
        .roundToInt()
        .coerceIn(0, ScratchMaxPlaybackRatePermille)
}

private fun interpolateMonoSample(
    samples: ShortArray,
    sampleIndex: Double,
): Int {
    if (samples.isEmpty()) {
        return 0
    }
    val clampedIndex = sampleIndex.coerceIn(0.0, samples.lastIndex.toDouble())
    val leftIndex = clampedIndex.toInt()
    val rightIndex = (leftIndex + 1).coerceAtMost(samples.lastIndex)
    val fraction = clampedIndex - leftIndex
    val left = samples[leftIndex].toDouble()
    val right = samples[rightIndex].toDouble()
    return (left + ((right - left) * fraction)).roundToInt()
}

private data class ScratchBuffer(
    val sourceKey: String,
    val sampleRate: Int,
    val monoSamples: ShortArray,
) {
    fun positionMsToSample(positionMs: Long): Double {
        if (monoSamples.isEmpty()) {
            return 0.0
        }
        val samplePosition = positionMs.coerceAtLeast(0L) * sampleRate.toDouble() / 1_000.0
        return samplePosition.coerceIn(0.0, monoSamples.lastIndex.toDouble())
    }
}

private fun decodeScratchBuffer(
    context: Context,
    sourceUri: Uri,
): ScratchBuffer? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    return try {
        extractor.setDataSource(context, sourceUri, null)
        val trackIndex = findAudioTrackIndex(extractor)
        if (trackIndex == -1) {
            null
        } else {
            extractor.selectTrack(trackIndex)
            val sourceFormat = extractor.getTrackFormat(trackIndex)
            val sourceKey = sourceUri.toString()
            if (
                sourceFormat.containsKey(MediaFormat.KEY_DURATION) &&
                sourceFormat.getLong(MediaFormat.KEY_DURATION) / 1_000L > ScratchMaxDecodedDurationMs
            ) {
                return null
            }
            val mimeType = sourceFormat.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(sourceFormat, null, null, 0)
                start()
            }
            val decoder = codec ?: return null

            val initialSampleRate = sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val initialCapacity = buildInitialSampleCapacity(sourceFormat, initialSampleRate)
            val sampleBuilder = MonoSampleBuilder(initialCapacity)
            val bufferInfo = MediaCodec.BufferInfo()
            var outputSampleRate = initialSampleRate
            var outputChannelCount = sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var inputEnded = false
            var outputEnded = false

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(ScratchDecodeTimeoutUs)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex) ?: break
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                extractor.sampleFlags,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, ScratchDecodeTimeoutUs)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        outputSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        outputChannelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else {
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> {
                        if (outputIndex >= 0) {
                            if (bufferInfo.size > 0) {
                                val outputBuffer = decoder.getOutputBuffer(outputIndex)
                                    ?.duplicate()
                                    ?.apply {
                                        position(bufferInfo.offset)
                                        limit(bufferInfo.offset + bufferInfo.size)
                                    }
                                if (outputBuffer != null) {
                                    sampleBuilder.append(
                                        outputBuffer,
                                        outputChannelCount,
                                        pcmEncoding,
                                    )
                                }
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputEnded = true
                            }
                        }
                    }
                }
            }

            val monoSamples = sampleBuilder.build()
            if (monoSamples.isEmpty()) {
                null
            } else {
                ScratchBuffer(
                    sourceKey = sourceKey,
                    sampleRate = outputSampleRate,
                    monoSamples = monoSamples,
                )
            }
        }
    } catch (_: Throwable) {
        null
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        extractor.release()
    }
}

private fun findAudioTrackIndex(extractor: MediaExtractor): Int {
    repeat(extractor.trackCount) { index ->
        val format = extractor.getTrackFormat(index)
        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return@repeat
        if (mimeType.startsWith("audio/")) {
            return index
        }
    }
    return -1
}

private fun buildInitialSampleCapacity(
    format: MediaFormat,
    sampleRate: Int,
): Int {
    if (!format.containsKey(MediaFormat.KEY_DURATION)) {
        return sampleRate * 30
    }
    val durationUs = format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(0L)
    val projectedSamples = (durationUs * sampleRate / 1_000_000L)
        .coerceAtLeast(sampleRate.toLong())
        .coerceAtMost(sampleRate.toLong() * ScratchMaxDecodedDurationMs / 1_000L)
    return projectedSamples.toInt()
}

private class MonoSampleBuilder(initialCapacity: Int) {
    private var buffer = ShortArray(initialCapacity.coerceAtLeast(2_048))
    private var size = 0

    fun append(
        outputBuffer: ByteBuffer,
        channelCount: Int,
        pcmEncoding: Int,
    ) {
        when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> appendFloat(outputBuffer, channelCount)
            else -> append16Bit(outputBuffer, channelCount)
        }
    }

    fun build(): ShortArray = buffer.copyOf(size)

    private fun append16Bit(
        outputBuffer: ByteBuffer,
        channelCount: Int,
    ) {
        val channels = channelCount.coerceAtLeast(1)
        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frameCount = shortBuffer.remaining() / channels
        ensureCapacity(size + frameCount)
        repeat(frameCount) {
            var mixedSample = 0f
            repeat(channels) {
                mixedSample += shortBuffer.get().toFloat()
            }
            buffer[size++] = (mixedSample / channels.toFloat())
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun appendFloat(
        outputBuffer: ByteBuffer,
        channelCount: Int,
    ) {
        val channels = channelCount.coerceAtLeast(1)
        val floatBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val frameCount = floatBuffer.remaining() / channels
        ensureCapacity(size + frameCount)
        repeat(frameCount) {
            var mixedSample = 0f
            repeat(channels) {
                mixedSample += floatBuffer.get()
            }
            buffer[size++] = ((mixedSample / channels.toFloat()) * Short.MAX_VALUE.toFloat())
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun ensureCapacity(requiredSize: Int) {
        if (requiredSize <= buffer.size) {
            return
        }
        var newSize = buffer.size
        while (newSize < requiredSize) {
            newSize *= 2
        }
        buffer = buffer.copyOf(newSize)
    }
}
