package uz.alphazet.data

sealed class UIResource<out T> {

    class Success<T>(val data: T?) : UIResource<T>()
    data class Error(val throwable: Throwable): UIResource<Nothing>()
    object Loading : UIResource<Nothing>()

}

typealias AnyResource = UIResource<Any>
