package com.jd.audioextractor.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale

class AudioExtractorEngine(private val context: Context) {

    suspend fun inspectVideo(uri: Uri): VideoInfo = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        var fileName = "video_${System.currentTimeMillis()}"
        var fileSize = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    cursor.getString(nameIndex)?.let { fileName = it }
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val pfd = contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Could not open selected video file.")

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(pfd.fileDescriptor)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("No audio track detected in this video file.")
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/unknown"
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val isDirectMuxable = mime.equals("audio/mp4a-latm", ignoreCase = true) ||
                    mime.equals("audio/aac", ignoreCase = true) ||
                    mime.equals("audio/opus", ignoreCase = true)

            val codecDisplay = when {
                mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
                mime.contains("opus", ignoreCase = true) -> "Opus"
                mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
                mime.contains("ac3", ignoreCase = true) -> "Dolby AC-3"
                mime.contains("flac", ignoreCase = true) -> "FLAC"
                else -> mime.substringAfter("audio/").uppercase(Locale.ROOT)
            }

            VideoInfo(
                fileName = fileName,
                fileSizeFormatted = formatFileSize(fileSize),
                durationMs = durationUs / 1000,
                audioCodec = codecDisplay,
                sampleRate = sampleRate,
                channelCount = channels,
                isDirectMuxable = isDirectMuxable
            )
        } finally {
            extractor.release()
            pfd.close()
        }
    }

    suspend fun extractAudio(
        videoUri: Uri,
        outputFormatExtension: String, // "m4a" or "webm"
        outputFile: File,
        onProgress: (Float, Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val pfd = context.contentResolver.openFileDescriptor(videoUri, "r")
            ?: throw IllegalStateException("Cannot access video file stream.")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(pfd.fileDescriptor)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("Selected video does not have an audio track.")
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""

            // Determine if direct remuxing is possible or if transcoding is preferred
            val canDirectMux = if (outputFormatExtension.equals("webm", ignoreCase = true)) {
                mime.contains("opus", ignoreCase = true) || mime.contains("vorbis", ignoreCase = true)
            } else {
                mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true)
            }

            if (canDirectMux) {
                // Direct zero-reencode demux & mux (Ultra fast!)
                runDirectDemux(
                    extractor = extractor,
                    audioTrackIndex = audioTrackIndex,
                    audioFormat = audioFormat,
                    outputFile = outputFile,
                    isWebm = outputFormatExtension.equals("webm", ignoreCase = true),
                    onProgress = onProgress
                )
            } else {
                // Transcode stream to clean AAC in MPEG-4 (.m4a) container
                runTranscodeToAac(
                    extractor = extractor,
                    audioTrackIndex = audioTrackIndex,
                    inputFormat = audioFormat,
                    outputFile = outputFile,
                    onProgress = onProgress
                )
            }

            outputFile
        } finally {
            try {
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
            try {
                pfd.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun runDirectDemux(
        extractor: MediaExtractor,
        audioTrackIndex: Int,
        audioFormat: MediaFormat,
        outputFile: File,
        isWebm: Boolean,
        onProgress: (Float, Int) -> Unit
    ) {
        val muxerFormat = if (isWebm) {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        } else {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }

        val muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
        try {
            extractor.selectTrack(audioTrackIndex)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxInputSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
            } else {
                128 * 1024
            }

            val buffer = ByteBuffer.allocateDirect(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            var lastReportTime = 0L

            while (currentCoroutineContext().isActive) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    break
                }

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastReportTime > 50 && durationUs > 0) {
                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress, (progress * 100).toInt())
                    lastReportTime = currentTime
                }

                extractor.advance()
            }
            onProgress(1f, 100)
        } finally {
            try {
                muxer.stop()
            } catch (_: Exception) {}
            try {
                muxer.release()
            } catch (_: Exception) {}
        }
    }

    private suspend fun runTranscodeToAac(
        extractor: MediaExtractor,
        audioTrackIndex: Int,
        inputFormat: MediaFormat,
        outputFile: File,
        onProgress: (Float, Int) -> Unit
    ) {
        val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/unknown"
        val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else 44100
        val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 2
        val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
            inputFormat.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        extractor.selectTrack(audioTrackIndex)

        // Setup Decoder
        val decoder = MediaCodec.createDecoderByType(inputMime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        // Setup AAC Encoder
        val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 192000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerAudioTrack = -1
        var muxerStarted = false

        val decoderBufferInfo = MediaCodec.BufferInfo()
        val encoderBufferInfo = MediaCodec.BufferInfo()

        var extractorEos = false
        var decoderEos = false
        var encoderEos = false
        var lastReportTime = 0L

        try {
            while (!encoderEos && currentCoroutineContext().isActive) {
                // Feed Extractor -> Decoder
                if (!extractorEos) {
                    val inIndex = decoder.dequeueInputBuffer(2500)
                    if (inIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorEos = true
                            } else {
                                decoder.queueInputBuffer(
                                    inIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    extractor.sampleFlags
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                // Decoder -> Encoder (PCM transfer)
                if (!decoderEos) {
                    val outIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, 2500)
                    if (outIndex >= 0) {
                        val decodedBuffer = decoder.getOutputBuffer(outIndex)
                        if (decodedBuffer != null) {
                            val encInIndex = encoder.dequeueInputBuffer(2500)
                            if (encInIndex >= 0) {
                                val encoderInBuf = encoder.getInputBuffer(encInIndex)
                                if (encoderInBuf != null) {
                                    encoderInBuf.clear()
                                    if (decoderBufferInfo.size > 0) {
                                        decodedBuffer.position(decoderBufferInfo.offset)
                                        decodedBuffer.limit(decoderBufferInfo.offset + decoderBufferInfo.size)
                                        encoderInBuf.put(decodedBuffer)
                                    }
                                    val flags = if (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                        decoderEos = true
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    } else 0

                                    encoder.queueInputBuffer(
                                        encInIndex,
                                        0,
                                        decoderBufferInfo.size,
                                        decoderBufferInfo.presentationTimeUs,
                                        flags
                                    )
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                    }
                }

                // Encoder -> Muxer
                val encOutIndex = encoder.dequeueOutputBuffer(encoderBufferInfo, 2500)
                if (encOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw IllegalStateException("Encoder format changed twice")
                    }
                    val newFormat = encoder.outputFormat
                    muxerAudioTrack = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (encOutIndex >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(encOutIndex)
                    if (encodedBuffer != null && muxerStarted) {
                        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && encoderBufferInfo.size > 0) {
                            muxer.writeSampleData(muxerAudioTrack, encodedBuffer, encoderBufferInfo)

                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastReportTime > 50 && durationUs > 0) {
                                val progress = (encoderBufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                                onProgress(progress, (progress * 100).toInt())
                                lastReportTime = currentTime
                            }
                        }
                        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderEos = true
                        }
                    }
                    encoder.releaseOutputBuffer(encOutIndex, false)
                }
            }
            onProgress(1f, 100)
        } finally {
            try { decoder.stop(); decoder.release() } catch (_: Exception) {}
            try { encoder.stop(); encoder.release() } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (_: Exception) {}
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
