# 🤖 Android Gradle & Dependency Blueprint for AI Agents

When initializing or generating a new Android application on this machine, AI agents **MUST** use the exact Gradle wrapper version, Android Gradle Plugin (AGP), Kotlin versions, `gradle.properties` flags, and dependency versions specified below. 

Using these exact versions ensures that all dependencies are already pre-cached in the local Gradle store, avoiding build failures, syntax mismatches, duplicate extension errors in AGP 9.0+, or network download delays.

---

## 1. ⚙️ Root `gradle.properties`

Always create `gradle.properties` at the root of the project with the following contents:

```properties
# Project-wide Gradle settings
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=true
kotlin.code.style=official

# Memory & Build settings
org.gradle.daemon=false
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8

# Project Identity
VERSION_NAME=1.0.0

# Disable AGP 9.0+ built-in Kotlin to avoid duplicate 'kotlin' extension conflict
android.builtInKotlin=false

# Revert to legacy DSL implementation to fix Kotlin Gradle Plugin BaseExtension cast exception
android.newDsl=false
```

---

## 2. 📦 Gradle Wrapper Configuration (`gradle/wrapper/gradle-wrapper.properties`)

Create `gradle/wrapper/gradle-wrapper.properties` with:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-all.zip
```

---

## 3. 🌐 Root `settings.gradle.kts`

Create `settings.gradle.kts` at the root:

```kotlin
import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "your-app-name"
include(":app")
```

---

## 4. 🛠️ Root `build.gradle.kts`

Create `build.gradle.kts` at the root:

```kotlin
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
```

---

## 5. 📱 App Module `app/build.gradle.kts`

Create `app/build.gradle.kts` with the following configuration and pre-cached dependencies:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.yourapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.yourapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Strip unused localized strings from Compose & Material dependencies
        resourceConfigurations += listOf("en")

        // Prevent bundling unnecessary native architectures (reduces size by up to 60-70%)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules-release.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules-debug.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.properties",
                "DebugProbesKt.bin"
            )
        }
    }
}

dependencies {
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Pre-tested System Dependencies
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Core Utilities & Libraries
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Debugging & Testing
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

---

## 6. 🗜️ Mandatory APK Size Minimization Rules

To ensure generated APKs maintain the **minimum possible file size** without sacrificing functionality, AI agents **MUST** strictly adhere to the following compression rules:

### 1. Locale & String Stripping (`resourceConfigurations`)
Jetpack Compose, AndroidX, and Material libraries bundle strings for 80+ localized languages by default.
- Always include `resourceConfigurations += listOf("en")` in `defaultConfig`.
- Stripping unused translations saves **2 to 5 MB** of unnecessary string resource tables.

### 2. Native ABI Architecture Filtering (`ndk.abiFilters`)
If native media/audio/video libraries (e.g., FFmpeg, Media3, sound engines) are bundled, including all native ABIs (`x86`, `x86_64`, `armeabi-v7a`, `arm64-v8a`) inflates APK size by 3x–4x.
- Always filter ABIs to target architectures in `defaultConfig`:
  ```kotlin
  ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a")
  }
  ```
- For modern-device-only builds, restrict strictly to `listOf("arm64-v8a")`.

### 3. Aggressive R8 Optimization & Resource Shrinking
- Ensure `isMinifyEnabled = true` and `isShrinkResources = true` in the `release` build type.
- Always use `getDefaultProguardFile("proguard-android-optimize.txt")` (the optimized configuration) rather than `proguard-android.txt`.

### 4. Packaging Overhead Elimination
Prevent packaging debug symbols and metadata files into the APK archive:
```kotlin
packaging {
    resources {
        excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/*.version",
            "/META-INF/*.properties",
            "DebugProbesKt.bin"
        )
    }
}
```