package com.github.ppaszkiewicz.yeelightdeviceapi

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.github.ppaszkiewicz.yeelight.api.YLogCat
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightActiveViewModel
import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap
import kotlinx.android.synthetic.main.activity_simple.*

/**
 * Simple demo that gets devices from LiveData.
 */
class DemoSimpleActivity : AppCompatActivity(), Observer<YeelightDeviceMap> {
    companion object {
        const val TAG = "DemoSimpleActivity"
    }

    /** Livedata that will automatically connect and keep connections alive. */
    private val yeelightVM: YeelightActiveViewModel by viewModels()

    /** inject android logger into yeelight library */
    private val logCatter = YLogCat().install()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple)
        title = "Simple demo"

        // observe livedata to obtain the devices
        yeelightVM.devices.observe(this, this)

        // when clicking the button, toggle the power of all devices
        buttonToggle.setOnClickListener {
            yeelightVM.deviceList?.let {
                Log.d(TAG, "toggling the power of: ${it.size} devices")
                it.forEach { device ->
                    if (device.canConnect()) device.togglePower()
                }
            }
        }
    }

    override fun onChanged(result: YeelightDeviceMap) {
        if (result.error != null) {
            Log.e(TAG, "error obtaining devices: ${result.error}")
            txtSimple.text = "error: ${result.error!!.message}"
        } else {
            Log.d(TAG, "got devices: ${result.size}")
            if (result.isNullOrEmpty()) {
                txtSimple.text = "No devices found"
            }
        }

        if (result.isNotEmpty()) {
            val sb = StringBuilder()
            result.values.forEachIndexed { i, it ->
                sb.append("$i: ${it.model.displayName}, ${it.address}, On: ${it.power} \n")
            }
            txtSimple.text = sb.toString()
        }
    }
}