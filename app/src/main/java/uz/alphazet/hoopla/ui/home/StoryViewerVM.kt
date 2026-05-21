package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.domain.repositories.StoryRepo
import uz.alphazet.domain.ui.BaseVM

class StoryViewerVM(private val repo: StoryRepo) : BaseVM() {

    private val cache = mutableMapOf<Int, SharedFlow<UIResource<StoryDetailData>>>()

    suspend fun getStory(id: Int): SharedFlow<UIResource<StoryDetailData>> = ensureCached(id)

    fun prefetch(id: Int) {
        if (id <= 0) return
        viewModelScope.launch { ensureCached(id) }
    }

    private suspend fun ensureCached(id: Int): SharedFlow<UIResource<StoryDetailData>> {
        cache[id]?.let { return it }
        val flow = repo.getStory(id).shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )
        cache[id] = flow
        return flow
    }
}