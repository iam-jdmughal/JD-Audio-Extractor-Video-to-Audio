# 🎵 JD Audio Extractor (Video to Audio)

A lightweight, ultra-fast Android application built with **Kotlin** and **Jetpack Compose** that extracts audio tracks directly from video files without bulky FFmpeg binaries.

---

## ⚡ Highlights

- **Instant Extraction**: Uses native Android `MediaExtractor` and `MediaMuxer` APIs for direct hardware stream demuxing. Zero re-encoding overhead.
- **Lossless Quality**: Preserves the original audio stream quality (AAC / M4A).
- **Ultra Lightweight**: No external C++ `.so` libraries or FFmpeg dependencies. Negligible APK footprint.
- **Modern UI**: Clean Material 3 dark interface with real-time progress indicators and integrated audio playback.
- **Scoped Storage Compliant**: Easily save extracted tracks directly to your device's Music library or share with external apps.

---

## 🛠️ Built With

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Platform APIs**: `android.media.MediaExtractor`, `android.media.MediaMuxer`, `android.media.MediaPlayer`
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 16 (API 36)

---

## 🚀 Building from Source

Clone the repository and build with Gradle:

```bash
git clone https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio.git
cd JD-Audio-Extractor-Video-to-Audio
./gradlew assembleDebug
```

The debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).
