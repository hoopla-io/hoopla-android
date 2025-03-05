package uz.i_tv.domain.repositories

import uz.i_tv.data.services.PaymentService
import uz.i_tv.domain.network.BaseRepo

class PaymentServiceRepo(private val api: PaymentService) : BaseRepo() {

    suspend fun getPaymentServices() = handleFlow {
        api.getPaymentServices()
    }

    suspend fun topUpViaPayService(id: Int, amount: Double) = handle {
        api.topUpViaPayService(id, amount)
    }

}