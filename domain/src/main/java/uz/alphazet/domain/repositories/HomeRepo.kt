package uz.alphazet.domain.repositories

import uz.alphazet.data.services.HomeService
import uz.alphazet.domain.network.BaseRepo

class HomeRepo(private val homeService: HomeService) : BaseRepo() {

    suspend fun getLoyaltyCard() = handleFlow {
        homeService.getLoyaltyCard()
    }

    suspend fun getNearShops(lat: Double, long: Double, name: String?) = handleFlow {
        homeService.getNearShops(lat, long, name)
    }

    suspend fun getPendingFeedbacks() = handle { homeService.getPendingFeedbacks() }

    suspend fun submitFeedback(orderId: Int, rating: Int, comment: String?) = handle {
        val body = buildMap<String, Any> {
            put("rating", rating)
            if (!comment.isNullOrBlank()) put("comment", comment)
        }
        homeService.submitFeedback(orderId, body)
    }

}