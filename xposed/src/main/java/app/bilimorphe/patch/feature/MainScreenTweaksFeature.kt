package app.bilimorphe.patch.feature

import android.app.Activity
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 主界面两项（对应 1.28.6 的 `not_suicide` / `skip_reward_ad`）。
 *
 * 行为依据：
 * - `not_suicide`（`kofua/FXjb.java` + `tv/danmaku/bili/MainActivityV2.onBackPressed`）：
 *   主界面按返回时不退出应用，改为 `moveTaskToBack(true)` 退到后台；
 * - `skip_reward_ad`（`kofua/pIj.java`）：检测到 `RewardAdDisplayActivity` 启动即关闭，
 *   等价于宿主侧调用其关闭流程。
 *
 * 两个宿主类名均未混淆，跨版本可作为稳定锚点。
 */
object MainScreenTweaksFeature : Patch(
    name = "Main screen tweaks",
    description = "Back key sends the app to background instead of exiting; reward ad activity is dismissed.",
) {

    private const val MAIN_ACTIVITY = "tv.danmaku.bili.MainActivityV2"
    private const val REWARD_AD_ACTIVITY = "com.bilibili.ad.reward.activity.RewardAdDisplayActivity"

    override fun applyTo(engine: PatchEngine) {
        val notSuicide = ModulePrefs.isEnabled(SettingsKeys.NOT_SUICIDE)
        val skipRewardAd = ModulePrefs.isEnabled(SettingsKeys.SKIP_REWARD_AD)
        if (!notSuicide && !skipRewardAd) {
            Logger.i("[main_tweaks] off, skip")
            return
        }

        if (notSuicide) {
            val cls = engine.findClass(MAIN_ACTIVITY)
            val method = cls?.declaredMethods?.firstOrNull {
                it.name == "onBackPressed" && it.parameterCount == 0
            }
            val hookTarget = method ?: cls?.let { runCatching { it.getMethod("onBackPressed") }.getOrNull() }
            if (hookTarget == null) {
                Logger.i("[main_tweaks] not_suicide: onBackPressed not found")
            } else {
                runCatching {
                    XposedBridge.hookMethod(hookTarget, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val activity = param.thisObject as? Activity ?: return
                            runCatching { activity.moveTaskToBack(true) }
                            // 不执行原返回逻辑，避免退出应用
                            param.result = null
                        }
                    })
                    Logger.i("[main_tweaks] not_suicide armed")
                }.onFailure { Logger.e("[main_tweaks] not_suicide hook failed", it) }
            }
        }

        if (skipRewardAd) {
            val cls = engine.findClass(REWARD_AD_ACTIVITY)
            val onCreate = cls?.let { runCatching { it.getMethod("onCreate", android.os.Bundle::class.java) }.getOrNull() }
            if (onCreate == null) {
                Logger.i("[main_tweaks] skip_reward_ad: activity not found")
            } else {
                runCatching {
                    XposedBridge.hookMethod(onCreate, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            (param.thisObject as? Activity)?.let { activity ->
                                runCatching { activity.finish() }
                                Logger.i("[main_tweaks] reward ad dismissed")
                            }
                        }
                    })
                    Logger.i("[main_tweaks] skip_reward_ad armed")
                }.onFailure { Logger.e("[main_tweaks] skip_reward_ad hook failed", it) }
            }
        }
    }
}
