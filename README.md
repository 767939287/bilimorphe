# BiliMorphe

Runtime LSPosed module for the bilibili Android client (`tv.danmaku.bili`).

Instead of repacking and re-signing the APK, this module resolves target classes
and methods inside the running host process with
[DexKit](https://github.com/LuckyPray/DexKit) and applies Xposed hooks at runtime.
The module is registered through the `libxposed` API (`META-INF/xposed/*`), so it
works with current LSPosed releases without any patched APK.

## Status

Early scaffold. This build ships a single verification patch that logs the host
package and version at process startup. Feature patches will be added on top of
the same engine.

## Requirements

- Rooted device with LSPosed (libxposed API 101 or newer)
- bilibili Android client 9.x

## Build

Builds run on GitHub Actions:

- Gradle 9.8.1 (wrapper)
- Android Gradle Plugin 9.4.1
- JDK 21
- compileSdk 36, minSdk 27

Artifacts are uploaded on every push and attached to releases tagged `v*`.

## Project layout

```
app/                                   LSPosed module
  src/main/java/app/bilimorphe/
    MainHook.kt                        libxposed entry point
    PatchEngine.kt                     host class loading and lookup helpers
    patch/                             patch registry and patches
  src/main/resources/META-INF/xposed/  module metadata, scope, entry class
```

## License

GPL-3.0. See `LICENSE` and `NOTICE`.
