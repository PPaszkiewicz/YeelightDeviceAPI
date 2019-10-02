package com.github.ppaszkiewicz.yeelightdeviceapi.adapter

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelightdeviceapi.R
import org.json.JSONArray
import org.json.JSONException

abstract class DevicesAdapterBase : RecyclerView.Adapter<DeviceViewHolder>() {
    var devices: Array<YeelightDevice> = emptyArray()
    /** IDs checked by user */
    val checkedIds = ArrayList<Long>()
    /** Devices matching checked ids */
    val checkedDevices: List<YeelightDevice>
        get() = devices.toList().filter { it.id in checkedIds }

    init {
        setHasStableIds(true)
    }

    /** Set currently checked IDs. */
    fun setCheckedIds(checkedIds: Array<Long>?){
        if (checkedIds != null) {
            this.checkedIds.clear()
            this.checkedIds.addAll(checkedIds)
        }
    }

    /** Change dataset. Use null instead of [checkedIds] to keep checked data intact. */
    abstract fun changeDataSet(newData: Array<YeelightDevice>)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder =
            DeviceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.vh_device, parent, false), this)

    override fun getItemCount(): Int = devices.size
    override fun getItemId(position: Int): Long = devices[position].id
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) = holder.bind(devices[position])
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty())
            onBindViewHolder(holder, position)
        else {
            val device = devices[position]
            payloads.forEach {
                if (it is Array<*>)
                    it.forEach { item -> holder.update(device, item!!) }
                else
                    holder.update(device, it)
            }
        }
    }

    fun getPositionForId(id: Long) = devices.indexOfFirst { it.id == id }

    /** Save current checked item IDs in preferences. */
    fun saveCheckedState(context : Context){
        val ids = JSONArray().apply {
            checkedIds.forEach { put(it) }
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString("checkedIds", ids.toString())
        }
    }

    /** Restore current checked item IDs from preferences. */
    fun restoreCheckedState(context: Context){
        val jsStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("checkedIds", null)
        if(jsStr == null) return
        try {
            val idArr = JSONArray(jsStr)
            val restoredIds = LongArray(idArr.length()) {
                idArr.getLong(it)
            }
            setCheckedIds(restoredIds.toTypedArray())
        }catch (jsEx : JSONException){
            Log.e("Adapter", "Failed to restore checked state")
            jsEx.printStackTrace()
        }
    }
}

