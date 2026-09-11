package com.wenku8.reader.feature.update

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenku8.reader.core.data.ApkDownloader
import com.wenku8.reader.core.data.GitHubUpdateApi
import com.wenku8.reader.core.data.UpdateConfig
import com.wenku8.reader.core.data.UserPreferences
import com.wenku8.reader.core.data.model.ReleaseInfo
import com.wenku8.reader.core.util.AppLog
import com.wenku8.reader.core.util.AppVersion
import com.wenku8.reader.core.util.isNewerVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data class Available(val release: ReleaseInfo) : UpdatePhase
    data class Downloading(
        val release: ReleaseInfo,
        val progress: Float,
        val downloaded: Long,
        val total: Long,
    ) : UpdatePhase
    data class ReadyToInstall(val release: ReleaseInfo, val apk: File) : UpdatePhase
    data class AwaitInstallPermission(val release: ReleaseInfo, val apk: File) : UpdatePhase
    data class Error(val message: String, val retry: (() -> Unit)?) : UpdatePhase
}

data class UpdateUiState(
    val checking: Boolean = false,
    val phase: UpdatePhase = UpdatePhase.Idle,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateApi: GitHubUpdateApi,
    private val downloader: ApkDownloader,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val checkMutex = Mutex()
    private var downloadJob: Job? = null
    private var pendingRelease: ReleaseInfo? = null

    init {
        viewModelScope.launch { autoCheckIfNeeded() }
    }

    private fun isOnline(): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    private suspend fun autoCheckIfNeeded() {
        if (!UpdateConfig.enabled) return
        if (!isOnline()) return

        val now = System.currentTimeMillis()
        val last = preferences.getUpdateLastCheckAt()
        if (now - last < UpdateConfig.CHECK_INTERVAL_MS) {
            AppLog.d(UpdateConfig.TAG, "距上次检查不足 24h，跳过自动检查")
            return
        }
        checkForUpdate(manual = false)
    }

    /** Manual entry point from the dev-log screen; ignores the 24h gate. */
    fun checkNow() {
        if (!UpdateConfig.enabled) {
            AppLog.i(UpdateConfig.TAG, "更新检查未启用（UPDATE_CHECK_ENABLED=false）")
            return
        }
        viewModelScope.launch { checkForUpdate(manual = true) }
    }

    private suspend fun checkForUpdate(manual: Boolean) {
        checkMutex.withLock {
            if (_state.value.checking) return@withLock
            _state.value = _state.value.copy(checking = true)
            AppLog.i(UpdateConfig.TAG, if (manual) "开始手动检查更新…" else "开始检查更新…")
            val release = updateApi.fetchLatestRelease()
            val now = System.currentTimeMillis()

            if (release == null) {
                // Failure (network/404/rate limit): short backoff so cold starts
                // don't burn a 15s timeout every time in restricted networks.
                preferences.setUpdateLastCheckAt(now - UpdateConfig.CHECK_INTERVAL_MS + UpdateConfig.FAIL_BACKOFF_MS)
                _state.value = _state.value.copy(checking = false)
                if (manual) AppLog.w(UpdateConfig.TAG, "检查更新失败或暂无可用 release")
                return@withLock
            }

            preferences.setUpdateLastCheckAt(now)
            val installed = AppVersion.installedVersionName(context)
            val skipped = preferences.getUpdateSkippedVersion()
            val newer = isNewerVersion(release.tag, installed)
            AppLog.i(UpdateConfig.TAG, "最新版本 ${release.tag}，当前 $installed；hasApk=${release.hasApk}")

            when {
                !release.hasApk -> if (manual) AppLog.w(UpdateConfig.TAG, "最新 release 未包含 APK 资产")
                !newer -> if (manual) AppLog.i(UpdateConfig.TAG, "当前已是最新版本")
                // "跳过此版本" only suppresses the automatic prompt; a manual check still shows it.
                !manual && release.tag == skipped -> AppLog.d(UpdateConfig.TAG, "版本 ${release.tag} 已被跳过")
                _state.value.phase is UpdatePhase.Available -> Unit
                else -> {
                    pendingRelease = release
                    _state.value = _state.value.copy(phase = UpdatePhase.Available(release))
                    AppLog.i(UpdateConfig.TAG, "发现新版本 ${release.tag}")
                }
            }
            _state.value = _state.value.copy(checking = false)
        }
    }

    fun skipThisVersion() {
        val release = pendingRelease ?: (_state.value.phase as? UpdatePhase.Available)?.release
        viewModelScope.launch {
            release?.let { preferences.setUpdateSkippedVersion(it.tag) }
            AppLog.i(UpdateConfig.TAG, "已跳过版本 ${release?.tag}")
        }
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
        pendingRelease = null
    }

    /** Dismiss the prompt and suppress auto-prompting for another interval. */
    fun later() {
        viewModelScope.launch {
            preferences.setUpdateLastCheckAt(System.currentTimeMillis())
        }
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
        pendingRelease = null
    }

    fun startDownload() {
        val release = (_state.value.phase as? UpdatePhase.Available)?.release ?: pendingRelease ?: return
        if (!release.hasApk) return
        pendingRelease = release

        downloadJob?.cancel()
        _state.value = _state.value.copy(
            phase = UpdatePhase.Downloading(release, 0f, 0L, release.apkSize.coerceAtLeast(-1L)),
        )
        downloadJob = viewModelScope.launch {
            val apk = downloader.download(
                url = release.apkUrl!!,
                tag = release.tag,
                expectedSize = release.apkSize,
            ) { read, total ->
                val p = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Downloading(release, p, read, total),
                )
            }
            if (apk != null) {
                _state.value = _state.value.copy(phase = UpdatePhase.ReadyToInstall(release, apk))
            } else if (_state.value.phase is UpdatePhase.Downloading) {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Error("下载失败，请检查网络后重试", retry = ::startDownload),
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        AppLog.i(UpdateConfig.TAG, "已取消下载")
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
    }

    fun requestInstallPermission(release: ReleaseInfo, apk: File) {
        _state.value = _state.value.copy(
            phase = UpdatePhase.AwaitInstallPermission(release, apk),
        )
    }

    /** Called by UpdateHost after the system installer Intent has been fired. */
    fun onInstallDispatched() {
        AppLog.i(UpdateConfig.TAG, "已调起系统安装器")
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
    }

    fun onInstallFailed() {
        AppLog.w(UpdateConfig.TAG, "无法调起系统安装器")
        _state.value = _state.value.copy(
            phase = UpdatePhase.Error("无法调起系统安装器", retry = null),
        )
    }

    fun dismissError() {
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
    }

    fun dismiss() {
        _state.value = _state.value.copy(phase = UpdatePhase.Idle)
    }
}
