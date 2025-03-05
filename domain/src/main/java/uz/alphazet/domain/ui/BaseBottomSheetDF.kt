package uz.alphazet.domain.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uz.alphazet.data.UIResource
import uz.alphazet.domain.R
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.network.BadRequestException
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.network.ForbiddenException
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.RemoteErrorListener
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.network.TooManyRequestException
import uz.alphazet.domain.network.UnauthorizedException
import uz.alphazet.domain.network.ValidationException
import uz.alphazet.domain.utils.hideKeyboard
import uz.alphazet.domain.utils.log
import uz.alphazet.domain.utils.showKeyboard
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


abstract class BaseBottomSheetDF(
    @LayoutRes val layoutId: Int
) : BottomSheetDialogFragment(), View.OnClickListener, RemoteErrorListener {

    protected open val screenSize: SheetSizes? = SheetSizes.NONE

    private lateinit var notAttachedContext: Context

    val cache: AppCache by inject()

    open var forceKeyboard = false

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onClick(view: View) {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notAttachedContext = requireActivity()

        dialog?.setOnShowListener {
            notAttachedContext.applicationContext
            when (screenSize) {
                SheetSizes.FULLSCREEN -> {
                    setupFullHeight(notAttachedContext, it as BottomSheetDialog)
                }

                SheetSizes.HALF -> {
                    setupHalfHeight(notAttachedContext, it as BottomSheetDialog)
                }

                SheetSizes.DEFAULT -> {
                    setupDefaultHeight(it as BottomSheetDialog)
                }

                SheetSizes.NONE -> {
                    setupNullable(it as BottomSheetDialog)
                }

                else -> Unit
            }

            if (!forceKeyboard)
                hideKeyboard()
            else
                requireContext().showKeyboard()

            initialize()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (!this@BaseBottomSheetDF.onBackPressed()) super.onBackPressed()
            }
        }
    }

    open fun onBackPressed(): Boolean = false

    protected fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = lifecycleScope.launch(context, start, block)

    fun <T> UIResource<T>.collect(
        onLoading: ((Boolean) -> Unit)? = { isLoading = it },
        onError: ((Throwable) -> Unit)? = null,
        onChanged: (T?) -> Unit = {}
    ) {
        this.log("COLLECT_REMOTE")
        onLoading?.invoke(this == UIResource.Loading)
        when (this) {
            is UIResource.Success<T> -> onChanged.invoke(this.data)
            is UIResource.Error -> {
                if (onError == null) checkErrors(this.throwable) else onError.invoke(this.throwable)
            }

            else -> Unit
        }
    }

    protected fun showErrorMessage(message: String?) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    private fun checkErrors(throwable: Throwable) {
        throwable.log("REMOTE_ERROR")
        when (throwable) {
            is UnauthorizedException -> onUnauthorizedException(throwable.message, throwable.code)
            is PaymentException -> onPaymentException(throwable.message, throwable.code)
            is ValidationException -> onValidationException(throwable.message, throwable.code)
            is BadRequestException -> onBadRequestException(throwable.message, throwable.code)
            is ForbiddenException -> onForbiddenException(throwable.message, throwable.code)
            is ConflictException -> onConflictException(throwable.message, throwable.code)
            is NotFoundException -> onNotFoundException(throwable.message, throwable.code)
            is TooManyRequestException -> onTooManyRequestException(
                throwable.message,
                throwable.code
            )

            is RemoteException -> onRemoteException(throwable.message, throwable.code)
        }
    }

    override fun onRemoteException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onUnauthorizedException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onPaymentException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onValidationException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onBadRequestException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onForbiddenException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onConflictException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onNotFoundException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onTooManyRequestException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onServerErrorException(message: String?, code: Int) {
        showErrorMessage(message)
    }

    override fun onConnectionErrorException(message: String?) {
        showErrorMessage(message)
    }

    private var isLoading: Boolean = false
        set(value) {
            if (value) showLoading() else hideLoading()
            field = value
        }

    open fun showLoading() {
//        showLoadingDialog()
    }

    open fun hideLoading() {
//        hideLoadingDialog()
    }

    private fun setupNullable(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams

        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupFullHeight(context: Context, bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams
        val windowHeight = getWindowHeight(context)

        if (windowHeight == 0)
            setupDefaultHeight(bottomSheetDialog)
        else {
            if (layoutParams != null) {
                layoutParams.height = windowHeight
            }
            bottomSheet.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun setupDefaultHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams
        val windowHeight = bottomSheet.height
        if (layoutParams != null) {
            layoutParams.height = windowHeight
        }
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupHalfHeight(context: Context, bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
        val layoutParams = bottomSheet.layoutParams
        val windowHeight = getWindowHeight(context) / 2

        if (windowHeight == 0) {
            setupDefaultHeight(bottomSheetDialog)
        } else {
            if (layoutParams != null) {
                layoutParams.height = windowHeight
            }
            bottomSheet.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getWindowHeight(context: Context): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (context as Activity?)!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    abstract fun initialize()

    enum class SheetSizes {
        FULLSCREEN, HALF, DEFAULT, NONE
    }
}