package uz.alphazet.domain.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import uz.alphazet.domain.R
import uz.alphazet.domain.utils.log

class PillSizeSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val rootContainer: FrameLayout
    private val optionsContainer: LinearLayout
    private val indicatorCard: CardView
    private val textSelectedLabel: TextView
    private val textSelectedVolume: TextView

    private var options: List<CoffeeSize> = emptyList()
    private var selectedOption: CoffeeSize? = null
    private var onOptionSelectedListener: ((CoffeeSize) -> Unit)? = null

    // Attributes
    private var pillBackgroundColor: Int = Color.WHITE
    private var pillIndicatorColor: Int = Color.WHITE
    private var pillTextColor: Int = Color.GRAY
    private var pillSelectedTextColor: Int = Color.BLACK // Default, will change
    private var pillCornerRadius: Float = 0f

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_pill_selector, this, true)
        rootContainer = view.findViewById(R.id.rootContainer)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        indicatorCard = view.findViewById(R.id.indicatorCard)
        textSelectedLabel = view.findViewById(R.id.textSelectedLabel)
        textSelectedVolume = view.findViewById(R.id.textSelectedVolume)

        // Read attributes
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PillSizeSelectorView,
            0, 0
        ).apply {
            try {
                pillBackgroundColor =
                    getColor(R.styleable.PillSizeSelectorView_pillBackgroundColor, Color.WHITE)
                pillIndicatorColor =
                    getColor(R.styleable.PillSizeSelectorView_pillIndicatorColor, Color.WHITE)
                pillTextColor = getColor(R.styleable.PillSizeSelectorView_pillTextColor, Color.GRAY)
                pillSelectedTextColor = getColor(
                    R.styleable.PillSizeSelectorView_pillSelectedTextColor,
                    Color.parseColor("#D79965")
                )
                pillCornerRadius = getDimension(
                    R.styleable.PillSizeSelectorView_pillCornerRadius,
                    120f
                ) // 40dp approx
            } finally {
                recycle()
            }
        }

        applyAttributes()
    }

    private fun applyAttributes() {
        // Apply background color implementation would need a simpler way for FrameLayout shape
        // For now, relying on the generic drawable, but we could tint it.
        // rootContainer.background.setTint(pillBackgroundColor) // API 21+

        indicatorCard.setCardBackgroundColor(pillIndicatorColor)
        textSelectedLabel.setTextColor(pillSelectedTextColor)
        // Volume text color could be derived or fixed
    }

    fun setOptions(newOptions: List<CoffeeSize>, defaultSelection: CoffeeSize? = null) {
        this.options = newOptions
        optionsContainer.removeAllViews()
        optionsContainer.weightSum = newOptions.size.toFloat()

        newOptions.forEach { size ->
            val itemView = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                isClickable = true
                setOnClickListener { selectOption(size) }
            }

            val textView = TextView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                text = size.label
                textSize = 16f
                setTextColor(pillTextColor)
                typeface = Typeface.DEFAULT_BOLD
            }

            itemView.addView(textView)
            optionsContainer.addView(itemView)
        }

        val selection = defaultSelection ?: newOptions.firstOrNull()
        if (selection != null) {
            // Post to wait for layout to have dimensions
            post {
                selectOption(selection, animate = false)
            }
        }
    }

    fun setOnOptionSelectedListener(listener: (CoffeeSize) -> Unit) {
        this.onOptionSelectedListener = listener
    }

    private fun selectOption(option: CoffeeSize, animate: Boolean = true) {
        if (selectedOption == option && animate) return
        selectedOption = option

        val index = options.indexOf(option)
        if (index == -1) return

        // Update selected text
        textSelectedLabel.text = option.label
        textSelectedVolume.text = "${option.volume}ml"

        // Calculate position
        val segmentWidth =
            (width - rootContainer.paddingStart - rootContainer.paddingEnd) / options.size
        val targetX = (segmentWidth * index).toFloat() + rootContainer.paddingStart

        // Animate
        if (animate) {
            indicatorCard.animate()
                .translationX(targetX)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.2f)) // Mimic spring-like bounce
                .start()
        } else {
            indicatorCard.translationX = targetX
        }

        onOptionSelectedListener?.invoke(option)
    }

    // Handle resizing to adjust indicator width
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (options.isNotEmpty()) {
            val segmentWidth = w / options.size

            // Re-position if needed
            selectedOption?.let {
                val index = options.indexOf(it)
                indicatorCard.translationX = (segmentWidth * index).toFloat()
            }
        }
    }
}
