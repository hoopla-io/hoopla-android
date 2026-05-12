package uz.alphazet.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.rv.BaseItem

data class StoryItemData(
    val id: Int?,
    val title: String?,
    val coverImageUrl: String?,
    val isSeen: Boolean?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}

data class StoryDetailData(
    val id: Int?,
    val title: String?,
    val coverImageUrl: String?,
    val items: List<StorySlideData>?
)

@Parcelize
data class StorySlideData(
    val id: Int?,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val linkType: String?,
    val linkValue: String?,
    val duration: Int?
) : Parcelable

object StoryLinkTypes {
    const val PARTNER = "partner"
    const val DRINK = "drink"
    const val URL = "url"
}