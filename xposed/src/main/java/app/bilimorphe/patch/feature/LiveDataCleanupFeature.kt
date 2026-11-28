package app.bilimorphe.patch.feature

import app.bilimorphe.PatchEngine
import app.bilimorphe.patch.Patch
import app.bilimorphe.settings.ModulePrefs
import app.bilimorphe.settings.SettingsKeys
import app.bilimorphe.util.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Type

/**
 * 直播/推荐数据净化三项（对应 1.28.6 的 `live_no_block` / `hide_live_banner` / `block_activity_label`）。
 *
 * 行为依据（`kofua/IhH.java`、`kofua/ncV.java`）：
 * - `live_no_block`：直播间信息里 `blockInfo = null`；同时把 `switchInfo["room-player-watermark"]` 置 0；
 * - `hide_live_banner`：直播首页 `BiliLiveHomeFeedPage.pageList` 移除 banner 项；
 * - `block_activity_label`：推荐卡片 `headLine.clearLabel()`（清掉活动角标）。
 *
 * 1.28.6 是在 fastjson 反序列化出口（`JavaBeanDeserializer.deserialze` / `JSON.parseArray`）套包装，
 * 本项目重写为直接 hook 这两个出口，按返回对象类型分发处理。
 */
object LiveDataCleanupFeature : Patch(
    name = "Live data cleanup",
    description = "Strips live-room block info, live home banners and recommendation activity labels.",
) {

    private const val JSON = "com.alibaba.fastjson.JSON"
    private const val BEAN_DESERIALIZER = "com.alibaba.fastjson.parser.JavaBeanDeserializer"

    private const val ROOM_INFO = "BiliLiveRoomInfo"
    private const val HOME_FEED_PAGE = "BiliLiveHomeFeedPage"

    /** 直播首页 banner 卡片的类型标记（1.28.6 过滤条件）。 */
    private const val BANNER_CARD_TYPE = "banner_v2"

    override fun applyTo(engine: PatchEngine) {
        val liveNoBlock = ModulePrefs.isEnabled(SettingsKeys.LIVE_NO_BLOCK)
        val hideBanner = ModulePrefs.isEnabled(SettingsKeys.HIDE_LIVE_BANNER)
        val blockLabel = ModulePrefs.isEnabled(SettingsKeys.BLOCK_ACTIVITY_LABEL)
        if (!liveNoBlock && !hideBanner && !blockLabel) {
            Logger.i("[live_cleanup] off, skip")
            return
        }

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val result = param.result ?: return
                runCatching {
                    if (liveNoBlock) cleanRoomInfo(result)
                    if (hideBanner) cleanHomeFeed(result)
                    if (blockLabel) cleanActivityLabel(result)
                }.onFailure { Logger.e("[live_cleanup] apply failed", it) }
            }
        }

        var hooked = 0
        engine.findClass(BEAN_DESERIALIZER)?.let { cls ->
            for (m in cls.declaredMethods) {
                if (m.name != "deserialze") continue
                if (m.parameterCount != 3) continue
                if (m.parameterTypes[1] != Type::class.java) continue
                runCatching { XposedBridge.hookMethod(m, hook); hooked++ }
            }
        }
        engine.findClass(JSON)?.let { cls ->
            for (m in cls.declaredMethods) {
                if (m.name != "parseArray" && m.name != "parseObject") continue
                if (!Modifier.isStatic(m.modifiers)) continue
                if (m.parameterTypes.isEmpty() || m.parameterTypes[0] != String::class.java) continue
                runCatching { XposedBridge.hookMethod(m, hook); hooked++ }
            }
        }

        Logger.i("[live_cleanup] hooked=$hooked noBlock=$liveNoBlock banner=$hideBanner label=$blockLabel")
    }

    /** 直播间：清拉黑/屏蔽信息（`BiliLiveRoomInfo.blockInfo = null`）。 */
    private fun cleanRoomInfo(target: Any) {
        if (!target.javaClass.simpleName.contains(ROOM_INFO)) return
        writeField(target, "blockInfo", null)
    }

    /** 直播首页：pageList 去掉 `cardType == "banner_v2"` 的元素（与 1.28.6 的过滤条件一致）。 */
    private fun cleanHomeFeed(target: Any) {
        if (!target.javaClass.simpleName.contains(HOME_FEED_PAGE)) return
        val pageList = readField(target, "pageList") as? MutableList<*> ?: return
        runCatching {
            pageList.removeAll { item ->
                val cardType = item?.let { readField(it, "cardType") } as? String
                cardType == BANNER_CARD_TYPE
            }
        }.onFailure { Logger.e("[live_cleanup] banner removal failed", it) }
    }

    /** 推荐卡片：清掉 headLine 上的活动标签。 */
    private fun cleanActivityLabel(target: Any) {
        val headLine = runCatching { target.javaClass.getMethod("getHeadLine").invoke(target) }
            .getOrNull() ?: return
        runCatching { headLine.javaClass.getMethod("clearLabel").invoke(headLine) }
    }

    private fun fieldOf(target: Any, name: String): Field? {
        var cls: Class<*>? = target.javaClass
        while (cls != null && cls != Any::class.java) {
            runCatching { cls!!.getDeclaredField(name) }.getOrNull()?.let {
                it.isAccessible = true
                return it
            }
            cls = cls.superclass
        }
        return null
    }

    private fun readField(target: Any, name: String): Any? =
        runCatching { fieldOf(target, name)?.get(target) }.getOrNull()

    private fun writeField(target: Any, name: String, value: Any?) {
        runCatching { fieldOf(target, name)?.set(target, value) }
            .onFailure { Logger.e("[live_cleanup] write $name failed", it) }
    }
}
