package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class NotificationItemData(
    val createdAt: String?,
    val files: Files?,
    val isNew: Boolean?,
    val notificationDescription: String?,
    val notificationId: Int?,
    val notificationTitle: String?,
    val url: String?
) : BaseItem {
    override val uniqueId: String = notificationId.toString()

    data class Files(
        val imageUrl: String?
    )
}