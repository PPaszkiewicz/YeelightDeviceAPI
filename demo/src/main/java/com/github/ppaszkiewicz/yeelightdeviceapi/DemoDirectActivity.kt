package com.github.ppaszkiewicz.yeelightdeviceapi

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.ppaszkiewicz.yeelight.api.connection.YeelightLifecycleConnection
import com.github.ppaszkiewicz.yeelight.api.onReply
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightConnection
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import kotlinx.android.synthetic.main.activity_direct.*
import java.util.regex.Pattern

/**
 * Activity that connects directly to one device with specific IP.
 */
class DemoDirectActivity : AppCompatActivity(R.layout.activity_direct) {
    companion object {
        const val TAG = "DemoSingleActivity"
    }
    
    var device: YeelightDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        buttonConnect.setOnClickListener { _ ->
            if(!validateIp()){
                toast("invalid IP format")
                return@setOnClickListener
            }
            val deviceIp = etRawIp.text.toString()
            val devicePort = etRawPort.text.toString().toInt()
            // check if this is a new device
            if (deviceIp != device?.address || devicePort != device?.port) {
                // release previous device connection
                device?.connectionProvider?.release()

                // create standalone device
                val newDevice = YeelightDevice.createSingle(deviceIp, devicePort)
                // inject connection provider - using lifecycle aware one so connection closes
                // alongside activity
                newDevice.connectionProvider = YeelightLifecycleConnection.PoolProvider(this)
                appendText("Connecting to $newDevice")
                newDevice.setConnectionListener(object : YeelightConnection.ListenerAdapter(){
                    override fun onYeelightDeviceConnected(deviceId: Long) {
                        appendText("connected")
                    }

                    override fun onYeelightDeviceDisconnected(deviceId: Long, error: Throwable?) {
                        appendText("disconnected")
                    }

                    override fun onYeelightDeviceConnectionError(deviceId: Long, exception: Throwable, failedCommand: YeelightCommand?) {
                        runOnUiThread { appendText("connection error $exception") }
                    }

                    override fun onYeelightDeviceResponse(deviceId: Long, deviceReply: YeelightReply) {
//                        runOnUiThread { appendText(deviceReply.toString()) }
                    }
                })
                device = newDevice
            }else{
                toast("already connected to this device")
            }
        }

        buttonToggle.setOnClickListener {
            device?.let{device ->
                device.togglePower().onReply(this){
                    appendText("power toggle is ${it.ok}")
                }
            } ?: toast("connect first")
        }

        buttonUpdate.setOnClickListener {
            device?.let{device ->
                appendText("requesting prop updates")
                device.updateAllProps().onReply(this){
                    appendText("properties updated: ${it.hasProps()}")
                }
            } ?: toast("connect first")
        }

        buttonInfo.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(device?.dumpDeviceInfo() ?: "Device not connected")
                    .show()
        }
    }

    private fun validateIp() : Boolean{
        val ipPattern = Pattern.compile("^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!\$)|\$)){4}\$")
        val matcher = ipPattern.matcher(etRawIp.text)
        return matcher.find()
    }

    private fun appendText(text : String){
        txtSimple.text = txtSimple.text.toString() + "\n$text"
    }
}