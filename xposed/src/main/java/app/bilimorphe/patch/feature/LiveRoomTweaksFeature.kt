package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.luckypray.dexkit.DexKitBridge

/**
 * 直播间防误触三项（1.23.3 的 `Forbid live room auto float` / `Forbid live room slide left` /
 * `Disable live room double click`）。
 *
 * 1.23.3 的实现方法（`LiveRoomPatch` 的静态方法）在这份开源备份里缺失，
 * 因此按行为语义重写：
 * - `forbid_live_room_auto_float`：宿主设置小窗开关的方法（带 `live_float_window_is_open` 特征串）
 *   强制传入 false，从而不自动进小窗；
 * - `forbid_live_room_slide_left`：宿主 `LiveRoomTouchDispatchViewModel_slideRightToLeft`
 *   所在方法直接不执行，左滑不再切房间；
 * - `disable_live_room_double_click`：直播间手势识别器的 `onDoubleTap` 返回 false（不消费双击）。
 *
 * 三项都用 DexKit 字符串/方法名定位，不绑类名；1.23.3 的 `forbid_switch_live_room` 无可用特征，暂未实现。
 */
object LiveRoomTweaksFeature : Patch(
    name = "Live room tweaks",
    description = "Prevents auto float window, left-swipe room switch and double tap in live rooms.",
) {

    /** 小窗开关方法的特征串（1.23.3 LiveRoomSetFloatWindowFingerprint 同款）。 */
    private val AUTO_FLOAT_HINTS = listOf(
        "live_float_window_is_open",
        "bundle_key_player_params_controller_enable_live_window_play",
    )

    /** 左滑切房间的特征串。 */
    private const val SLIDE_LEFT_HINT = "LiveRoomTouchDispatchViewModel_slideRightToLeft"

    /** 手势双击方法名。 */
    private const val DOUBLE_TAP_METHOD = "onDoubleTap"

    /** 双击只处理直播相关类，避免影响其他地方。 */
    private const val LIVE_PACKAGE_MARK = "bililive"

    /** 直播竖屏容器：控制能否滑动切换房间。 */
    private const val VERTICAL_PAGER =
        "com.bilibili.bililive.room.ui.roomv3.vertical.widget.LiveVerticalPagerView"

    /** 1.23.3 是拦 onInterceptTouchEvent；9.21.0 无该控件结构，改用这两个 setter。 */
    private val SWITCH_GUARDS = listOf("setUserInputEnabled", "setRoomCanScroll")

    override fun applyTo(engine: PatchEngine) {
        val autoFloat = ModulePrefs.isEnabled(SettingsKeys.FORBID_LIVE_ROOM_AUTO_FLOAT)
        val slideLeft = ModulePrefs.isEnabled(SettingsKeys.FORBID_LIVE_ROOM_SLIDE_LEFT)
        val doubleClick = ModulePrefs.isEnabled(SettingsKeys.DISABLE_LIVE_ROOM_DOUBLE_CLICK)
        val forbidSwitch = ModulePrefs.isEnabled(SettingsKeys.FORBID_SWITCH_LIVE_ROOM)
        if (!autoFloat && !slideLeft && !doubleClick && !forbidSwitch) {
            Logger.i("[live_tweaks] off, skip")
            return
        }

        // 禁止切房：优先钩竖屏容器的两个控制开关（不需要 DexKit）
        if (forbidSwitch) {
            val cls = engine.findClass(VERTICAL_PAGER)
            if (cls == null) {
                Logger.i("[live_tweaks] forbid_switch: pager missing")
            } else {
                var local = 0
                for (method in cls.declaredMethods) {
                    if (method.name !in SWITCH_GUARDS || method.parameterCount != 1) continue
                    runCatching {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.args[0] = false
                            }
                        })
                        local++
                    }.onFailure { Logger.e("[live_tweaks] forbid_switch hook failed", it) }
                }
                Logger.i("[live_tweaks] forbid_switch hooked=$local")
            }
        }

        val bridge = DexKitBridge.create(engine.hostApkPath)
        try {
            var hooked = 0

            if (autoFloat) {
                hooked += locateByStrings(bridge, engine, AUTO_FLOAT_HINTS, "auto_float") { method ->
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (param.args.isNotEmpty()) param.args[0] = false
                        }
                    })
                }
            }

            if (slideLeft) {
                hooked += locateByStrings(bridge, engine, listOf(SLIDE_LEFT_HINT), "slide_left") { method ->
                    val returnsVoid = method.returnType == java.lang.Void.TYPE
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (returnsVoid) param.result = null
                        }
                    })
                }
            }

            if (doubleClick) {
                val hits = bridge.findMethod {
                    matcher {
                        name = DOUBLE_TAP_METHOD
                        returnType = "boolean"
                    }
                }
                var local = 0
                for (data in hits) {
                    val className = data.className ?: continue
                    if (!className.contains(LIVE_PACKAGE_MARK)) continue
                    val method = runCatching { data.getMethodInstance(engine.classLoader) }.getOrNull()
                        ?: continue
                    runCatching {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = false
                            }
                        })
                        local++
                    }.onFailure { Logger.e("[live_tweaks] double tap hook failed", it) }
                }
                Logger.i("[live_tweaks] double_tap hooked=$local")
                hooked += local
            }

            Logger.i("[live_tweaks] hooked=$hooked autoFloat=$autoFloat slideLeft=$slideLeft doubleTap=$doubleClick")
        } finally {
            runCatching { bridge.close() }
        }
    }

    /** 用字符串特征定位方法并执行 hook，返回成功数量。 */
    private fun locateByStrings(
        bridge: DexKitBridge,
        engine: PatchEngine,
        strings: List<String>,
        tag: String,
        installer: (java.lang.reflect.Method) -> Unit,
    ): Int {
        var local = 0
        runCatching {
            val hits = bridge.findMethod {
                matcher { usingStrings(*strings.toTypedArray()) }
            }
            for (data in hits) {
                val method = runCatching { data.getMethodInstance(engine.classLoader) }.getOrNull()
                    ?: continue
                runCatching {
                    installer(method)
                    local++
                    Logger.i("[live_tweaks] $tag hooked ${data.className}#${data.methodName}")
                }.onFailure { Logger.e("[live_tweaks] $tag hook failed", it) }
            }
        }.onFailure { Logger.e("[live_tweaks] $tag locate failed", it) }
        if (local == 0) Logger.i("[live_tweaks] $tag no target matched")
        return local
    }
}
