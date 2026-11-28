package app.bilimorphe.patch.feature

import android.view.View
import android.view.ViewGroup
import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method

/**
 * 净化三项（1.23.3 的 `Block up recommend ads` / `Hide follow button` / `Remove vip section`）。
 *
 * 行为依据：
 * - `UpRcmdAdsPatch`：宿主 `getAdContent()` 在开关开时返回空值；
 * - `HideFollowButtonPatch`：`getShowFollowBtn(Z)Z`（评论）与 `getShowFollow(Z)Z`（动态）返回 false；
 * - `RemoveVipSectionPatch`：`HomeUserCenterFragment` 恢复时把 `mine_vip_layout` 的高度压到 0。
 *
 * 定位方式：前两项按**方法名**用 DexKit 全量搜索（这两个 getter 名在宿主中未混淆），
 * 不依赖具体类名；第三项用稳定类名 + 按资源名找 id。
 */
object PurifyFeature : Patch(
    name = "Purify",
    description = "Blocks up-recommend ads, follow buttons and the vip banner section.",
) {

    private const val USER_CENTER_FRAGMENT = "tv.danmaku.bili.ui.main2.mine.HomeUserCenterFragment"
    private const val VIP_LAYOUT_ID_NAME = "mine_vip_layout"

    /** 方法名 → 命中后要写入的返回值。 */
    private val FALSE_METHODS = listOf("getShowFollowBtn", "getShowFollow")
    private const val AD_METHOD = "getAdContent"

    override fun applyTo(engine: PatchEngine) {
        val blockAds = ModulePrefs.isEnabled(SettingsKeys.BLOCK_UP_RCMD_ADS)
        val hideFollow = ModulePrefs.isEnabled(SettingsKeys.BLOCK_FOLLOW_BUTTON)
        val removeVip = ModulePrefs.isEnabled(SettingsKeys.REMOVE_VIP_SECTION)
        if (!blockAds && !hideFollow && !removeVip) {
            Logger.i("[purify] off, skip")
            return
        }

        var hooked = 0

        // 1) 广告与关注按钮：按方法名定位
        val nameHooks = buildList {
            if (blockAds) add(AD_METHOD to null)
            if (hideFollow) FALSE_METHODS.forEach { add(it to false) }
        }
        if (nameHooks.isNotEmpty()) {
            runCatching {
                val bridge = DexKitBridge.create(engine.hostApkPath)
                try {
                    for ((methodName, forced) in nameHooks) {
                        val hits = bridge.findMethod { matcher { name = methodName } }
                        var local = 0
                        for (data in hits) {
                            val method: Method = runCatching {
                                data.getMethodInstance(engine.classLoader)
                            }.getOrNull() ?: continue
                            runCatching {
                                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                                        param.result = forced
                                    }
                                })
                                hooked++
                                local++
                            }.onFailure { Logger.e("[purify] hook $methodName failed", it) }
                        }
                        Logger.i("[purify] $methodName hooked=$local")
                    }
                } finally {
                    runCatching { bridge.close() }
                }
            }.onFailure { Logger.e("[purify] dexkit locate failed", it) }
        }

        // 2) 我的页会员横图：恢复时压平
        if (removeVip) {
            runCatching {
                val cls = engine.findClass(USER_CENTER_FRAGMENT)
                if (cls == null) {
                    Logger.i("[purify] user center fragment missing")
                    return@runCatching
                }
                val onResume = cls.declaredMethods.firstOrNull {
                    it.name == "onResume" && it.parameterCount == 0
                }
                if (onResume == null) {
                    Logger.i("[purify] onResume missing")
                    return@runCatching
                }
                XposedBridge.hookMethod(onResume, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        runCatching { collapseVipSection(param.thisObject) }
                            .onFailure { Logger.e("[purify] vip collapse failed", it) }
                    }
                })
                hooked++
                Logger.i("[purify] vip section hook armed")
            }.onFailure { Logger.e("[purify] vip hook failed", it) }
        }

        Logger.i("[purify] hooked=$hooked ads=$blockAds follow=$hideFollow vip=$removeVip")
    }

    /** 把 `mine_vip_layout` 的子视图隐藏并把高度压成 0。 */
    private fun collapseVipSection(fragment: Any) {
        val view = fragment.javaClass.getMethod("getView").invoke(fragment) as? ViewGroup ?: return
        val context = view.context
        val id = context.resources.getIdentifier(VIP_LAYOUT_ID_NAME, "id", context.packageName)
        if (id == 0) {
            Logger.i("[purify] $VIP_LAYOUT_ID_NAME not found")
            return
        }
        val vip = view.findViewById<ViewGroup>(id) ?: return
        for (i in 0 until vip.childCount) {
            vip.getChildAt(i).visibility = View.GONE
        }
        vip.layoutParams = vip.layoutParams.apply { height = 0 }
    }
}
