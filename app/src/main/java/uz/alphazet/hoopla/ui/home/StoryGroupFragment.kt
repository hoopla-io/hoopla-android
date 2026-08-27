package uz.alphazet.hoopla.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.data.models.StoryLinkTypes
import uz.alphazet.data.models.StorySlideData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenStoryGroupBinding
import kotlin.math.abs
import uz.alphazet.domain.R as DomainR

/**
 * One story group (one page of [StoryViewerActivity]'s outer pager): a timer-driven pager of
 * slides with Instagram-style progress bars, tap zones, hold-to-pause and an optional CTA.
 *
 * Lifecycle notes — the outer pager keeps neighbouring pages alive but only the visible one is
 * RESUMED, so everything that *advances time* (slide timer, auto-skip of an empty group) is
 * gated on [isResumed]; everything that merely *renders* (texts, bars, image) runs regardless,
 * so a page is fully drawn the moment it slides into view.
 */
class StoryGroupFragment : BaseFragment(R.layout.screen_story_group) {

    interface Host {
        fun goToNextGroup()

        /** @return false when there is no previous group — the caller then restarts its first slide. */
        fun goToPreviousGroup(): Boolean

        /** Closes the viewer and asks its launcher to open the partner's screen. */
        fun openPartner(partnerId: Int)

        fun closeViewer()

        /** A slide of [storyId] is actually on screen — the group counts as viewed. */
        fun onGroupViewed(storyId: Int)
    }

    private val binding by viewBinding(ScreenStoryGroupBinding::bind)
    private val viewModel: StoryViewerVM by viewModel(ownerProducer = { requireActivity() })
    private val host: Host? get() = activity as? Host

    private val storyId: Int by lazy { requireArguments().getInt(ARG_STORY_ID) }

    private val slideAdapter = StorySlideAdapter()
    private var slides: List<StorySlideData> = emptyList()
    private var currentIndex = 0
    private val progressBars = mutableListOf<View>()
    private var animator: ObjectAnimator? = null

    /** Story detail arrived with at least one slide. */
    private var isLoaded = false

    /** Story detail arrived with no slides — skipped as soon as this page is the visible one. */
    private var isEmptyGroup = false
    private var storyJob: Job? = null

    private var chromeViews: List<View> = emptyList()

    /** Finger is down on a tap zone: timer frozen immediately (Instagram-style). */
    private var isTouchHeld = false

    /** The hold lasted long enough to count as "peek": chrome faded out until release. */
    private var isChromeHidden = false

    /** The activity is mid-gesture (swiping between groups / dragging to dismiss): timer paused. */
    private var isHeldByHost = false
    private var longPressFired = false
    private var chromeHiddenAtDown = false

    /** TalkBack users read at their own pace — never auto-advance under touch exploration. */
    private var autoAdvance = true

