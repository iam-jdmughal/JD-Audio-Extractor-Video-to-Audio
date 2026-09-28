package com.jd.audioextractor.ui

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jd.audioextractor.data.AudioExtractorEngine
import com.jd.audioextractor.data.ExtractionState
import com.jd.audioextractor.data.MediaStoreSaver
import com.jd.audioextractor.data.VideoInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class AudioExtractorViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = AudioExtractorEngine(application)
    private val mediaSaver = MediaStoreSaver(application)

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _selectedFormat = MutableStateFlow("m4a")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    private val _isSavedToMusic = MutableStateFlow(false)
    val isSavedToMusic: StateFlow<Boolean> = _isSavedToMusic.asStateFlow()

    // Audio Playback
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPlayMs = MutableStateFlow(0)
    val currentPlayMs: StateFlow<Int> = _currentPlayMs.asStateFlow()

    private val _audioDurationMs = MutableStateFlow(0)
    val audioDurationMs: StateFlow<Int> = _audioDurationMs.asStateFlow()

    private var currentOutputFile: File? = null

    fun selectVideo(uri: Uri) {
        stopPlayback()
        _selectedVideoUri.value = uri
        _extractionState.value = ExtractionState.Analyzing()
        _isSavedToMusic.value = false
        currentOutputFile = null

        viewModelScope.launch {
            try {
                val info = engine.inspectVideo(uri)
                _videoInfo.value = info
                _extractionState.value = ExtractionState.Idle
            } catch (e: Exception) {
                _videoInfo.value = null
                _extractionState.value = ExtractionState.Error(e.message ?: "Failed to inspect video.")
            }
        }
    }

    fun setFormat(format: String) {
        _selectedFormat.value = format
    }

    fun startExtraction() {
        val uri = _selectedVideoUri.value ?: return
        val info = _videoInfo.value ?: return

        stopPlayback()
        _isSavedToMusic.value = false

        val ext = _selectedFormat.value
        val outputFile = mediaSaver.createTempOutputFile(info.fileName, ext)
        currentOutputFile = outputFile

        _extractionState.value = ExtractionState.Processing(0f, 0, "Extracting audio track...")

        viewModelScope.launch {
            try {
                val resultFile = engine.extractAudio(
                    videoUri = uri,
                    outputFormatExtension = ext,
                    outputFile = outputFile,
                    onProgress = { progress, percent ->
                        _extractionState.value = ExtractionState.Processing(
                            progress = progress,
                            percent = percent,
                            statusMessage = "Demuxing: $percent%"
                        )
                    }
                )

                val fileSize = formatFileSize(resultFile.length())
                _extractionState.value = ExtractionState.Success(
                    outputFilePath = resultFile.absolutePath,
                    outputFileName = resultFile.name,
                    fileSizeFormatted = fileSize,
                    durationFormatted = info.durationFormatted
                )
                initMediaPlayer(resultFile)
            } catch (e: Exception) {
                _extractionState.value = ExtractionState.Error(e.message ?: "Extraction failed.")
            }
        }
    }

    private fun initMediaPlayer(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                _audioDurationMs.value = duration
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    _currentPlayMs.value = 0
                }
            }
        } catch (_: Exception) {}
    }

    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            playbackJob?.cancel()
        } else {
            player.start()
            _isPlaying.value = true
            startPlaybackTracker()
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        val total = _audioDurationMs.value
        if (total > 0) {
            val targetMs = (progress * total).toInt()
            player.seekTo(targetMs)
            _currentPlayMs.value = targetMs
            _playbackProgress.value = progress
        }
    }

    private fun startPlaybackTracker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition
                        val total = player.duration
                        _currentPlayMs.value = current
                        if (total > 0) {
                            _playbackProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
                delay(200)
            }
        }
    }

    fun saveToMusicFolder() {
        val file = currentOutputFile ?: return
        val mime = if (file.extension.equals("webm", ignoreCase = true)) "audio/webm" else "audio/mp4"

        viewModelScope.launch {
            val savedUri = mediaSaver.saveToMusicLibrary(file, file.name, mime)
            if (savedUri != null) {
                _isSavedToMusic.value = true
            }
        }
    }

    fun shareExtractedAudio() {
        val file = currentOutputFile ?: return
        mediaSaver.shareAudio(file, file.name)
    }

    fun reset() {
        stopPlayback()
        _selectedVideoUri.value = null
        _videoInfo.value = null
        _extractionState.value = ExtractionState.Idle
        _isSavedToMusic.value = false
        currentOutputFile = null
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
        _currentPlayMs.value = 0
        _audioDurationMs.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
