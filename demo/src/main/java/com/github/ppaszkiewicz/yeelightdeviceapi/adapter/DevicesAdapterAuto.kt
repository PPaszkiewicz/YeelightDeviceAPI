package com.github.ppaszkiewicz.yeelightdeviceapi.adapter

import android.util.Log
import android.widget.Toast
import com.github.ppaszkiewicz.yeelight.api.wrapWithLifecycle
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import com.github.ppaszkiewicz.yeelightdeviceapi.DemoActivity
import com.github.ppaszkiewicz.yeelightdeviceapi.uiToast

/***
 * Auto activity does not rely on LiveData to keep connection state.
 *
 * Instead update it manually from within connection listener here.
 */
class DevicesAdapterAuto(activity: DemoActivity) : DevicesAdapterBase(){
    /** Connection listener wrapped in lifecycle aware object. */
    val connectionListener = object : YeelightConnection.Listener {
        override fun onYeelightDeviceResponse(deviceId: Long, yeelightRepy: YeelightReply) {
            // toast any possible errors
            if (yeelightRepy.isError) {
                activity.uiToast("${yeelightRepy.method}: ${yeelightRepy.message}")
            }
        }

        override fun onYeelightDeviceConnectionError(deviceId: Long, exception: Throwable, failedCommand: YeelightCommand?) {
            if (failedCommand != null) {
                activity.uiToast("command failed: ${failedCommand.method}", Toast.LENGTH_SHORT)
            } else {
                activity.uiToast("failed to connect: ${exception.message}", Toast.LENGTH_SHORT)
            }
        }

        override fun onYeelightDeviceConnected(deviceId: Long) {
            activity.runOnUiThread {
                Log.d("Adapter", "device connected: $deviceId")
                // manually handle connected/disconnected flag
                val pos = getPositionForId(deviceId)
                if (pos != -1) {
                    devices[pos].isOnline = true
                    notifyItemChanged(pos, true)
                }
            }
        }

        override fun onYeelightDeviceDisconnected(deviceId: Long, error: Throwable?) {
            activity.runOnUiThread {
                Log.d("Adapter", "device disconnected: $deviceId")
                val pos = getPositionForId(deviceId)
                if (pos != -1) {
                    devices[pos].isOnline = false
                    notifyItemChanged(pos, false)
                }
            }
        }
    }.wrapWithLifecycle(activity)   // <---- NOTE: This entire listener is wrapped

    override fun changeDataSet(newData: Array<YeelightDevice>) {
        newData.forEach {
            if(it.isConnected){
                it.isOnline = true
            }
            //attach listeners to new data
            it.setConnectionListener(connectionListener)
        }
        devices = newData
        notifyDataSetChanged()
    }
}