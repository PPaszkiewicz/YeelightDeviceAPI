package com.github.ppaszkiewicz.yeelightdeviceapi

import android.os.Bundle
import androidx.activity.viewModels
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightViewModel
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterAuto
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterBase
import kotlinx.android.synthetic.main.scroll_content.*

/** Activity that does not handle device connection (use library default). */
class DemoAutoActivity : DemoActivity(){
    override val adapter: DevicesAdapterBase by lazy { DevicesAdapterAuto(this) }

    override val yeelightViewModel : YeelightViewModel by viewModels{
        YeelightViewModel.Auto(PreferenceDeviceCache(application))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        txtGeneralHint.setText(R.string.title_auto)
        txtRecyclerHint.setText(R.string.tips_auto, R.string.tips_general)
        title = "Auto Connection"
    }

    /**
     * Default YeelightLiveData does not keep track of isOnline variable so don't check it.
     */
    override fun validateCanSendToDevice(device: YeelightDevice): Boolean {
        return device.canConnect()
    }
}