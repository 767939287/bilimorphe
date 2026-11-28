package app.bilimorphe.patch

import app.bilimorphe.patch.feature.BlockBackFeature
import app.bilimorphe.patch.feature.BottomTabFeature
import app.bilimorphe.patch.feature.ColorOsTransitionsFeature
import app.bilimorphe.patch.feature.CommentTweaksFeature
import app.bilimorphe.patch.feature.ConfigOverrideFeature
import app.bilimorphe.patch.feature.DisableHotPatchFeature
import app.bilimorphe.patch.feature.JsonCleanupFeature
import app.bilimorphe.patch.feature.LiveDataCleanupFeature
import app.bilimorphe.patch.feature.MainScreenTweaksFeature
import app.bilimorphe.patch.feature.MiniPlayerProbeFeature
import app.bilimorphe.patch.feature.PicNoFoldFeature
import app.bilimorphe.patch.feature.PlayUrlTweaksFeature
import app.bilimorphe.patch.feature.RemoveVideoScreenshotWatermarkFeature

/**
 * Patch registry. The order matters: the bridge patch must run first so the
 * integrations runtime is ready before network hooks are installed.
 */
object Patches {
    const val TARGET_PACKAGE = "tv.danmaku.bili"

    private val patches: List<Patch> = listOf(
        StartupProbePatch,
        BridgePatch,
        OkHttpHookPatch,
        DisableHotPatchFeature,
        BlockBackFeature,
        RemoveVideoScreenshotWatermarkFeature,
        PicNoFoldFeature,
        ConfigOverrideFeature,
        CommentTweaksFeature,
        LiveDataCleanupFeature,
        PlayUrlTweaksFeature,
        MainScreenTweaksFeature,
        BottomTabFeature,
        JsonCleanupFeature,
        ColorOsTransitionsFeature,
        MiniPlayerProbeFeature,
    )

    fun forPackage(packageName: String): List<Patch>? =
        if (packageName == TARGET_PACKAGE) patches else null
}
