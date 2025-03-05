package uz.alphazet.domain.repositories

import uz.alphazet.data.services.PaymentService
import uz.alphazet.domain.network.BaseRepo

class PaymentServiceRepo(private val api: PaymentService) : BaseRepo() {

    suspend fun getPaymentServices() = handleFlow {
        api.getPaymentServices()
    }

    suspend fun topUpViaPayService(id: Int, amount: Double) = handle {
        api.topUpViaPayService(id, amount)
    }

}