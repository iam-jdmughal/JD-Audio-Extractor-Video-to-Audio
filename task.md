# Tasks: JD Video to Audio (Native Muxer)

- [x] 1. Project Scaffolding & Configuration <!-- id: 1 -->
  - [x] 1.1 Copy Gradle wrapper and set up `local.properties` <!-- id: 1.1 -->
  - [x] 1.2 Create root `gradle.properties`, `settings.gradle.kts`, `build.gradle.kts` <!-- id: 1.2 -->
  - [x] 1.3 Create `app/build.gradle.kts` with pre-cached Compose & optimization flags <!-- id: 1.3 -->
  - [x] 1.4 Create `AndroidManifest.xml` with permissions & FileProvider <!-- id: 1.4 -->
- [x] 2. Minimalist Vector Drawables (res/drawable) <!-- id: 2 -->
  - [x] 2.1 Create clean 24dp outline icons (`ic_video_file`, `ic_audio_wave`, `ic_bolt_mux`, `ic_play_outline`, `ic_pause_outline`, `ic_share_outline`, `ic_folder_save`, `ic_check_circle`) <!-- id: 2.1 -->
- [x] 3. Native Extraction Engine & Storage <!-- id: 3 -->
  - [x] 3.1 Implement `AudioExtractorEngine.kt` (MediaExtractor + MediaMuxer direct demuxing) <!-- id: 3.1 -->
  - [x] 3.2 Implement `MediaStoreSaver.kt` (Scoped Storage save & share) <!-- id: 3.2 -->
- [x] 4. Clean Minimalist UI & ViewModel <!-- id: 4 -->
  - [x] 4.1 Create Material 3 Color & Typography Theme (clean, no neon) <!-- id: 4.1 -->
  - [x] 4.2 Create `AudioExtractorViewModel.kt` with state management & audio player <!-- id: 4.2 -->
  - [x] 4.3 Create `MainScreen.kt` (picker card, audio specs, format chips, extract button, result player) <!-- id: 4.3 -->
  - [x] 4.4 Create `MainActivity.kt` <!-- id: 4.4 -->
- [x] 5. Build & Verification <!-- id: 5 -->
  - [x] 5.1 Project ready for user manual build (`./gradlew assembleDebug`) <!-- id: 5.1 -->

