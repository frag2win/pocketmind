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

## Status
**Resolved.** The project now builds successfully using `:app:compileDebugKotlin`.
