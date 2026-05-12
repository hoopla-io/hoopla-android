package uz.alphazet.domain.repositories

import uz.alphazet.data.services.StoryService
import uz.alphazet.domain.network.BaseRepo

class StoryRepo(private val service: StoryService) : BaseRepo() {

    suspend fun getStories() = handle {
        service.getStories()
    }

    suspend fun getStory(id: Int) = handleFlow {
        service.getStory(id)
    }

}