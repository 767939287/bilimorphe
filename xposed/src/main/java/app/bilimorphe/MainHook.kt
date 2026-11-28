package app.bilimorphe

import app.bilimorphe.patch.Patches
import app.bilimorphe.util.Logger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed entry point, registered through META-INF/xposed/java_init.list.
 */
class MainHook : XposedModule() {

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val patches = Patches.forPackage(param.packageName) ?: return
        if (patches.isEmpty()) return

        try {
            val engine = PatchEngine(param)
            // 不再只处理首进程：B 站有 158 处 android:process，
            // 下载/播放等能力只在子进程里才有意义。
            // 每个补丁用 ProcessScope 自己声明适用范围。
            Logger.i("process ready: ${engine.processName} main=${engine.isMainProcess}")
            for (patch in patches) {
                if (!patch.scope.allows(engine)) {
                    Logger.i("patch skipped by scope: ${patch.name}")
                    continue
                }
                runCatching { patch.applyTo(engine) }
                    .onSuccess { Logger.i("patch applied: ${patch.name}") }
                    .onFailure { Logger.e("patch failed: ${patch.name}", it) }
            }
        } catch (t: Throwable) {
            Logger.e("patch engine failed", t)
        }
    }
}
