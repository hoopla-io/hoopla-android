package uz.alphazet.hoopla.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.databinding.ActivityStoryViewerBinding
import kotlin.math.abs

/**
 * Full-screen story viewer: a horizontal pager of story groups (one [StoryGroupFragment] per
 * story id) with a cube transition, swipe-down-to-dismiss, and a result that tells the launcher
 * which groups were viewed and whether a shop link was tapped.
 */
class StoryViewerActivity : BaseActivity(), StoryGroupFragment.Host {

    private lateinit var binding: ActivityStoryViewerBinding
    private val viewModel: StoryViewerVM by viewModel()

    private var storyIds: IntArray = intArrayOf()

    /** Groups that actually showed a slide (not merely paged past / failed to load). */
    private val viewedGroupIds = linkedSetOf<Int>()

    /** Shop the customer tapped inside a story; reported back for the launcher to open. */
    private var openShopId: Int? = null

    // ---- swipe-down-to-dismiss state ----
    private var touchSlop = 0
    private var dismissVelocityPx = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragStartY = 0f
    private var isDragging = false
    private var isDismissing = false
    private var velocityTracker: VelocityTracker? = null

    /** Programmatic (timer / tap driven) group transition currently playing, if any. */
    private var groupTransition: ValueAnimator? = null

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            prefetchAround(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            // Freeze the visible group's timer while a group swipe (user or programmatic) is
            // in flight — otherwise it can finish mid-transition and fire a second one.
            when (state) {
                ViewPager2.SCROLL_STATE_DRAGGING -> currentGroup()?.setHeldByHost(true)
                ViewPager2.SCROLL_STATE_IDLE -> currentGroup()?.setHeldByHost(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val explicitIds = intent.getIntArrayExtra(STORY_IDS)
            ?.filter { it > 0 }?.distinct()?.toIntArray()?.takeIf { it.isNotEmpty() }
        val singleId = intent.getIntExtra(STORY_ID, -1).takeIf { it > 0 }
        storyIds = explicitIds ?: singleId?.let { intArrayOf(it) } ?: run {
            finish()
            return
        }

        val config = ViewConfiguration.get(this)
        touchSlop = config.scaledTouchSlop
        dismissVelocityPx = DISMISS_VELOCITY_DP_PER_S * resources.displayMetrics.density

        binding.outerPager.adapter = StoryGroupPagerAdapter(this, storyIds)
        binding.outerPager.offscreenPageLimit = 1
        binding.outerPager.setPageTransformer(CubePageTransformer())
        binding.outerPager.registerOnPageChangeCallback(pageChangeCallback)

        val restoredIndex = savedInstanceState?.getInt(KEY_GROUP_INDEX)
            ?: intent.getIntExtra(START_INDEX, 0)
        val startIndex = restoredIndex.coerceIn(0, storyIds.lastIndex)
        binding.outerPager.setCurrentItem(startIndex, false)
        // setCurrentItem(0, false) is a no-op for the default initial page and
        // won't fire onPageSelected — seed the prefetches explicitly.
        prefetchAround(startIndex)
    }

    /**
     * Warm the detail cache two groups out in each direction. The immediate neighbours are
     * already live pages (offscreenPageLimit = 1) and load their own first slide; this makes
     * sure the *next* neighbours are ready by the time the user reaches them.
     */
    private fun prefetchAround(position: Int) {
        for (offset in intArrayOf(-2, -1, 1, 2)) {
            storyIds.getOrNull(position + offset)?.let(viewModel::prefetch)
        }
    }

    private fun currentGroup(): StoryGroupFragment? =
        supportFragmentManager.fragments
            .filterIsInstance<StoryGroupFragment>()
            .firstOrNull { it.isResumed }

    override fun finish() {
        if (viewedGroupIds.isNotEmpty() || openShopId != null) {
            val data = Intent().putExtra(VIEWED_IDS, viewedGroupIds.toIntArray())
            openShopId?.let { data.putExtra(OPEN_SHOP_ID, it) }
            setResult(RESULT_OK, data)
        }
        super.finish()
    }

    // ---- Host ---------------------------------------------------------------------------

    override fun openShop(shopId: Int) {
        openShopId = shopId
        finish()
    }

    override fun closeViewer() = finish()

    override fun onGroupViewed(storyId: Int) {
        viewedGroupIds.add(storyId)
    }

    override fun goToNextGroup() {
        if (groupTransition?.isRunning == true) return
        val next = binding.outerPager.currentItem + 1
        if (next >= storyIds.size) {
            finish()
        } else {
            binding.outerPager.smoothScrollTo(next, GROUP_TRANSITION_MS)
        }
    }

    override fun goToPreviousGroup(): Boolean {
        if (groupTransition?.isRunning == true) return true
        val prev = binding.outerPager.currentItem - 1
        if (prev < 0) return false
        binding.outerPager.smoothScrollTo(prev, GROUP_TRANSITION_MS)
        return true
    }

    override fun updateStatusBarViewHeight() {}

    // ---- swipe-down-to-dismiss ----------------------------------------------------------

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isDismissing) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                isDragging = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                if (!isDragging && canStartDrag()) {
                    val dy = ev.y - downY
                    val dx = ev.x - downX
                    if (dy > touchSlop && dy > abs(dx) * VERTICAL_BIAS) {
                        isDragging = true
                        dragStartY = ev.y
                        currentGroup()?.setHeldByHost(true)
                        // The gesture is ours now: cancel whatever press the pages had going
                        // (otherwise the tap zone fires a click / long-press on release).
                        val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                    }
                }
                if (isDragging) {
                    applyDrag((ev.y - dragStartY).coerceAtLeast(0f))
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val tracker = velocityTracker
                velocityTracker = null
                if (isDragging) {
                    isDragging = false
                    var velocityY = 0f
                    if (tracker != null) {
                        tracker.addMovement(ev)
                        tracker.computeCurrentVelocity(1000)
                        velocityY = tracker.yVelocity
                    }
                    tracker?.recycle()
                    settleDrag(flingDown = ev.actionMasked == MotionEvent.ACTION_UP && velocityY > dismissVelocityPx)
                    return true
                }
                tracker?.recycle()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun canStartDrag(): Boolean =
        binding.outerPager.scrollState == ViewPager2.SCROLL_STATE_IDLE &&
                !binding.outerPager.isFakeDragging

    private fun applyDrag(translationY: Float) {
        val root = binding.root
        val fraction = (translationY / root.height.coerceAtLeast(1)).coerceIn(0f, 1f)
        root.translationY = translationY
        val scale = 1f - fraction * DRAG_MAX_SCALE_DOWN
        root.scaleX = scale
        root.scaleY = scale
        root.alpha = 1f - fraction * DRAG_MAX_FADE
    }

    private fun settleDrag(flingDown: Boolean) {
        val root = binding.root
        val ty = root.translationY
        val dismiss = flingDown || ty > root.height / DRAG_DISMISS_FRACTION
        if (dismiss) {
            isDismissing = true
            root.animate()
                .translationY(root.height.toFloat())
                .scaleX(1f - DRAG_MAX_SCALE_DOWN)
                .scaleY(1f - DRAG_MAX_SCALE_DOWN)
                .alpha(0f)
                .setDuration(DRAG_SETTLE_MS)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    finish()
                    // The view already animated itself out — skip the window exit animation.
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }
                .start()
        } else {
            root.animate()
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DRAG_SETTLE_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
            currentGroup()?.setHeldByHost(false)
        }
    }

    // ---- group transition ---------------------------------------------------------------

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
        groupTransition?.cancel()
        // beginFakeDrag() refuses while the user's finger is on the pager — fall back to the
        // stock animation rather than silently dropping the transition.
        if (!beginFakeDrag()) {
            setCurrentItem(item, true)
            return
        }

        val pxToDrag = w * (item - currentItem)
        var previousValue = 0
        groupTransition = ValueAnimator.ofInt(0, pxToDrag).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val current = va.animatedValue as Int
                if (isFakeDragging) fakeDragBy(-(current - previousValue).toFloat())
                previousValue = current
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isFakeDragging) endFakeDrag()
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (isFakeDragging) endFakeDrag()
                }
            })
            start()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_GROUP_INDEX, binding.outerPager.currentItem)
    }

    override fun onDestroy() {
        groupTransition?.cancel()
        velocityTracker?.recycle()
        velocityTracker = null
        if (::binding.isInitialized) {
            binding.outerPager.unregisterOnPageChangeCallback(pageChangeCallback)
        }
        super.onDestroy()
    }

    companion object {
        const val STORY_ID = "story_id"
        const val STORY_IDS = "story_ids"
        const val START_INDEX = "start_index"
        const val VIEWED_IDS = "viewed_ids"
        const val OPEN_SHOP_ID = "open_shop_id"
        private const val KEY_GROUP_INDEX = "current_group_index"
        private const val DRAG_SETTLE_MS = 220L
        private const val DRAG_DISMISS_FRACTION = 4f
        private const val DRAG_MAX_SCALE_DOWN = 0.15f
        private const val DRAG_MAX_FADE = 0.4f
        private const val DISMISS_VELOCITY_DP_PER_S = 1000f
        private const val VERTICAL_BIAS = 1.5f
        private const val GROUP_TRANSITION_MS = 450L
    }
}
