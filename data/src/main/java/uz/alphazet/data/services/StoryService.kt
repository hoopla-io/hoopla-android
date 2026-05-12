package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Path
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.data.models.StoryItemData

interface StoryService {

    @GET("v1/stories/list")
    suspend fun getStories(): BaseResponse<List<StoryItemData>>

    @GET("v1/stories/show/{id}")
    suspend fun getStory(@Path("id") id: Int): BaseResponse<StoryDetailData>

}