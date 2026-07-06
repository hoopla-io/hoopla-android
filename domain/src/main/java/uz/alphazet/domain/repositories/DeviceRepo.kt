package uz.alphazet.domain.repositories

import uz.alphazet.data.services.DevicesService
import uz.alphazet.domain.network.BaseRepo

class DeviceRepo(private val api: DevicesService) : BaseRepo() {

    suspend fun getDevices() = handle { api.getDevices() }

    suspend fun revokeDevice(id: Int) = handleFlow { api.revokeDevice(id) }

}
