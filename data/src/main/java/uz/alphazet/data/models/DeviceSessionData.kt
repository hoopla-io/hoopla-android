package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class DeviceSessionData(
    val id: Int?,
    val deviceName: String?,
    val platform: String?,
    val appVersion: String?,
    val ip: String?,
    val lastActiveAt: Long?,
    val createdAt: Long?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}
