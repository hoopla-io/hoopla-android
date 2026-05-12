package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.domain.repositories.StoryRepo
import uz.alphazet.domain.ui.BaseVM

class StoryViewerVM(private val repo: StoryRepo) : BaseVM() {

    suspend fun getStory(id: Int): SharedFlow<UIResource<StoryDetailData>> {
        return repo.getStory(id)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}