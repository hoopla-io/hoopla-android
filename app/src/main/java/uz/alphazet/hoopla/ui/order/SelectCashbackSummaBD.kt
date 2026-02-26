package uz.alphazet.hoopla.ui.order

import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogSelectCashbackSummaBinding

class SelectCashbackSummaBD(
    private val orderData: OrderDetails,
    private val userData: UserData?,
    private val maxLimitSumma: Double,
    private val usingCashbackSumma: Double,
    private val onSummaSelected: (Double) -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_select_cashback_summa) {

    private val binding by viewBinding(DialogSelectCashbackSummaBinding::bind)
    private var currentSumma = 0.0
    private var repeatJob: Job? = null

    override fun initialize() {
        binding.cashbackBalance.text = userData?.balance?.formatToPrice().plus(" UZS")
        binding.description.text = getString(
            uz.alphazet.domain.R.string.label_use_cashback_description,
            userData?.balance?.formatToPrice()
        )

        currentSumma = if (usingCashbackSumma <= 0.0) maxLimitSumma
        else usingCashbackSumma

        if (currentSumma > (userData?.balance ?: 0.0)) {
            currentSumma = userData?.balance ?: 0.0
        }
        binding.currentSumma.text = currentSumma.formatToPrice().plus(" UZS")

        binding.plus.setOnClickListener {
            val result = plus()
            currentSumma = result
            binding.currentSumma.text = currentSumma.formatToPrice().plus(" UZS")
        }

        binding.plus.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    repeatJob = lifecycleScope.launch {
                        while (isActive) {
                            val result = plus()
                            currentSumma = result
                            binding.currentSumma.text = currentSumma.formatToPrice().plus(" UZS")
                            delay(300)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    repeatJob?.cancel()
                    true
                }

                else -> false
            }
        }

        binding.minus.setOnClickListener {
            val result = minus()
            currentSumma = result
            binding.currentSumma.text = currentSumma.formatToPrice().plus(" UZS")
        }

        binding.minus.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    repeatJob = lifecycleScope.launch {
                        while (isActive) {
                            val result = minus()
                            currentSumma = result
                            binding.currentSumma.text = currentSumma.formatToPrice().plus(" UZS")
                            delay(300)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    repeatJob?.cancel()
                    true
                }

                else -> false
            }
        }

        binding.use.setOnClickListener {
            onSummaSelected.invoke(currentSumma)
            dismiss()
        }

        binding.reset.setOnClickListener {
            currentSumma = 0.0
            onSummaSelected.invoke(currentSumma)
            dismiss()
        }

    }

    private fun plus(): Double {
        val cashBalance = userData?.balance ?: 0.0
        var result = currentSumma

        if (result < maxLimitSumma) {
            result += 1000
        }

        if (result > maxLimitSumma) {
            result = maxLimitSumma
        }
        if (result > cashBalance) {
            result = cashBalance
        }

        return result
    }

    private fun minus(): Double {
        val cashBalance = userData?.balance ?: 0.0
        var result = currentSumma

        if (result == cashBalance || result == maxLimitSumma) {
            if (result % 1000 != 0.0) {
                result -= result % 1000
            } else {
                result -= 1000
            }
        } else {
            result -= 1000
        }

        if (result < 0) result = 0.0

        return result
    }

    companion object {
        private const val TAG = "SelectCashbackSummaBD"

        fun BaseActivity.showSelectCashbackSummaBD(
            orderData: OrderDetails,
            userData: UserData?,
            maxLimitSumma: Double,
            usingCashbackSumma: Double,
            onSummaSelected: (Double) -> Unit = {}
        ) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                SelectCashbackSummaBD(
                    orderData,
                    userData,
                    maxLimitSumma,
                    usingCashbackSumma,
                    onSummaSelected
                ).show(
                    supportFragmentManager,
                    TAG
                )
            }
        }
    }

}