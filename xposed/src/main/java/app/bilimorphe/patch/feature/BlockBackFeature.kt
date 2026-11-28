package app.bilimorphe.patch.feature

import android.app.Activity
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method

/**
 * 拦截返回（对应 1.28.6 的 `block_back`）。
 *
 * 行为依据：1.28.6 在下列三个页面里，开关打开时直接 `return`，不再执行原返回逻辑：
 * - `com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3`（直播间）
 * - `com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity`（详情页）
 * - `com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity`（列表页）
 *
 * 本项目重写为运行时 hook：拦截 `onBackPressed()`，开关打开时不执行原实现。
 * 三个类名在宿主中均未混淆，跨版本可作为稳定锚点；找不到的类只记录日志，不影响其他项。
 */
object BlockBackFeature : Patch(
    name = "Block back",
    description = "Prevents the back key from leaving live room / detail / playlist screens.",
) {

    private val TARGETS = listOf(
        "com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3",
        "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
        "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity",
    )

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.BLOCK_BACK)) {
            Logger.i("[block_back] off, skip")
            return
        }

        var hooked = 0
        var missing = 0
        for (className in TARGETS) {
            val target = locate(engine, className)
            if (target == null) {
                missing++
                Logger.i("[block_back] not found: $className")
                continue
            }
            runCatching {
                XposedBridge.hookMethod(target, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // 不执行原实现，页面因此不会退出
                        param.result = null
                    }
                })
                hooked++
                Logger.i("[block_back] hooked ${className}#${target.name}")
            }.onFailure { Logger.e("[block_back] hook failed: $className", it) }
        }
        Logger.i("[block_back] hooked=$hooked missing=$missing")
    }

    /** 找到无参 `onBackPressed`，优先声明方法，其次整个继承链。 */
    private fun locate(engine: PatchEngine, className: String): Method? {
        val cls = engine.findClass(className) ?: return null
        if (!Activity::class.java.isAssignableFrom(cls)) {
            Logger.i("[block_back] not an Activity: $className")
        }
        cls.declaredMethods
            .firstOrNull { it.name == "onBackPressed" && it.parameterCount == 0 }
            ?.let { it.isAccessible = true; return it }
        var cur: Class<*>? = cls
        while (cur != null && cur != Any::class.java) {
            cur.declaredMethods
                .firstOrNull { it.name == "onBackPressed" && it.parameterCount == 0 }
                ?.let { it.isAccessible = true; return it }
            cur = cur.superclass
        }
        return null
    }
}
