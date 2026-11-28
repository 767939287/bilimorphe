package app.bilimorphe.patch.feature

import android.app.Application
import android.content.Context
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File

/**
 * 禁止热更新（对应 1.28.6 的 `disable_hot_patch`）。
 *
 * 行为依据：1.28.6 在开关打开时清空宿主的热更新落地目录
 * `context.getDir("tribe", 0)`（见 `docs/key-diff-1.23-vs-1.28.md` 备注）。
 * 本项目按行为重写：宿主 Application.onCreate 之后清空该目录，并输出删除统计，
 * 便于实机核对；开关关闭时不安装任何 hook。
 *
 * 限制：开关只在进程启动时生效（勾选后需重启宿主）。
 */
object DisableHotPatchFeature : Patch(
    name = "Disable hot patch",
    description = "Purges the host hot-patch directory so downloaded patches never take effect.",
) {

    /** 宿主热更新目录名。 */
    private const val HOT_PATCH_DIR = "tribe"

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.DISABLE_HOT_PATCH)) {
            Logger.i("[disable_hot_patch] off, skip")
            return
        }

        val appClass = engine.classLoader.loadClass(engine.hostClassName)
        val onCreate = appClass.getMethod("onCreate")
        XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val app = param.thisObject as Application
                purge(app)
            }
        })
        Logger.i("[disable_hot_patch] armed")
    }

    private fun purge(app: Application) {
        val dir: File = runCatching { app.getDir(HOT_PATCH_DIR, Context.MODE_PRIVATE) }
            .getOrElse {
                Logger.e("[disable_hot_patch] dir lookup failed", it)
                return
            }

        if (!dir.exists()) {
            Logger.i("[disable_hot_patch] dir absent: ${dir.absolutePath}")
            return
        }

        var deleted = 0
        var failed = 0
        dir.listFiles()?.forEach { child ->
            if (runCatching { child.deleteRecursively() }.getOrDefault(false)) {
                deleted++
            } else {
                failed++
            }
        }
        Logger.i("[disable_hot_patch] purged deleted=$deleted failed=$failed at ${dir.absolutePath}")
    }
}
