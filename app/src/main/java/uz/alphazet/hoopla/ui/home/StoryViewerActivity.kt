package uz.alphazet.hoopla.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.StoryDetailData
import uz.alphazet.data.models.StoryLinkTypes
import uz.alphazet.data.models.StorySlideData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ActivityStoryViewerBinding
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity

class StoryViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityStoryViewerBinding
    private val viewModel: StoryViewerVM by viewModel()

    private val storyId by lazy { intent.getIntExtra(STORY_ID, -1) }

    private var slides: List<StorySlideData> = emptyList()
    private var currentIndex = 0
    private val progressBars = mutableListOf<View>()
    private var animator: ObjectAnimator? = null

    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private var pauseRunnable: Runnable? = null
    private var isHoldingPaused = false
    private var touchDownX = 0f
    private var touchDownY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (storyId <= 0) {
            finish()
            return
        }

        binding.btnClose.setOnClickListener { finish() }
        binding.tapPrev.setOnClickListener { showSlide(currentIndex - 1) }
        binding.tapNext.setOnClickListener { showSlide(currentIndex + 1) }
        binding.ctaContainer.setOnClickListener {
            slides.getOrNull(currentIndex)?.let(::handleLink)
        }
        binding.tapPrev.setOnTouchListener(holdToPauseListener)
        binding.tapNext.setOnTouchListener(holdToPauseListener)

        launch {
            viewModel.getStory(storyId).collectLatest(::collectStory)
        }
    }

    override fun updateStatusBarViewHeight() {}

    private fun collectStory(t: UIResource<StoryDetailData>) = t.collect { data ->
        val items = data?.items.orEmpty()
        if (items.isEmpty()) {
            finish()
            return@collect
        }
        slides = items
        currentIndex = 0
        buildProgressBars(items.size)
        showSlide(0)
    }

    private fun buildProgressBars(count: Int) {
        binding.progressContainer.removeAllViews()
        progressBars.clear()

        val density = resources.displayMetrics.density
        val height = (3f * density).toInt()
        val gap = (4f * density).toInt()

        for (i in 0 until count) {
            val track = FrameLayout(this).apply {
                background = ContextCompat.getDrawable(
                    this@StoryViewerActivity, R.drawable.bg_story_progress_track
                )
                clipChildren = true
            }
            val lp = LinearLayout.LayoutParams(0, height, 1f).apply {
                if (i > 0) marginStart = gap
            }
            track.layoutParams = lp

            val fill = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                background = ContextCompat.getDrawable(
                    this@StoryViewerActivity, R.drawable.bg_story_progress_fill
                )
                pivotX = 0f
                scaleX = 0f
            }
            track.addView(fill)
            binding.progressContainer.addView(track)
            progressBars.add(fill)
        }
    }

    private fun showSlide(index: Int) {
        if (index < 0) return
        if (index >= slides.size) {
            finish()
            return
        }

        animator?.cancel()
        animator = null

        currentIndex = index

        progressBars.forEachIndexed { i, view ->
            view.scaleX = when {
                i < index -> 1f
                i > index -> 0f
                else -> 0f
            }
        }

        val slide = slides[index]
        binding.slideImage.load(slide.imageUrl)

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

        val durationMs = ((slide.duration ?: DEFAULT_DURATION_SECONDS) * 1000L)
            .coerceAtLeast(MIN_DURATION_MS)
        val fillView = progressBars.getOrNull(index) ?: return
        animator = ObjectAnimator.ofFloat(fillView, "scaleX", 0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing && animation == animator) {
                        showSlide(currentIndex + 1)
                    }
                }
            })
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val holdToPauseListener = View.OnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isHoldingPaused = false
                val r = Runnable {
                    animator?.pause()
                    isHoldingPaused = true
                }
                pauseRunnable = r
                v.postDelayed(r, longPressTimeout)
                false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY
                if (dx * dx + dy * dy > touchSlop * touchSlop) {
                    pauseRunnable?.let { v.removeCallbacks(it) }
                    pauseRunnable = null
                }
                false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pauseRunnable?.let { v.removeCallbacks(it) }
                pauseRunnable = null
                if (isHoldingPaused) {
                    isHoldingPaused = false
                    animator?.resume()
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }

    private fun handleLink(slide: StorySlideData) {
        val type = slide.linkType ?: return
        val value = slide.linkValue ?: return
        when (type) {
            StoryLinkTypes.URL -> intentToBrowser(value)
            StoryLinkTypes.PARTNER -> {
                val partnerId = value.toIntOrNull() ?: return
                val intent = Intent(this, ShopDetailActivity::class.java)
                intent.putExtra(ShopDetailActivity.SHOP_ID, partnerId)
                startActivity(intent)
            }

            StoryLinkTypes.DRINK -> {
                // No standalone drink-detail screen yet — skip.
            }
        }
    }

    override fun onPause() {
        super.onPause()
        animator?.pause()
    }

    override fun onResume() {
        super.onResume()
        animator?.resume()
    }

    override fun onDestroy() {
        animator?.cancel()
        animator = null
        super.onDestroy()
    }

    companion object {
        const val STORY_ID = "story_id"
        private const val DEFAULT_DURATION_SECONDS = 5
        private const val MIN_DURATION_MS = 1500L
    }
}