package uz.alphazet.hoopla.util

import android.app.Activity
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.launch
import uz.alphazet.domain.R
import uz.alphazet.domain.utils.log

/**
 * Drives Google Play In-App Updates with the FLEXIBLE flow: the update downloads in
 * the background while the user keeps using the app, and a Snackbar prompts a restart
 * once it is ready to install.
 *
 * Construct this from an Activity's `onCreate` (before the activity is STARTED, so the
 * ActivityResult launcher can register) and call [checkForUpdate]. The instance
 * registers itself as a lifecycle observer to re-surface a downloaded update.
 */
class InAppUpdateManager(
    private val activity: AppCompatActivity,
    private val rootView: View,
    private val anchorView: View? = null,
) : DefaultLifecycleObserver {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                "In-app update flow not completed: resultCode=${result.resultCode}".log(TAG)
            }
        }

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showRestartSnackbar()
        }
    }

    init {
        activity.lifecycle.addObserver(this)
        appUpdateManager.registerListener(installStateListener)
    }

    /** Queries Play for an available update and launches the flexible flow. */
    fun checkForUpdate() {
        activity.lifecycleScope.launch {
            val info = runCatching { appUpdateManager.requestAppUpdateInfo() }
                .onFailure { it.log(TAG) }
                .getOrNull() ?: return@launch

            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                startUpdate(info)
            }
        }
    }

    private fun startUpdate(info: AppUpdateInfo) {
        runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                updateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        }.onFailure { it.log(TAG) }
    }

    override fun onResume(owner: LifecycleOwner) {
        activity.lifecycleScope.launch {
            val info = runCatching { appUpdateManager.requestAppUpdateInfo() }
                .getOrNull() ?: return@launch

            // A flexible update finished downloading while the app was backgrounded.
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun showRestartSnackbar() {
        Snackbar.make(rootView, R.string.update_downloaded, Snackbar.LENGTH_INDEFINITE)
            .apply { anchorView?.let { setAnchorView(it) } }
            .setAction(R.string.restart) { appUpdateManager.completeUpdate() }
            .show()
    }

    private companion object {
        const val TAG = "InAppUpdateManager"
    }
}