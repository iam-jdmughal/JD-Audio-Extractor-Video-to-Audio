package com.jd.audioextractor.data

data class VideoInfo(
    val fileName: String,
    val fileSizeFormatted: String,
    val durationMs: Long,
    val audioCodec: String,
    val sampleRate: Int,
    val channelCount: Int,
    val isDirectMuxable: Boolean
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

sealed class ExtractionState {
    object Idle : ExtractionState()
    data class Analyzing(val message: String = "Analyzing media streams...") : ExtractionState()
    data class Processing(
        val progress: Float, // 0.0f to 1.0f
        val percent: Int,
        val statusMessage: String
    ) : ExtractionState()
    data class Success(
        val outputFilePath: String,
        val outputFileName: String,
        val fileSizeFormatted: String,
        val durationFormatted: String
    ) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
}
