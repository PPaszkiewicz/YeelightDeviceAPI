package com.github.ppaszkiewicz.yeelightdeviceapi

import android.os.Bundle
import androidx.activity.viewModels
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightViewModel
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterBase
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterLive
import kotlinx.android.synthetic.main.scroll_content.*

/** Activity handling connection with devices. */
class DemoLiveActivity : DemoActivity(){
    override val adapter: DevicesAdapterBase by lazy { DevicesAdapterLive(this) }

    override val yeelightViewModel : YeelightViewModel by viewModels{
        YeelightViewModel.Active(PreferenceDeviceCache(application))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        txtGeneralHint.setText(R.string.title_live)
        txtRecyclerHint.setText(R.string.tips_live, R.string.tips_general)
        title = "Live Connection"
    }

    /**
     * YeelightActiveLiveData keeps isOnline state variable true as long as it's connected.
     */
    override fun validateCanSendToDevice(device: YeelightDevice): Boolean {
        return device.canConnect() //&& device.isOnline disabled because it prevents queuing messages
    }
}