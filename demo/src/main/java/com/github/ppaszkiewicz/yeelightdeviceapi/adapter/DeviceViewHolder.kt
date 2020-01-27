package com.github.ppaszkiewicz.yeelightdeviceapi.adapter

import android.annotation.SuppressLint
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.recyclerview.widget.RecyclerView
import com.github.ppaszkiewicz.yeelight.api.currentColor
import com.github.ppaszkiewicz.yeelight.api.onOff
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import com.github.ppaszkiewicz.yeelight.core.values.YeelightDeviceModel
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp
import com.github.ppaszkiewicz.yeelightdeviceapi.R

/** Viewholder displaying one of the bulbs. */
class DeviceViewHolder(itemView: View, val adapter: DevicesAdapterBase) : RecyclerView.ViewHolder(itemView) {
    val icon: AppCompatImageView = itemView.findViewById(R.id.imgDeviceIcon)
    val iconOutline: AppCompatImageView = itemView.findViewById(R.id.imgDeviceIconOutline)
    val name: TextView = itemView.findViewById(R.id.txtDeviceName)
    val status: TextView = itemView.findViewById(R.id.txtDeviceStatus)
    val checked: CheckBox = itemView.findViewById(R.id.chkDeviceChecked)

    /** Gets device at this adapters position*/
    val device get() = adapter.devices[adapterPosition]

    init {
        checked.setOnClickListener {
            if (checked.isChecked)
                adapter.checkedIds.add(device.id)
            else
                adapter.checkedIds.remove(device.id)
        }
        icon.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                    .setMessage(device.dumpDeviceInfo())
                    .show()
        }
    }

    fun bind(item: YeelightDevice) {
        when (item.model) {
            YeelightDeviceModel.STRIPE -> {
                icon.setImageResource(R.drawable.ic_stripe)
                iconOutline.setImageResource(R.drawable.ic_stripe_outline)
            }
            else -> {
                icon.setImageResource(R.drawable.ic_bulb)
                iconOutline.setImageResource(R.drawable.ic_bulb_outline)
            }
        }
        icon.setColorFilter(item.currentColor())
        name.text = item.name
        setStatusText(item)
        checked.isChecked = adapter.checkedDevices.contains(item)
    }

    @SuppressLint("SetTextI18n")
    fun update(item: YeelightDevice, payload: Any) {
        if (payload is YeelightReply) {
            payload.propHashMap?.forEach { pair ->
                //must use pair instead of k, v or run fails below jdk8
                when (pair.key) {
                    YeelightProp.name -> name.text = item.name
                    YeelightProp.rgb, YeelightProp.hue, YeelightProp.sat,
                    YeelightProp.ct, YeelightProp.color_mode, YeelightProp.flowing -> {
                        icon.setColorFilter(item.currentColor())
                        setStatusText(item)
                    }
                    YeelightProp.power, YeelightProp.bright -> setStatusText(item)
                    else -> Unit    //does not display other props directly
                }
            }
        } else if (payload is Boolean)
            setStatusText(item)
    }

    @SuppressLint("SetTextI18n")
    fun setStatusText(item: YeelightDevice) {
        status.text = "${item.model} ${item.id} ${onlineString(item)}" +
                "\n${item.power.onOff()}, ${item.brightness}%, ${colorMode(item)}"
    }

    private fun onlineString(item: YeelightDevice): String {
        return when {
            !item.isOnline && !item.isDiscovered -> "[CACHED]"
            !item.isOnline -> "[OFFLINE]"
            else -> "[${item.address}]"
        }
    }

    private fun colorMode(item: YeelightDevice): String {
        if (item.isFlowing) return "Flow active"
        return when (item.colorMode) {
            YeelightProp.ColorMode.mode_unknown -> "??"
            YeelightProp.ColorMode.mode_rgb -> {
                item.color.run { "RGB($red $green $blue)" }
            }
            YeelightProp.ColorMode.mode_color_temp -> {
                "TEMP(${item.colorTemp}K)"
            }
            YeelightProp.ColorMode.mode_hsv -> {
                "HSV(H${item.hue} S${item.saturation})"
            }
        }
    }
}