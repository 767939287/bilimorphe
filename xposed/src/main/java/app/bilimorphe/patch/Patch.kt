package app.bilimorphe.patch

import app.bilimorphe.PatchEngine

/**
 * 补丁生效范围：B 站有 158 处 `android:process`，不同补丁适合不同进程。
 *
 * - [ALL]：每个进程都装（默认，如网络/数据类）
 * - [MAIN_ONLY]：仅主进程（如宿主设置页入口、Application 级初始化）
 * - [SUB_ONLY]：仅子进程
 */
enum class ProcessScope {
    ALL,
    MAIN_ONLY,
    SUB_ONLY;

    fun allows(engine: PatchEngine): Boolean = when (this) {
        ALL -> true
        MAIN_ONLY -> engine.isMainProcess
        SUB_ONLY -> !engine.isMainProcess
    }
}

/**
 * A patch is a self contained unit of runtime modification.
 * Each patch is applied once per process and must be idempotent safe.
 */
abstract class Patch(
    val name: String,
    val description: String = "",
    /** 生效的进程范围，默认所有进程。 */
    val scope: ProcessScope = ProcessScope.ALL,
) {
    abstract fun applyTo(engine: PatchEngine)
}