    private var ctaBasePaddingBottom = 0
    private var topScrimBaseHeight = 0
    private var bottomScrimBaseHeight = 0
    private var onRetry: (() -> Unit)? = null
    private val showLoadingRunnable = Runnable { if (view != null) binding.loading.visible() }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            renderSlide(position)
        }
    }

    private val zoneTouchListener = View.OnTouchListener { _, ev ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressFired = false
                chromeHiddenAtDown = isChromeHidden
                isTouchHeld = true
                syncTimer()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouchHeld = false
                if (isChromeHidden) showChrome()
                syncTimer()
            }
        }
        false
    }

    override fun initialize() {
        chromeViews = listOf(
            binding.topScrim,
            binding.bottomScrim,
            binding.topChrome,
            binding.ctaContainer
        )
        ctaBasePaddingBottom = binding.ctaContainer.paddingBottom
        topScrimBaseHeight = binding.topScrim.layoutParams.height
        bottomScrimBaseHeight = binding.bottomScrim.layoutParams.height
        applyWindowInsets()

        val a11y = requireContext().getSystemService(AccessibilityManager::class.java)
        autoAdvance = a11y?.isTouchExplorationEnabled != true

        slideAdapter.onSlideImageReady = ::handleImageReady
        slideAdapter.onSlideImageError = ::handleImageError

        binding.pager.adapter = slideAdapter
        // Slide navigation is tap- and timer-driven; horizontal swipes belong to the outer (group) pager.
        binding.pager.isUserInputEnabled = false
        binding.pager.offscreenPageLimit = 1
        binding.pager.setPageTransformer(CrossfadePageTransformer())
        binding.pager.registerOnPageChangeCallback(pageChangeCallback)

        binding.btnCta.setOnClickListener {
            slides.getOrNull(currentIndex)?.let(::handleLink)
        }
        binding.btnClose.setOnClickListener { host?.closeViewer() }
        binding.btnRetry.setOnClickListener { onRetry?.invoke() }

        binding.leftZone.setOnTouchListener(zoneTouchListener)
        binding.rightZone.setOnTouchListener(zoneTouchListener)

        binding.leftZone.setOnClickListener {
            if (longPressFired || chromeHiddenAtDown) return@setOnClickListener
            goToSlide(currentIndex - 1)
        }
        binding.rightZone.setOnClickListener {
            if (longPressFired || chromeHiddenAtDown) return@setOnClickListener
            goToSlide(currentIndex + 1)
        }

        val longPressListener = View.OnLongClickListener {
            longPressFired = true
            hideChrome()
            true
        }
        binding.leftZone.setOnLongClickListener(longPressListener)
        binding.rightZone.setOnLongClickListener(longPressListener)

        loadStory()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.topChrome.updatePadding(top = bars.top)
            binding.ctaContainer.updatePadding(bottom = ctaBasePaddingBottom + bars.bottom)
            binding.topScrim.updateLayoutParams { height = topScrimBaseHeight + bars.top }
            binding.bottomScrim.updateLayoutParams { height = bottomScrimBaseHeight + bars.bottom }
            insets
        }
        // Pages are attached after the window's initial inset pass — ask for a fresh one.
        binding.root.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    override fun onResume() {
        super.onResume()
        // A host gesture can't outlive a page change; whatever held us before is over.
        isHeldByHost = false
        if (isEmptyGroup) {
            host?.goToNextGroup()
            return
        }
        if (!isLoaded || slides.isEmpty()) return
        host?.onGroupViewed(storyId)
        if (animator != null) {
            syncTimer()
            return
        }
        // No animator: either the image is still loading (handleImageReady will start it),
        // or this slide already finished once (we're being re-entered) — restart it.
        val idx = currentIndex.coerceIn(0, slides.lastIndex)
        if (slideAdapter.isImageLoaded(idx)) startSlideTimer(idx)
    }

    override fun onPause() {
        super.onPause()
        animator?.pause()
    }

    override fun onDestroyView() {
        binding.pager.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.loading.removeCallbacks(showLoadingRunnable)
        animator?.cancel()
        animator = null
        super.onDestroyView()
    }

    // ---- host hooks ---------------------------------------------------------------------

    /** Called by the activity while it owns the touch stream (group swipe / drag-to-dismiss). */
    fun setHeldByHost(held: Boolean) {
        if (isHeldByHost == held) return
        isHeldByHost = held
        syncTimer()
    }

    // ---- loading ------------------------------------------------------------------------

    private fun loadStory(refresh: Boolean = false) {
        storyJob?.cancel()
        isLoaded = false
        isEmptyGroup = false
        storyJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getStory(storyId, refresh).collectLatest(::collectStory)
        }
    }

    private fun collectStory(t: UIResource<StoryDetailData>) {
        when (t) {
            is UIResource.Loading -> showSlideLoading()
            is UIResource.Error -> showError(getString(DomainR.string.story_load_failed)) {
                loadStory(refresh = true)
            }

            is UIResource.Success -> onStoryLoaded(t.data)
        }
    }

    private fun onStoryLoaded(data: StoryDetailData?) {
        bindHeader(data?.title, data?.coverImageUrl)

        val items = data?.items.orEmpty().filter { !it.imageUrl.isNullOrBlank() }
        if (items.isEmpty()) {
            hideStates()
            isEmptyGroup = true
            if (isResumed) host?.goToNextGroup()
            return
        }

        slides = items
        slideAdapter.submitList(items)
        buildProgressBars(items.size)
        val startAt = currentIndex.coerceIn(0, items.lastIndex)
        binding.pager.setCurrentItem(startAt, false)
        isLoaded = true
        if (isResumed) host?.onGroupViewed(storyId)
        // Render unconditionally — the timer gates itself on isResumed, and a page that loads
        // while off-screen must still be fully drawn by the time it slides in.
        renderSlide(startAt)
    }

    private fun bindHeader(title: String?, coverUrl: String?) {
        val hasTitle = !title.isNullOrBlank()
        val hasCover = !coverUrl.isNullOrBlank()
        binding.groupTitle.text = title
        binding.groupTitle.isVisible = hasTitle
        binding.groupCoverCard.isVisible = hasCover
        if (hasCover) binding.groupCover.load(coverUrl)
    }

    private fun buildProgressBars(count: Int) {
        binding.progressContainer.removeAllViews()
        progressBars.clear()

        val density = resources.displayMetrics.density
        val height = (3f * density).toInt()
        val gap = (4f * density).toInt()

        for (i in 0 until count) {
            val track = FrameLayout(requireContext()).apply {
                background = ContextCompat.getDrawable(
                    requireContext(), R.drawable.bg_story_progress_track
                )
                clipChildren = true
            }
            val lp = LinearLayout.LayoutParams(0, height, 1f).apply {
                if (i > 0) marginStart = gap
            }
            track.layoutParams = lp

            val fill = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                background = ContextCompat.getDrawable(
                    requireContext(), R.drawable.bg_story_progress_fill
                )
                pivotX = 0f
                scaleX = 0f
            }
            track.addView(fill)
            binding.progressContainer.addView(track)
            progressBars.add(fill)
        }
    }

    // ---- navigation ---------------------------------------------------------------------

    private fun goToSlide(index: Int) {
        // Nothing to navigate yet — a tap while the detail is loading must not skip the group.
        if (!isLoaded || slides.isEmpty()) return
        when {
            index < 0 -> {
                // First slide of the first group: restart it instead of doing nothing.
                if (host?.goToPreviousGroup() != true) renderSlide(0)
            }

            index >= slides.size -> host?.goToNextGroup()
            index == currentIndex -> renderSlide(index)
            else -> binding.pager.setCurrentItem(index, true) // → onPageSelected → renderSlide
        }
    }

    private fun renderSlide(index: Int) {
        animator?.cancel()
        animator = null

        currentIndex = index

        progressBars.forEachIndexed { i, view ->
            view.scaleX = if (i < index) 1f else 0f
        }

        val slide = slides.getOrNull(index) ?: return

        val hasTitle = !slide.title.isNullOrBlank()
        val hasDescription = !slide.description.isNullOrBlank()
        binding.slideTitle.text = slide.title
        binding.slideTitle.isVisible = hasTitle
        binding.slideDescription.text = slide.description
        binding.slideDescription.isVisible = hasDescription

        val ctaLabel = ctaLabelFor(slide)
        binding.ctaText.text = ctaLabel
        binding.btnCta.isVisible = ctaLabel != null
        binding.bottomScrim.isVisible = hasTitle || hasDescription || ctaLabel != null

        // Timer is gated on the image — if it's already on screen, start now; otherwise the
        // adapter's load listener (handleImageReady / handleImageError) takes it from here.
        when {
            slideAdapter.isImageLoaded(index) -> {
                hideStates()
                startSlideTimer(index)
            }

            slideAdapter.isImageFailed(index) -> showImageError()
            else -> showSlideLoading()
        }
    }

    private fun ctaLabelFor(slide: StorySlideData): String? {
        val value = slide.linkValue?.takeIf { it.isNotBlank() } ?: return null
        return when (slide.linkType) {
            StoryLinkTypes.PARTNER ->
                if (value.toIntOrNull() != null) getString(DomainR.string.story_cta_open_partner) else null

            StoryLinkTypes.URL -> getString(DomainR.string.story_cta_learn_more)
            else -> null // DRINK: no standalone drink-detail screen yet
        }
    }

    private fun handleImageReady(position: Int) {
        if (position != currentIndex) return
        hideStates()
        if (isLoaded && animator == null) startSlideTimer(position)
    }

    private fun handleImageError(position: Int) {
        if (position != currentIndex) return
        animator?.cancel()
        animator = null
        showImageError()
    }

    private fun startSlideTimer(index: Int) {
        if (animator != null) return
        val slide = slides.getOrNull(index) ?: return
        val fillView = progressBars.getOrNull(index) ?: return
        if (!autoAdvance) {
            // Mark the current segment and leave advancing to the user's taps.
            fillView.scaleX = 1f
            return
        }
        val durationMs = ((slide.duration ?: 0) * 1000L).coerceAtLeast(MIN_SLIDE_DURATION_MS)
        animator = ObjectAnimator.ofFloat(fillView, View.SCALE_X, 0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (canceled) return
                    // Drop the finished animator so a later re-entry restarts this slide
                    // instead of resuming an animation that has nothing left to play.
                    animator = null
                    if (isResumed) goToSlide(currentIndex + 1)
                }
            })
            start()
            // Hidden / non-primary / held pages still get rendered but must not advance time.
            if (!shouldTimerRun) pause()
        }
    }

    /** The single source of truth for whether the slide timer may advance right now. */
    private val shouldTimerRun: Boolean
        get() = isResumed && !isTouchHeld && !isHeldByHost

    private fun syncTimer() {
        val a = animator ?: return
        if (shouldTimerRun) a.resume() else a.pause()
    }

    // ---- hold to peek -------------------------------------------------------------------

    private fun hideChrome() {
        if (isChromeHidden) return
        isChromeHidden = true
        chromeViews.forEach { v ->
            v.animate().alpha(0f).setDuration(CHROME_FADE_MS).start()
        }
    }

    private fun showChrome() {
        if (!isChromeHidden) return
        isChromeHidden = false
        chromeViews.forEach { v ->
            v.animate().alpha(1f).setDuration(CHROME_FADE_MS).start()
        }
    }

    // ---- loading / error states ---------------------------------------------------------

    private fun showSlideLoading() {
        binding.errorContainer.gone()
        // Delay a touch so cached / fast loads don't flash a spinner.
        binding.loading.removeCallbacks(showLoadingRunnable)
        binding.loading.postDelayed(showLoadingRunnable, LOADING_DELAY_MS)
    }

    private fun showImageError() {
        showError(getString(DomainR.string.story_image_load_failed)) {
            showSlideLoading()
            slideAdapter.reload(currentIndex)
        }
    }

    private fun showError(message: String, retry: () -> Unit) {
        binding.loading.removeCallbacks(showLoadingRunnable)
        binding.loading.gone()
        onRetry = retry
        binding.errorText.text = message
        binding.errorContainer.visible()
    }

    private fun hideStates() {
        binding.loading.removeCallbacks(showLoadingRunnable)
        binding.loading.gone()
        binding.errorContainer.gone()
        onRetry = null
    }

    // ---- links --------------------------------------------------------------------------

    private fun handleLink(slide: StorySlideData) {
        val type = slide.linkType ?: return
        val value = slide.linkValue?.takeIf { it.isNotBlank() } ?: return
        when (type) {
            StoryLinkTypes.URL -> requireActivity().intentToBrowser(value)
            StoryLinkTypes.PARTNER -> {
                // linkValue is a partner (brand) id, not a shop id. The partner screen lives
                // inside MainActivity, so the viewer can't show it itself — it closes and hands
                // the id back to whoever launched it.
                val partnerId = value.toIntOrNull() ?: return
                host?.openPartner(partnerId)
            }

            StoryLinkTypes.DRINK -> {
                // No standalone drink-detail screen yet — skip.
            }
        }
    }

    /** Keeps every slide stacked in place and fades between them instead of sliding. */
    private class CrossfadePageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            page.translationX = -position * page.width
            page.alpha = (1f - abs(position)).coerceIn(0f, 1f)
        }
    }

    companion object {
        private const val ARG_STORY_ID = "story_id"
        private const val MIN_SLIDE_DURATION_MS = 5_000L
        private const val CHROME_FADE_MS = 180L
        private const val LOADING_DELAY_MS = 250L

        fun newInstance(storyId: Int): StoryGroupFragment =
            StoryGroupFragment().apply {
                arguments = Bundle().apply { putInt(ARG_STORY_ID, storyId) }
            }
    }
}
