package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.domain.repositories.StoryRepo
import uz.alphazet.domain.ui.BaseVM

/**
 * Shared between [StoryViewerActivity] and every [StoryGroupFragment] it hosts, so a story
 * group is fetched once no matter how many times its page is created, preloaded or re-entered.
 */
class StoryViewerVM(private val repo: StoryRepo) : BaseVM() {

    private val cache = mutableMapOf<Int, SharedFlow<UIResource<StoryDetailData>>>()

    /** Bumped per id on every (re)fetch so a stale flow's error can't evict a newer one. */
    private val generation = mutableMapOf<Int, Int>()

    /**
     * The story detail for [id]. Starts with [UIResource.Loading] (the repository flow itself
     * only emits the final result) and replays the result to late subscribers. A failed fetch
     * is evicted from the cache, so the next call — e.g. a retry tap — actually re-requests.
     */
    fun getStory(id: Int, refresh: Boolean = false): Flow<UIResource<StoryDetailData>> {
        if (refresh) cache.remove(id)
        return ensureCached(id)
    }

    fun prefetch(id: Int) {
        if (id <= 0) return
        ensureCached(id)
    }

    private fun ensureCached(id: Int): SharedFlow<UIResource<StoryDetailData>> {
        cache[id]?.let { return it }
        val gen = (generation[id] ?: 0) + 1
        generation[id] = gen
        val shared = flow {
            emit(UIResource.Loading)
            repo.getStory(id).collect { emit(it) }
        }
            .onEach {
                if (it is UIResource.Error && generation[id] == gen) cache.remove(id)
            }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                replay = 1
            )
        cache[id] = shared
        return shared
    }
}
