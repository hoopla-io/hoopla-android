package uz.alphazet.domain.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uz.alphazet.data.UIResource
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.network.BadRequestException
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.network.ForbiddenException
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.PreconditionRequiredException
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

abstract class BaseFragment : Fragment, View.OnClickListener, RemoteErrorListener {

    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    val cache: AppCache by inject()

    open var forceKeyboard = false

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
        if (!forceKeyboard)
            hideKeyboard()
        else
            requireContext().showKeyboard()

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

    protected fun navigateTo(screen: Screen) = router.navigateTo(screen)
    protected fun replaceScreen(screen: Screen) = router.replaceScreen(screen)
    protected fun newRootScreen(screen: Screen) = router.newRootScreen(screen)
    protected fun backTo(screen: Screen?) = router.backTo(screen)
    protected fun exit() = router.exit()

    abstract fun initialize()

    open fun onBackPressed(): Boolean = false

    protected open fun onRealDestroy() {}

    override fun onClick(view: View) {}

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
            is PreconditionRequiredException -> onPreconditionRequiredException(
                throwable.message,
                throwable.code
            )

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

    override fun onPreconditionRequiredException(message: String?, code: Int) {
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

val BaseFragment.router: Router get() = findParentRouter()

fun BaseFragment.findParentRouter(): Router {
    val parentActivity = activity as? BaseRootActivity
    if (parentActivity != null) {
        return parentActivity.router
    } else throw NullPointerException("router not found in parent activity or parent fragments")
}

enum class CreateMode {
    NEW, RESTORED_AFTER_ROTATION, RESTORED_AFTER_DEATH
}