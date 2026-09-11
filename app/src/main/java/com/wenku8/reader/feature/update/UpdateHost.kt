package com.wenku8.reader.feature.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.core.util.ApkInstaller

/**
 * Global update overlay hosted above the NavHost. Drives the dialogs and the
 * unknown-source settings round trip; the VM is Activity-scoped.
 */
@Composable
fun UpdateHost(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val phase = state.phase

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // resultCode is unreliable here — re-check the actual permission.
        val current = viewModel.state.value.phase
        if (current is UpdatePhase.AwaitInstallPermission && ApkInstaller.canRequestInstalls(context)) {
            if (runCatching { ApkInstaller.install(context, current.apk) }.isSuccess) {
                viewModel.onInstallDispatched()
            } else {
                viewModel.onInstallFailed()
            }
        }
    }

    LaunchedEffect(phase) {
        val p = phase as? UpdatePhase.ReadyToInstall ?: return@LaunchedEffect
        if (ApkInstaller.canRequestInstalls(context)) {
            if (runCatching { ApkInstaller.install(context, p.apk) }.isSuccess) {
                viewModel.onInstallDispatched()
            } else {
                viewModel.onInstallFailed()
            }
        } else {
            viewModel.requestInstallPermission(p.release, p.apk)
        }
    }

    when (phase) {
        is UpdatePhase.Available -> UpdateAvailableDialog(
            release = phase.release,
            onUpdate = viewModel::startDownload,
            onSkip = viewModel::skipThisVersion,
            onLater = viewModel::later,
        )
        is UpdatePhase.Downloading -> DownloadProgressDialog(
            phase = phase,
            onCancel = viewModel::cancelDownload,
        )
        is UpdatePhase.AwaitInstallPermission -> InstallPermissionDialog(
            onSettings = {
                settingsLauncher.launch(ApkInstaller.permissionSettingsIntent(context))
            },
            onCancel = viewModel::dismiss,
        )
        is UpdatePhase.Error -> UpdateErrorDialog(
            message = phase.message,
            retry = phase.retry,
            onDismiss = viewModel::dismissError,
        )
        UpdatePhase.Idle,
        is UpdatePhase.ReadyToInstall -> Unit
    }
}
