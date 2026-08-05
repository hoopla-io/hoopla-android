package uz.alphazet.domain.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import uz.alphazet.domain.R
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
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
import uz.alphazet.domain.utils.log
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseActivity : AppCompatActivity(), RemoteErrorListener {

    private val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()

    override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeDelegate.onCreate(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightNavigationBars = false

        setupSystemBarInsets()
    }

    private fun setupSystemBarInsets() {
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            content.findViewById<View>(R.id.status_bar_view)?.let { statusBar ->
                val lp = statusBar.layoutParams
                if (lp.height != systemBars.top) {
                    lp.height = systemBars.top
                    statusBar.layoutParams = lp
                }
            }

            onApplySystemBarInsets(systemBars)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    open fun onApplySystemBarInsets(systemBars: Insets) {}

    open fun updateStatusBarViewHeight() {}

    suspend fun getStatusBarHeight(): Int {
        val content = findViewById<View>(android.R.id.content) ?: return 0
        ViewCompat.getRootWindowInsets(content)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
            ?.let { return it }

        val deferred = CompletableDeferred<Int>()
        content.doOnLayout {
            val top = ViewCompat.getRootWindowInsets(content)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            deferred.complete(top)
        }
        return deferred.await()
    }

    override fun onResume() {
        super.onResume()
        localeDelegate.onResumed(this)
    }

    override fun onPause() {
        super.onPause()
        localeDelegate.onPaused()
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        localeDelegate.setLocale(this, locale)
    }

    protected fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return lifecycleScope.launch(context, start, block)
    }

    fun <T> UIResource<T>.collect(
        onLoading: ((Boolean) -> Unit)? = { isLoading = it },
        onError: ((Throwable) -> Unit)? = null,
        onChanged: (T?) -> Unit = {}
    ) {
        onLoading?.invoke(this == UIResource.Loading)
        when (this) {
            is UIResource.Success<T> -> onChanged.invoke(this.data)
            is UIResource.Error -> {
                if (onError == null) checkErrors(this.throwable) else onError.invoke(this.throwable)
            }

            else -> Unit
        }
    }

    /**
     * The default error routing. Exposed so a screen that needs to do something of its own on
     * failure — re-arm a button, drop a spinner — can still hand the throwable on to the typed
     * `onXxxException` handlers instead of having to re-implement this dispatch.
     */
    protected fun checkErrors(throwable: Throwable) {
        throwable.log("REMOTE_ERROR")
        when (throwable) {
            is UnauthorizedException -> onUnauthorizedException(throwable.message, throwable.code)
            is PaymentException -> onPaymentException(
                throwable.errorData,
                throwable.message,
                throwable.code
            )

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

    override fun onPaymentException(
        errorData: PaymentRequiredExceptionData?,
        message: String?,
        code: Int
    ) {
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

    protected fun showErrorMessage(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

}