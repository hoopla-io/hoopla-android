package uz.alphazet.hoopla.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.databinding.ActivityStoryViewerBinding
import kotlin.math.abs

class StoryViewerActivity : BaseActivity(), StoryGroupFragment.Host {

    private lateinit var binding: ActivityStoryViewerBinding
    private val viewModel: StoryViewerVM by viewModel()

    private var storyIds: IntArray = intArrayOf()
    private val viewedGroupIds = linkedSetOf<Int>()

    private var isDragging = false
    private var dragStartY = 0f
    private var touchSlop = 0

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            storyIds.getOrNull(position)?.let(viewedGroupIds::add)
            preloadFirstSlide(position - 1)
            preloadFirstSlide(position + 1)
        }
    }

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                if (!isDragging) {
                    val dy = e2.y - e1.y
                    val dx = e2.x - e1.x
                    if (dy > touchSlop && abs(dy) > abs(dx)) {
                        isDragging = true
                        dragStartY = e1.y
                    }
                }
                if (isDragging) {
                    binding.root.translationY = (e2.y - dragStartY).coerceAtLeast(0f)
                    binding.root.alpha = 1f - (binding.root.translationY / binding.root.height)
                        .coerceIn(0f, 1f) * 0.5f
                    return true
                }
                return false
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val explicitIds = intent.getIntArrayExtra(STORY_IDS)?.takeIf { it.isNotEmpty() }
        val singleId = intent.getIntExtra(STORY_ID, -1).takeIf { it > 0 }
        storyIds = explicitIds ?: singleId?.let { intArrayOf(it) } ?: run {
            finish()
            return
        }

        touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop

        binding.outerPager.adapter = StoryGroupPagerAdapter(this, storyIds)
        binding.outerPager.offscreenPageLimit = 1
        binding.outerPager.setPageTransformer(CubePageTransformer())
        binding.outerPager.registerOnPageChangeCallback(pageChangeCallback)

        val restoredIndex = savedInstanceState?.getInt(KEY_GROUP_INDEX)
            ?: intent.getIntExtra(START_INDEX, 0)
        val startIndex = restoredIndex.coerceIn(0, storyIds.lastIndex)
        binding.outerPager.setCurrentItem(startIndex, false)
        // setCurrentItem(0, false) is a no-op for the default initial page and
        // won't fire onPageSelected — seed the viewed set + adjacent preloads explicitly.
        storyIds.getOrNull(startIndex)?.let(viewedGroupIds::add)
        preloadFirstSlide(startIndex - 1)
        preloadFirstSlide(startIndex + 1)

        binding.btnClose.setOnClickListener { finish() }
    }

    private fun preloadFirstSlide(groupIndex: Int) {
        val id = storyIds.getOrNull(groupIndex)?.takeIf { it > 0 } ?: return
        launch {
            val resource = viewModel.getStory(id).first()
            val url = (resource as? UIResource.Success)?.data?.items?.firstOrNull()?.imageUrl
            if (!url.isNullOrBlank()) {
                val request = ImageRequest.Builder(this@StoryViewerActivity)
                    .data(url)
                    .build()
                SingletonImageLoader.get(this@StoryViewerActivity).enqueue(request)
            }
        }
    }

    override fun finish() {
        if (viewedGroupIds.isNotEmpty()) {
            val data = Intent().putExtra(VIEWED_IDS, viewedGroupIds.toIntArray())
            setResult(RESULT_OK, data)
        }
        super.finish()
    }

    override fun updateStatusBarViewHeight() {}

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP ||
            ev.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            if (isDragging) {
                isDragging = false
                settleDrag()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_GROUP_INDEX, binding.outerPager.currentItem)
    }

    override fun goToNextGroup() {
        val next = binding.outerPager.currentItem + 1
        if (next >= storyIds.size) {
            finish()
        } else {
            binding.outerPager.smoothScrollTo(next, GROUP_TRANSITION_MS)
        }
    }

    override fun goToPreviousGroup() {
        val prev = binding.outerPager.currentItem - 1
        if (prev < 0) return
        binding.outerPager.smoothScrollTo(prev, GROUP_TRANSITION_MS)
    }

    /**
     * Programmatic page change with a controllable duration. ViewPager2 hardcodes
     * its own smooth-scroll timing, so we drive the transition via fakeDrag
     * instead — the page transformer (CubePageTransformer) still runs on every
     * frame, just over our duration instead of ~250ms.
     */
    private fun ViewPager2.smoothScrollTo(item: Int, durationMs: Long) {
        if (item == currentItem) return
        val w = width
        if (w <= 0) {
            setCurrentItem(item, false)
            return
        }
        if (isFakeDragging) endFakeDrag()

        val pxToDrag = w * (item - currentItem)
        val animator = ValueAnimator.ofInt(0, pxToDrag).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
        }
        var previousValue = 0
        animator.addUpdateListener { va ->
            val current = va.animatedValue as Int
            fakeDragBy(-(current - previousValue).toFloat())
            previousValue = current
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                beginFakeDrag()
            }

            override fun onAnimationEnd(animation: Animator) {
                if (isFakeDragging) endFakeDrag()
            }

            override fun onAnimationCancel(animation: Animator) {
                if (isFakeDragging) endFakeDrag()
            }
        })
        animator.start()
    }

    private fun settleDrag() {
        val ty = binding.root.translationY
        if (ty > binding.root.height / DRAG_DISMISS_FRACTION) {
            binding.root.animate()
                .translationY(binding.root.height.toFloat())
                .alpha(0f)
                .setDuration(DRAG_SETTLE_MS)
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
        } else {
            binding.root.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(DRAG_SETTLE_MS)
                .start()
        }
    }

    override fun onDestroy() {
        binding.outerPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    companion object {
        const val STORY_ID = "story_id"
        const val STORY_IDS = "story_ids"
        const val START_INDEX = "start_index"
        const val VIEWED_IDS = "viewed_ids"
        private const val KEY_GROUP_INDEX = "current_group_index"
        private const val DRAG_SETTLE_MS = 200L
        private const val DRAG_DISMISS_FRACTION = 4f
        private const val GROUP_TRANSITION_MS = 500L
    }
}