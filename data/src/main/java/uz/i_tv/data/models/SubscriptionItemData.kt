package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

data class SubscriptionItemData(
    val currency: String?,
    val days: Int?,
    val id: Int?,
    val name: String?,
    val price: Int?,
    val features: List<FeatureItemData?>?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}

data class FeatureItemData(
    val id: Int?,
    val feature: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}