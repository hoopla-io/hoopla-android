package uz.alphazet.hoopla.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import coil3.load
import kotlinx.coroutines.flow.collectLatest
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
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity

class StoryGroupFragment : BaseFragment(R.layout.screen_story_group) {

    interface Host {
        fun goToNextGroup()
        fun goToPreviousGroup()
    }

    private val binding by viewBinding(ScreenStoryGroupBinding::bind)
    private val viewModel: StoryViewerVM by viewModel(ownerProducer = { requireActivity() })

    private val storyId: Int by lazy { requireArguments().getInt(ARG_STORY_ID) }

    private val slideAdapter = StorySlideAdapter()
    private var slides: List<StorySlideData> = emptyList()
    private var currentIndex = 0
    private val progressBars = mutableListOf<View>()
    private var animator: ObjectAnimator? = null
    private var isLoaded = false

    private var chromeViews: List<View> = emptyList()
    private var isChromeHidden = false
    private var longPressFired = false
    private var chromeHiddenAtDown = false

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
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isChromeHidden) resumeAndShowChrome()
            }
        }
        false
    }

    override fun initialize() {
        chromeViews = listOf(
            binding.topScrim,
            binding.topChrome,
            binding.ctaContainer
        )

        slideAdapter.onSlideImageReady = ::handleImageReady
        slideAdapter.onSlideImageError = ::handleImageError

        binding.pager.adapter = slideAdapter
        // Slide navigation is tap- and timer-driven; horizontal swipes belong to the outer (group) pager.
        binding.pager.isUserInputEnabled = false
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(pageChangeCallback)

        binding.ctaContainer.setOnClickListener {
            slides.getOrNull(currentIndex)?.let(::handleLink)
        }

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
            pauseAndHideChrome()
            true
        }
        binding.leftZone.setOnLongClickListener(longPressListener)
        binding.rightZone.setOnLongClickListener(longPressListener)

        launch {
            viewModel.getStory(storyId).collectLatest(::collectStory)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLoaded || slides.isEmpty()) return
        val a = animator
        if (a != null) {
            if (!isChromeHidden) a.resume()
            return
        }
        // No animator yet — either we just landed on this slide, or its image
        // hadn't loaded when renderSlide ran. Start now if the image is ready.
        val idx = currentIndex.coerceIn(0, slides.lastIndex)
        if (slideAdapter.isImageLoaded(idx)) startSlideTimer(idx)
    }

    override fun onPause() {
        super.onPause()
        animator?.pause()
    }

    override fun onDestroyView() {
        binding.pager.unregisterOnPageChangeCallback(pageChangeCallback)
        animator?.cancel()
        animator = null
        super.onDestroyView()
    }

    private fun collectStory(t: UIResource<StoryDetailData>) = t.collect { data ->
        if (data == null) return@collect
        bindHeader(data.title, data.coverImageUrl)

        val items = data.items.orEmpty()
        if (items.isEmpty()) {
            (activity as? Host)?.goToNextGroup()
            return@collect
        }

        slides = items
        slideAdapter.submitList(items)
        buildProgressBars(items.size)
        val startAt = currentIndex.coerceIn(0, items.lastIndex)
        binding.pager.setCurrentItem(startAt, false)
        isLoaded = true
        if (isResumed) renderSlide(startAt)
    }

    private fun bindHeader(title: String?, coverUrl: String?) {
        val hasTitle = !title.isNullOrBlank()
        val hasCover = !coverUrl.isNullOrBlank()
        if (!hasTitle && !hasCover) {
            binding.headerContainer.gone()
            return
        }
        binding.headerContainer.visible()
        if (hasTitle) {
            binding.groupTitle.text = title
            binding.groupTitle.visible()
        } else {
            binding.groupTitle.gone()
        }
        if (hasCover) {
            binding.groupCoverCard.visible()
            binding.groupCover.load(coverUrl)
        } else {
            binding.groupCoverCard.gone()
        }
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

    private fun goToSlide(index: Int) {
        if (index < 0) {
            (activity as? Host)?.goToPreviousGroup()
            return
        }
        if (index >= slides.size) {
            (activity as? Host)?.goToNextGroup()
            return
        }
        binding.pager.setCurrentItem(index, true)
    }

    private fun renderSlide(index: Int) {
        animator?.cancel()
        animator = null

        currentIndex = index

        progressBars.forEachIndexed { i, view ->
            view.scaleX = if (i < index) 1f else 0f
        }

        val slide = slides.getOrNull(index) ?: return

        if (!slide.title.isNullOrBlank()) {
            binding.slideTitle.text = slide.title
            binding.slideTitle.visible()
        } else {
            binding.slideTitle.gone()
        }
        if (!slide.description.isNullOrBlank()) {
            binding.slideDescription.text = slide.description
            binding.slideDescription.visible()
        } else {
            binding.slideDescription.gone()
        }
        binding.ctaContainer.isClickable = slide.linkType != null && slide.linkValue != null

        // Timer is gated on the image — if it's already in cache, start now;
        // otherwise handleImageReady will fire when Coil resolves the load.
        if (slideAdapter.isImageLoaded(index)) {
            startSlideTimer(index)
        }
    }

    private fun handleImageReady(position: Int) {
        if (position != currentIndex) return
        if (animator != null) return
        if (progressBars.isEmpty()) return  // renderSlide will start the timer
        startSlideTimer(position)
    }

    private fun handleImageError(position: Int) {
        if (position != currentIndex) return
        if (isResumed) goToSlide(currentIndex + 1)
    }

    private fun startSlideTimer(index: Int) {
        if (animator != null) return
        val slide = slides.getOrNull(index) ?: return
        val durationMs = ((slide.duration ?: 0) * 1000L).coerceAtLeast(MIN_SLIDE_DURATION_MS)
        val fillView = progressBars.getOrNull(index) ?: return
        animator = ObjectAnimator.ofFloat(fillView, "scaleX", 0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (canceled) return
                    if (isResumed) goToSlide(currentIndex + 1)
                }
            })
            start()
            // Hidden / non-primary pages still get rendered but must not advance time.
            if (!isResumed || isChromeHidden) pause()
        }
    }

    private fun pauseAndHideChrome() {
        if (isChromeHidden) return
        isChromeHidden = true
        animator?.pause()
        chromeViews.forEach { v ->
            v.animate().alpha(0f).setDuration(CHROME_FADE_MS).start()
        }
    }

    private fun resumeAndShowChrome() {
        if (!isChromeHidden) return
        isChromeHidden = false
        chromeViews.forEach { v ->
            v.animate().alpha(1f).setDuration(CHROME_FADE_MS).start()
        }
        if (isResumed) animator?.resume()
    }

    private fun handleLink(slide: StorySlideData) {
        val type = slide.linkType ?: return
        val value = slide.linkValue ?: return
        when (type) {
            StoryLinkTypes.URL -> requireActivity().intentToBrowser(value)
            StoryLinkTypes.PARTNER -> {
                val partnerId = value.toIntOrNull() ?: return
                val intent = Intent(requireContext(), ShopDetailActivity::class.java)
                intent.putExtra(ShopDetailActivity.SHOP_ID, partnerId)
                startActivity(intent)
            }

            StoryLinkTypes.DRINK -> {
                // No standalone drink-detail screen yet — skip.
            }
        }
    }

    override fun onUnauthorizedException(message: String?, code: Int) {}

    companion object {
        private const val ARG_STORY_ID = "story_id"
        private const val MIN_SLIDE_DURATION_MS = 5_000L
        private const val CHROME_FADE_MS = 180L

        fun newInstance(storyId: Int): StoryGroupFragment =
            StoryGroupFragment().apply {
                arguments = Bundle().apply { putInt(ARG_STORY_ID, storyId) }
            }
    }
}