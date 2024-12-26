package uz.i_tv.domain.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uz.i_tv.data.UIResource
import uz.i_tv.domain.network.BadRequestException
import uz.i_tv.domain.network.ConflictException
import uz.i_tv.domain.network.ForbiddenException
import uz.i_tv.domain.network.NotFoundException
import uz.i_tv.domain.network.PaymentException
import uz.i_tv.domain.network.RemoteErrorListener
import uz.i_tv.domain.network.RemoteException
import uz.i_tv.domain.network.TooManyRequestException
import uz.i_tv.domain.network.UnauthorizedException
import uz.i_tv.domain.network.ValidationException
import uz.i_tv.domain.utils.log
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseFragment : Fragment, View.OnClickListener, RemoteErrorListener {

    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            runId = savedInstanceState.getString(FIRST_RUN_ID)!!

            val appId = requireContext().applicationContext.hashCode().toString()
            val savedAppId = savedInstanceState.getString(APP_RUN_ID)

            createMode = if (appId != savedAppId) {
                CreateMode.RESTORED_AFTER_DEATH
            } else {
                CreateMode.RESTORED_AFTER_ROTATION
            }
        } else {
            runId = "${javaClass.simpleName}[${hashCode()}]"
            createMode = CreateMode.NEW
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()
        initialize()
        requireView().setOnClickListener { hideKeyboard() }
    }

    override fun onStart() {
        super.onStart()
        instanceStateSaved = false
    }

    override fun onResume() {
        super.onResume()
        instanceStateSaved = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(APP_RUN_ID, requireContext().applicationContext.hashCode().toString())
        outState.putString(FIRST_RUN_ID, runId)
        instanceStateSaved = true
    }

    override fun onDestroy() {
        super.onDestroy()
        hideKeyboard()
        if (isRealDestroy()) onRealDestroy()
    }

    private fun isRealRemoving(): Boolean =
        (isRemoving && !instanceStateSaved) ||
                ((parentFragment as? BaseFragment)?.isRealRemoving() ?: false)

    private fun isRealDestroy(): Boolean = when {
        activity?.isChangingConfigurations == true -> false
        activity?.isFinishing == true -> true
        else -> isRealRemoving()
    }

    abstract fun initialize()

    protected open fun onRealDestroy() {}

    override fun onClick(view: View) {}

    protected fun hideKeyboard() {
        val manager: InputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (view != null)
            manager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

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
    }

    open fun hideLoading() {
    }

    private var instanceStateSaved: Boolean = false
    protected lateinit var runId: String
        private set

    protected lateinit var createMode: CreateMode
        private set


    companion object {
        private const val FIRST_RUN_ID = "STATE_FIRST_RUN_ID"
        private const val APP_RUN_ID = "STATE_APP_RUN_ID"
    }
}

enum class CreateMode {
    NEW, RESTORED_AFTER_ROTATION, RESTORED_AFTER_DEATH
}