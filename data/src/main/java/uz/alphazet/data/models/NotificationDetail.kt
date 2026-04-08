package uz.alphazet.data.models

data class NotificationDetail(
    val countReads: Int?,
    val createdAt: String?,
    val files: Files?,
    val isNew: Boolean?,
    val notificationDescription: String?,
    val notificationId: Int?,
    val notificationTitle: String?,
    val shareUrl: String?,
    val url: String?
) {
    data class Files(
        val imageUrl: String?
    )
}