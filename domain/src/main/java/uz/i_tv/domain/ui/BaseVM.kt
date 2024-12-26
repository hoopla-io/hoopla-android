package uz.i_tv.domain.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import uz.i_tv.data.AnyResource
import uz.i_tv.data.UIResource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class BaseVM : ViewModel() {

    protected val anyResourceEmitter: MutableSharedFlow<AnyResource> = MutableSharedFlow()
    val anyResourceFlow: SharedFlow<AnyResource> get() = anyResourceEmitter

    fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, start, block)
    }


}

suspend fun <T> MutableSharedFlow<UIResource<T>>.load(value: UIResource<T>) {
    emit(UIResource.Loading)
    emit(value)
}

suspend fun <T> MutableSharedFlow<UIResource<T>>.load(callback: suspend () -> UIResource<T>) {
    emit(UIResource.Loading)
    emit(callback())
}

suspend fun <T> MutableSharedFlow<UIResource<T>>.emit(callback: suspend () -> UIResource<T>) {
    emit(callback())
}

