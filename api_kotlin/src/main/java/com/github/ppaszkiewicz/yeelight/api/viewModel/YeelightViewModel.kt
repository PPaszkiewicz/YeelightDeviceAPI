package com.github.ppaszkiewicz.yeelight.api.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.ppaszkiewicz.yeelight.api.YeelightCache
import com.github.ppaszkiewicz.yeelight.api.connection.YeelightAutoConnectionKt
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnectionPool

/**
 * Viewmodel hosting yeelight device livedata.
 */
abstract class YeelightViewModel : ViewModel() {
    companion object {
        const val TAG = "YeelightViewModel"
        /** Factory for ViewModel with custom connection provider. */
        fun customFactory(connectionPool: YeelightConnectionPool<*>, invalidateOnUpdates: Boolean = true, cache: YeelightCache? = null)
                : ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return YeelightCustomViewModel(connectionPool, invalidateOnUpdates, cache) as T
                }
            }
        }

        /** Factory for [YeelightActiveViewModel] that can optionally not deploy updates. */
        fun customActiveFactory(invalidateOnUpdates: Boolean = true, cache: YeelightCache? = null)
                : ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return YeelightActiveViewModel(invalidateOnUpdates, cache) as T
                }
            }
        }

        /**
         * Builds [YeelightActiveViewModel] without cache. This will keep connections alive as long as there are
         * any active LiveData observers.
         * */
        val Active
            get() = customActiveFactory(true)

        /**
         * Builds [YeelightCustomViewModel] based on [YeelightAutoConnectionKt] without cache.
         * This will only keep connections alive for a brief period after sending data to each device.
         * */
        val Auto
            get() = customFactory(YeelightAutoConnectionKt.PoolProvider(), true)

        /**
         * Builds [YeelightActiveViewModel] with provided [cache]. This will keep connections alive as long as there are
         * any active LiveData observers.
         * */
        fun Active(cache: YeelightCache) = customActiveFactory(true, cache)

        /**
         * Builds [YeelightCustomViewModel] based on [YeelightAutoConnectionKt] with provided [cache].
         * This will only keep connections alive for a brief period after sending data to each device.
         * */
        fun Auto(cache: YeelightCache) = customFactory(YeelightAutoConnectionKt.PoolProvider(), true, cache)
    }

    /** LiveData of devices. */
    abstract val devices: YeelightLiveData

    /**
     * Shorthand for obtaining list of devices from [devices] LiveData.
     * */
    val deviceList: Collection<YeelightDevice>?
        get() = devices.value?.values

    override fun onCleared() {
        super.onCleared()
        devices.onCleared()
    }
}

/** ViewModel with custom connection provider. */
open class YeelightCustomViewModel(
        connectionPool: YeelightConnectionPool<*>? = null,
        invalidateOnUpdates: Boolean = true,
        cache: YeelightCache? = null
) : YeelightViewModel() {
    final override val devices = YeelightLiveData(connectionPool, invalidateOnUpdates, viewModelScope, cache)

    init {
        devices.start()
    }

    override fun onCleared() {
        super.onCleared()
        devices.onCleared()
    }
}

/** YeelightViewModel that will keep connections alive as long as livedata has any
 * active observers. */
open class YeelightActiveViewModel(
        invalidateOnUpdates: Boolean = true,
        cache: YeelightCache? = null
) : YeelightViewModel() {
    final override val devices = YeelightActiveLiveData(invalidateOnUpdates, viewModelScope, cache)
    override fun onCleared() {
        super.onCleared()
        devices.onCleared()
    }
}