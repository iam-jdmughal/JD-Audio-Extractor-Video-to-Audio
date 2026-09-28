# 🎵 JD Video to Audio (Native Muxer) — Project Blueprint & Feasibility

## 1. 📌 Executive Summary & Feasibility Report

### Feasibility: 100% Native & Highly Feasible
Instead of embedding heavy third-party binaries like FFmpeg (which balloon APK sizes by 30 MB – 80 MB), this app leverages Android's built-in **`android.media.MediaExtractor`** and **`android.media.MediaMuxer`** APIs.

- **Direct Stream Extraction (Demux & Remux)**: Extracts the existing compressed audio track (AAC, etc.) from the video container and muxes it directly into an audio container (`.m4a` / `.mp4` audio).
- **Speed**: **Instantaneous (Near zero-delay)** because it does **not** decode and re-encode audio samples—it simply demuxes packets from the video container and writes them to the audio container.
- **Battery & CPU Efficiency**: Zero CPU/GPU transcoding overhead.
- **Battery & Resource Footprint**: Negligible memory usage (~15–30 MB RAM during muxing) using direct NIO `ByteBuffer` buffering.

---

## 2. 📦 Estimated APK Size

| Build Variant | Estimated Size | Notes |
| :--- | :--- | :--- |
| **Debug APK** | **~8 MB – 10 MB** | Unminified, includes debug tooling. |
| **Release APK (Optimized)** | **~2.2 MB – 3.2 MB** | R8 shrinking + Resource shrinking + Single language (`en`) + **Zero Native `.so` libraries**. |

> [!NOTE]
> Because we do **not** use FFmpeg or external C++ libraries, there are **0 native `.so` files** inside the APK. The entire app is 100% pure Kotlin running on Android's built-in platform codecs.

---

## 3. 🎥 Supported Formats & Codecs

Android's built-in `MediaExtractor` supports standard Android platform media containers out of the box:

### Supported Input Video Formats:
- **MP4** (`.mp4`, `.m4v`) — Most common
- **MKV** (`.mkv`) — High definition & movies
- **WebM** (`.webm`) — Online/YouTube downloads
- **3GP / 3G2** (`.3gp`, `.3g2`) — Mobile recordings
- **TS** (`.ts`, `.mts`) — Broadcast streams
- **MOV** (`.mov`) — Apple QuickTime / iPhone recordings

### Supported Output Audio Containers:
- **M4A (AAC)** (`.m4a`) — Universal standard for mobile & desktop, supported by 100% of modern media players and Android's native `MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4`.
- **WebM Audio (Opus / Vorbis)** (`.webm` / `.opus`) — Supported via `MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM`.
- **OGG** (`.ogg`) — Supported on Android 8.0+ (API 26+) via `MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG`.

---

## 4. 🧩 Minimal Dependencies List

We only include the **absolute minimum** dependencies required to build a sleek Jetpack Compose UI with asynchronous Kotlin Coroutines. No third-party media libraries are needed.

```kotlin
dependencies {
    // 1. Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)

    // 2. Compose UI & Material 3
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // 3. Kotlin Coroutines (for background processing without freezing UI)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 4. Android Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // 5. Core KTX
    implementation("androidx.core:core-ktx:1.12.0")
}
```

*Excluded (Not needed for this project)*:
- ❌ No FFmpeg / MobileFFmpeg (~40MB removed)
- ❌ No Gson / Jackson (Not needed for simple local operations)
- ❌ No Room / SQLite (Simple file-in file-out architecture)

---

## 5. ⚙️ Processing Architecture (How Built-in Muxing Works)

```
[ Input Video File ] (SAF Uri / FileDescriptor)
         │
         ▼
[ MediaExtractor ]
  ├─ Read track formats (find `mime.startsWith("audio/")`)
  ├─ Select audio track
  └─ Extract track format & CSd buffers (AAC header/metadata)
         │
         ▼
[ MediaMuxer ] (Output: .m4a)
  ├─ Add audio track to Muxer
  ├─ muxer.start()
  ├─ While extractor has samples:
  │    ├─ extractor.readSampleData(byteBuffer)
  │    ├─ muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
  │    └─ extractor.advance()
  └─ muxer.stop() & muxer.release()
         │
         ▼
[ Output Audio File Saved to Music/Audio in MediaStore ]
```

---

## 6. 📱 Minimalist UI & UX Layout

The interface follows a sleek, dark-themed, single-screen workflow designed for maximum speed and simplicity:

1. **Top App Bar**:
   - Clean minimalist branding (`Audio Extractor`), navigation drawer, and quick settings.
2. **Selected Video Card**:
   - Video preview thumbnail with duration overlay.
   - Clean metadata typography: File name, file size, and detected audio track specs badge (e.g., `AAC • 48 kHz • Lossless`).
3. **Format Selector Chips**:
   - Quick selectable pills: `M4A (Direct Mux)` *(Default, zero-copy)*, `WebM (Opus)`, etc.
4. **Primary Conversion Action**:
   - High-contrast glowing accent button: `Extract Audio Now`.
5. **Interactive Audio Result Card**:
   - Dynamic audio waveform visualization.
   - Inline playback controls (`Play / Pause`, duration progress).
   - Minimalist action icons for instant sharing and saving to device storage.

---

## 7. 🎨 Clean Minimalist Vector Icons Specification

All vector icons will be built natively inside `res/drawable/` adhering to the exact line-art format:
- **Viewport**: `24dp` × `24dp` (`viewportWidth="24"`, `viewportHeight="24"`)
- **Stroke Width**: `1.9dp` – `2.0dp` with `strokeLineCap="round"` and `strokeLineJoin="round"`
- **Style**: Ultra-clean, border-only minimalist outlines (no heavy solid fills), matching the design language of `JD Downloader`:

| Drawable Name | Purpose | Style |
| :--- | :--- | :--- |
| `ic_video_select.xml` | Video picker / drop card | Outline rounded video frame with clapper marks |
| `ic_music_wave.xml` | Audio track indicator & result | Outline waveform bars / double eighth-note |
| `ic_bolt_mux.xml` | Direct fast mux action | Clean minimalist lightning bolt / forward chevron |
| `ic_play_outline.xml` | Result audio player | Smooth rounded outline triangle |
| `ic_pause_outline.xml` | Result audio pause | Parallel rounded vertical pill bars |
| `ic_folder_save.xml` | Export to Music folder | Minimalist outline folder with inward arrow |
| `ic_share_outline.xml` | Direct audio sharing | Clean 3-node connected outline tree |

