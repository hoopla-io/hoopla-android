package uz.i_tv.domain.repositories

import uz.i_tv.data.services.SubscriptionService
import uz.i_tv.domain.network.BaseRepo

class SubscriptionRepo(private val api: SubscriptionService) : BaseRepo() {

    suspend fun getSubscriptions() = handleFlow {
        api.getSubscriptions()
    }

}