package com.github.ppaszkiewicz.yeelightdeviceapi

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.ppaszkiewicz.yeelight.api.YeelightCache
import com.github.ppaszkiewicz.yeelight.api.connection.YeelightAutoConnectionKt
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightLiveData
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightViewModel
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterAuto
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterBase
import kotlinx.android.synthetic.main.scroll_content.*

/**
 * Similar to [DemoAutoActivity] but does not perform scan until requested.
 * */
class DemoNoScanActivity : DemoActivity() {
    override val adapter: DevicesAdapterBase by lazy { DevicesAdapterAuto(this) }

    override val yeelightViewModel: NoScanYeelightViewModel by viewModels {
        object : ViewModelProvider.Factory{
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return NoScanYeelightViewModel(PreferenceDeviceCache(application)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        txtGeneralHint.setText(R.string.title_cached)
        txtRecyclerHint.setText(R.string.tips_cached, R.string.tips_general)
        title = "No Scan"
    }

    /**
     * Default YeelightLiveData does not keep track of isOnline variable so don't check it.
     */
    override fun validateCanSendToDevice(device: YeelightDevice): Boolean {
        return device.canConnect()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.menu_refresh_button){
            // only rescan and dont start announcement listener
            yeelightViewModel.scanWithoutListening()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

/**
 * Custom Yeelight ViewModel that does not query the connections
 */
class NoScanYeelightViewModel(cache: YeelightCache) : YeelightViewModel() {
    override val devices: YeelightLiveData = YeelightLiveData(
            YeelightAutoConnectionKt.PoolProvider(),
            true,
            viewModelScope,
            cache
    )

    init {
        devices.restoreFromCache()
    }

    /** Scan for devices and DON'T start announcement listener. */
    fun scanWithoutListening(){
        devices.scanOnly()
    }

    override fun onCleared() {
        super.onCleared()
        devices.onCleared()
    }
}