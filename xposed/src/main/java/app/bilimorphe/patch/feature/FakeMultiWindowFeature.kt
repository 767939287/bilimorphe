package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

/**
 * 播放页伪装未处于多窗口模式（1.23.3 的 `Fake not in multi window mode`）。
 *
 * 行为依据（`FakeNotInMultiWindowPatch`）：1.23.3 是给 6 个 Activity **新增**一个
 * `isInMultiWindowMode()`，内部先问 `SettingsTransfer.fakeNotInMultiWindow()`，
 * 为 true 时直接返回 false，否则调 super。
 *
 * 原版宿主这些 Activity 并未声明该方法（实测 9.21.0 全部为 0），所以运行时改为
 * hook 系统 `android.app.Activity.isInMultiWindowMode()`，用类名白名单限定范围：
 * 只有这 6 个页面、且开关打开时，才返回 false。
 *
 * 开关键沿用 1.23.3：`fake_not_in_multi_window`。
 */
object FakeMultiWindowFeature : Patch(
    name = "Fake not in multi window",
    description = "Reports the player pages as not being in multi-window mode.",
) {

    private val ACTIVITIES = setOf(
        "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity",
        "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
        "com.bilibili.bangumi.ui.page.detail.BangumiDetailActivityV3",
        "com.bilibili.video.videodetail.VideoDetailsActivity",
        "com.bilibili.multitypeplayerV2.MultiTypeVideoContentActivity",
        "com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3",
    )

    override fun applyTo(engine: PatchEngine) {
        if (!ModulePrefs.isEnabled(SettingsKeys.FAKE_NOT_IN_MULTI_WINDOW)) {
            Logger.i("[fake_multi_window] off, skip")
            return
        }

        val activityClass = runCatching { Class.forName("android.app.Activity") }.getOrNull()
        val method = activityClass?.declaredMethods?.firstOrNull {
            it.name == "isInMultiWindowMode" && it.parameterCount == 0
        }
        if (method == null) {
            Logger.i("[fake_multi_window] Activity.isInMultiWindowMode not found")
            return
        }

        runCatching {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val name = param.thisObject?.javaClass?.name ?: return
                    if (name in ACTIVITIES) {
                        param.result = false
                    }
                }
            })
            Logger.i("[fake_multi_window] hooked Activity.isInMultiWindowMode for ${ACTIVITIES.size} pages")
        }.onFailure { Logger.e("[fake_multi_window] hook failed", it) }
    }
}
