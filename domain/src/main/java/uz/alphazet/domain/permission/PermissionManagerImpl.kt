package uz.alphazet.domain.permission

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener

class PermissionManagerImpl(val context: Context) : PermissionManager {

    override fun checkCameraPermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.CAMERA, onDeny, onAllow)
    }

    override fun checkContactsPermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.READ_CONTACTS, onDeny, onAllow)
    }

    override fun checkCallPhonePermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.CALL_PHONE, onDeny, onAllow)
    }

    override fun checkFineLocationPermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, onDeny, onAllow)
    }

    override fun checkCoarseLocationPermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, onDeny, onAllow)
    }

    override fun checkFineCoarseLocationPermission(onDeny: Body, onAllow: Body) {
        checkPermissions(
            arrayListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            onDeny,
            onAllow
        )
    }

    override fun checkReadExternalStoragePermission(onDeny: Body, onAllow: Body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermission(Manifest.permission.READ_MEDIA_IMAGES, onDeny, onAllow)
        } else {
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, onDeny, onAllow)
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun checkReadImagePermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.READ_MEDIA_IMAGES, onDeny, onAllow)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun checkPostNotificationPermission(onDeny: Body, onAllow: Body) {
        checkPermission(Manifest.permission.POST_NOTIFICATIONS, onDeny, onAllow)
    }

    override fun checkWriteExternalStoragePermission(
        onDeny: Body,
        onAllow: Body
    ) {
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, onDeny, onAllow)
    }

    private fun checkPermission(permission: String, onDeny: Body, onAllow: Body) {
        Dexter.withContext(context)
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    onAllow()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    onDeny()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun checkPermissions(permissions: ArrayList<String>, onDeny: Body, onAllow: Body) {
        Dexter.withContext(context)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        onAllow()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    requests: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }

            }).check()
    }
}
