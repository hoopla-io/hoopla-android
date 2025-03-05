package uz.alphazet.domain.permission

import android.os.Build
import androidx.annotation.RequiresApi

typealias Body = () -> Unit

interface PermissionManager {
    fun checkCameraPermission( onDeny: Body = {}, onAllow: Body)
    fun checkContactsPermission(onDeny: Body = {}, onAllow: Body)
    fun checkCallPhonePermission(onDeny: Body = {}, onAllow: Body)
    fun checkFineLocationPermission(onDeny: Body = {}, onAllow: Body)
    fun checkCoarseLocationPermission(onDeny: Body = {}, onAllow: Body)
    fun checkFineCoarseLocationPermission(onDeny: Body = {}, onAllow: Body)
    fun checkReadExternalStoragePermission(onDeny: Body = {}, onAllow: Body)
    fun checkWriteExternalStoragePermission(onDeny: Body = {}, onAllow: Body)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkReadImagePermission(onDeny: Body = {}, onAllow: Body)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPostNotificationPermission(onDeny: Body = {}, onAllow: Body)
}

