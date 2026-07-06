package uz.alphazet.hoopla.ui.profile.devices

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DeviceSessionData
import uz.alphazet.domain.network.DeviceInfoProvider
import uz.alphazet.domain.repositories.DeviceRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class DevicesVM(
    private val repo: DeviceRepo,
    private val deviceInfo: DeviceInfoProvider
) : BaseVM() {

    private val devicesEmitter: MutableStateFlow<UIResource<List<DeviceSessionData>>> =
        MutableStateFlow(UIResource.Loading)
    val devicesFlow: StateFlow<UIResource<List<DeviceSessionData>>> get() = devicesEmitter

    fun getDevices() {
        launch { devicesEmitter.load { repo.getDevices() } }
    }

    suspend fun revokeDevice(id: Int): SharedFlow<UIResource<Any>> =
        repo.revokeDevice(id).shareIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Best-effort match of the session running on this device. The `/user/devices`
     * response has no deviceId, so we match on device name + platform (the fields we
     * send on confirm-sms) and, when several match, take the most recently active.
     */
    fun findCurrentDeviceId(devices: List<DeviceSessionData>): Int? =
        devices
            .filter { it.deviceName == deviceInfo.deviceName && it.platform == deviceInfo.platform }
            .maxByOrNull { it.lastActiveAt ?: Long.MIN_VALUE }
            ?.id

}
