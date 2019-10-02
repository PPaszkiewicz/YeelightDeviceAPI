package com.github.ppaszkiewicz.yeelightdeviceapi.adapter

import android.widget.Toast
import com.github.ppaszkiewicz.yeelight.api.wrapWithLifecycle
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import com.github.ppaszkiewicz.yeelightdeviceapi.DemoActivity
import com.github.ppaszkiewicz.yeelightdeviceapi.uiToast

/**
 * Live activity depends entirely on LiveData to manage device states, only swap device references and
 * add listener to show errors.
 * */
class DevicesAdapterLive(activity: DemoActivity) : DevicesAdapterBase() {
    /** Error listener wrapped with lifecycle aware object. */
    private val errorListener = object : YeelightConnection.ListenerAdapter() {
        override fun onYeelightDeviceResponse(deviceId: Long, deviceReply: YeelightReply) {
            if (deviceReply.isError) {
                activity.uiToast("${deviceReply.method}: ${deviceReply.message}")
            }
        }

        override fun onYeelightDeviceConnectionError(deviceId: Long, exception: Throwable, failedCommand: YeelightCommand?) {
            if (failedCommand != null) {
                activity.uiToast("command failed: ${failedCommand.method}", Toast.LENGTH_SHORT)
            } else {
                activity.uiToast("failed to connect: ${exception.message}", Toast.LENGTH_SHORT)
            }
        }
    }.wrapWithLifecycle(activity)   // <---- NOTE: This entire listener is wrapped

    override fun changeDataSet(newData: Array<YeelightDevice>) {
        newData.forEach {
            //attach error listeners to new data
            it.setConnectionListener(errorListener)
        }
        devices = newData
        notifyDataSetChanged()
    }
}