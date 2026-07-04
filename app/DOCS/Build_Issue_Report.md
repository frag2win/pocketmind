# Build Issue Report - July 01, 2026

This report documents build errors encountered during project setup and the steps taken to resolve them.

## Issue 1: Missing `jlink` Executable

### Error Description
```
Execution failed for JdkImageTransform: ...\core-for-system-modules.jar.
> jlink executable C:\Users\sunanda.AMFIIND\.antigravity-ide\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\jlink.exe does not exist.
```

### Root Cause
The build system was attempting to use a JRE (Java Runtime Environment) provided by an IDE extension instead of a full JDK. `jlink` is a tool exclusive to the JDK, and since it was missing in the JRE, the Android Gradle Plugin (AGP) could not generate the required JDK image for Android 36.

### Resolution
1.  **Configured Java Toolchain:** Modified `app/build.gradle.kts` to explicitly use the Java 17 toolchain.
    ```kotlin
    kotlin {
        jvmToolchain(17)
    }
    ```
2.  **Compatibility Update:** Set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17` to align with the toolchain.

---

## Issue 2: KSP Internal Error (Room Compatibility)

### Error Description
```
[ksp] java.lang.IllegalStateException: unexpected jvm signature V
Execution failed for task ':app:kspDebugKotlin'.
> A failure occurred while executing com.google.devtools.ksp.gradle.KspAAWorkerAction
   > unexpected jvm signature V
```

### Root Cause
This is a known compatibility issue between Room 2.6.x and Kotlin 2.x when using KSP (Kotlin Symbol Processing). The "V" signature represents `void`, which older versions of the Room compiler fail to parse correctly for Kotlin `suspend` functions returning `Unit`.

### Resolution
1.  **Upgraded Room:** Updated the Room library version from `2.6.1` to `2.8.4` in `gradle/libs.versions.toml`.
    ```toml
    room = "2.8.4"
    ```
    Version 2.7.0+ contains the necessary fixes for KSP2/Kotlin 2.x compatibility.

---

## Issue 3: Invalid Composable Call in MainActivity

### Error Description
`MainActivity.kt` failed to compile because `ChatScreen()` was called without providing its required parameters (messages, isGenerating, etc.).

### Resolution
Updated `MainActivity.kt` to use `ChatScreenRoot()`, which uses Hilt to inject the `ChatViewModel` and handles the state collection automatically.

---

## Issue 4: Unresolved reference 'androidx' in MainActivity

### Error Description
```
e: file:///.../MainActivity.kt:28:30 Unresolved reference 'androidx'.
```

### Root Cause
Incorrect syntax in `MainActivity.kt` where extension functions (`consumeWindowInsets`, `imePadding`) were prefixed with their fully qualified package names inside a `Modifier` chain (e.g., `.androidx.compose.foundation.layout.imePadding()`). Kotlin expects extension functions to be called directly on the receiver object, with the package imported at the top of the file.

### Resolution
1.  **Refactored Modifier Chain:** Removed the package prefixes from the extension function calls.
2.  **Updated Imports:** Added explicit imports for `androidx.compose.foundation.layout.consumeWindowInsets` and `androidx.compose.foundation.layout.imePadding`.
3.  **Resolved Symbol:** Also updated the `ChatScreenRoot` call to remove its fully qualified prefix and added its import.

---

## Issue 5: Untested and Unsafe Implementations (Pre-Hackathon)

### Warning: Experimental Features
The following features have been implemented based on the PRD but are currently **untested** on physical hardware or **unsafe** for production use without further refinement:

1. **LiteRT-LM Initialization (Cold Start)**:
   - **Risk**: The `LiteRTInferenceEngine` is not yet called by any UI component to perform the initial `initialize(modelPath)` call. Attempting to send a message will result in an "Engine not initialized" error.
   - **Status**: Logic is correct for the 2026 SDK, but needs a "Model Loading" UI flow.

2. **Model Storage & Pathing**:
   - **Risk**: The current implementation assumes a valid `.litertlm` file path exists on the device. Since Play Asset Delivery is not yet set up, the app will fail to load a model unless manually placed in internal storage.
   - **Status**: Theoretical implementation.

3. **PDF File Permissions (Android 11+)**:
   - **Risk**: Saving to `getExternalFilesDir` works without permissions, but if we move to `Environment.getExternalStoragePublicDirectory`, we will need to handle `MANAGE_EXTERNAL_STORAGE` or MediaStore APIs for Android 13+.
   - **Status**: Safe for internal app storage, untested for user-facing file managers.

4. **NPU Backend Stability**:
   - **Risk**: Forcing `Backend.NPU()` on all MediaTek/Qualcomm chips might cause crashes on older drivers. A fallback mechanism is in place, but has not been stress-tested.
   - **Status**: Experimental.

---

## Issue 6: Duplicate Native Library Conflict (`libLiteRt.so`)

### Error Description
```
2 files found with path 'lib/arm64-v8a/libLiteRt.so' from inputs:
 - .../litert-2.1.5/jni/arm64-v8a/libLiteRt.so
 - .../litertlm-android-0.13.1/jni/arm64-v8a/libLiteRt.so
```

### Root Cause
Both the core `litert` runtime and the `litertlm-android` SDK bundle the same shared native libraries. When the Android Gradle Plugin (AGP) attempts to merge the native libs into the APK, it encounters a collision.

### Resolution
Updated `app/build.gradle.kts` to include a `packaging` block that specifies `pickFirsts` for LiteRT-related native libraries.
```kotlin
packaging {
    jniLibs {
        pickFirsts += "**/libLiteRt*.so"
    }
}
```

---

## Issue 7: Kotlin Metadata Incompatibility (2.4.0 vs 2.2.0)

### Error Description
```
Class 'kotlin.Suppress' was compiled with an incompatible version of Kotlin. The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.
```

### Root Cause
A newer dependency (Material 3 Markdown renderer) pulled in `kotlin-stdlib:2.4.0`. However, the project was using Kotlin Gradle Plugin `2.1.x`, which uses a `2.2.0` compiler that is incompatible with the newer metadata in the `2.4.0` stdlib. This caused KSP to fail during Room database code generation.

### Resolution
1.  **Aligned Kotlin/KSP Versions:** Updated `libs.versions.toml` to use Kotlin `2.1.0` and KSP `2.1.0-1.0.29`.
2.  **Enforced Dependency Versions:** Added a `resolutionStrategy` in root `build.gradle.kts` to force `kotlin-stdlib` to `2.1.0` across all configurations.
    ```kotlin
    allprojects {
        configurations.all {
            resolutionStrategy {
                force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0")
                force("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
            }
        }
    }
    ```
3.  **Updated SDK:** Bumped `compileSdk` and `targetSdk` to `37` as required by the newer libraries.

---

## Status
**Resolved.** The project now builds successfully using `:app:assembleDebug`.
