<div align="center">

# 🎵 JD Audio Extractor
### Ultra-Fast Lossless Video to Audio Converter for Android

[![GitHub Release](https://img.shields.io/github/v/release/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio?color=2563EB&style=flat-square)](https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio/releases)
[![APK Size](https://img.shields.io/badge/APK%20Size-~1.05%20MB-success?style=flat-square)](https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=flat-square)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey?style=flat-square)](LICENSE)

<br/>

**JD Audio Extractor** is a high-performance, lightweight Android application created by **JD Mughal** to extract lossless audio tracks directly from video files without bulky FFmpeg binaries or battery-draining transcoding.

[📥 Download Latest APK](https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio/releases/latest) • [⚡ Features](#-key-features) • [🛠️ Architecture](#-technical-architecture) • [🚀 How to Build](#-building-from-source)

</div>

---

## 🌟 What is JD Audio Extractor?

**JD Audio Extractor** is an open-source mobile tool that demuxes audio tracks directly from video containers (MP4, MKV, WebM, MOV, 3GP, TS) into pure M4A/AAC audio. Unlike traditional converters that decode and re-encode audio samples (wasting time and reducing sound quality), **JD Audio Extractor** operates at the native hardware stream level for near-instant, 100% bit-for-bit lossless extraction.

---

## ⚡ Key Features

- **⚡ Near-Zero Delay Extraction**: Extracts full-length audio tracks in seconds using Android's native `MediaExtractor` and `MediaMuxer` hardware APIs.
- **🎧 100% Lossless Quality**: Preserves the original bit-exact audio compression without audio degradation or re-encoding artifacts.
- **🪶 Ultra Lightweight (~1.05 MB)**: Zero heavy third-party C++ `.so` libraries or FFmpeg binaries. Keeps your device storage clean.
- **🎨 Modern Royal Blue UI**: Beautiful, intuitive Material 3 dark interface with real-time extraction progress and interactive audio playback.
- **📱 Built-In Audio Previewer**: Scrub, play, and preview extracted tracks instantly before saving.
- **💾 Scoped Storage Compliant**: Directly save extracted songs to your device's Music library or share with messaging and social apps.
- **🔒 Privacy First**: 100% offline, zero telemetry, zero analytics, zero data collection.

---

## 📦 Supported Input & Output Formats

| Format Type | Supported Formats |
| :--- | :--- |
| **Input Videos** | MP4, MKV, WebM, MOV, 3GP, TS, AVI, FLV |
| **Audio Stream Codecs** | AAC (Advanced Audio Coding), Opus, Vorbis |
| **Output Container** | `.m4a` (Universal compatibility with Apple Music, VLC, Samsung Music, Spotify) |

---

## 🛠️ Technical Architecture

- **Language**: 100% Kotlin
- **User Interface**: Jetpack Compose & Material 3
- **Design Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Core Engine**: `android.media.MediaExtractor` (Stream Demuxer) & `android.media.MediaMuxer` (Direct Remuxer)
- **Minimum SDK**: Android 8.0 Oreo (API 26)
- **Target SDK**: Android 16 (API 36)

---

## 📥 Download JD Audio Extractor APK

You can download the pre-compiled, signed APK directly from GitHub:

👉 **[Download JD Audio Extractor APK (v1.0.0)](https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio/releases/latest)**

---

## 🚀 Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio.git
   cd JD-Audio-Extractor-Video-to-Audio
   ```

2. Build debug APK using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

3. Output path:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## ❓ Frequently Asked Questions (FAQ)

### What makes JD Audio Extractor faster than other converters?
Most video-to-audio apps rely on FFmpeg to decode audio into raw PCM samples and re-encode them into MP3. **JD Audio Extractor** bypasses transcoding completely by directly copying the compressed audio packets into an M4A audio container, finishing in seconds.

### Is JD Audio Extractor free to use?
Yes, **JD Audio Extractor** is 100% free and open-source under the Apache License 2.0.

---

## 👨‍💻 Developer & Community

- **Developer**: [JD Mughal](https://github.com/iam-jdmughal)
- **Organization**: JD Softwares / JD Corporation
- **Repository**: [JD Audio Extractor on GitHub](https://github.com/iam-jdmughal/JD-Audio-Extractor-Video-to-Audio)

If you find this project helpful, please consider **starring ⭐ the repository**!

---

## 📄 License

```
Copyright (c) 2026 JD Mughal / JD Softwares

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
