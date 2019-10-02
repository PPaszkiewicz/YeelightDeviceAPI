package com.github.ppaszkiewicz.yeelightdeviceapi

import android.app.Application
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit
import com.github.ppaszkiewicz.yeelight.api.YeelightCache
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.YeelightDeviceMap
import org.json.JSONArray
import org.json.JSONException

/**
 * Cache that crudely keeps devices cached in shared preferences.
 */
class PreferenceDeviceCache(val application: Application) :YeelightCache{
    companion object{
        const val SAVE_DEVICES = "SAVE_DEVICES"
    }

    override fun getCachedDevices(): YeelightDeviceMap? {
        val savedDevicesJson = PreferenceManager.getDefaultSharedPreferences(application)
                .getString(SAVE_DEVICES, null)
        if(savedDevicesJson == null) return null
        try {
            val jsArr = JSONArray(savedDevicesJson)
            val devices = Array(jsArr.length()) {
                YeelightDevice.fromJSON(jsArr.getJSONObject(it))
            }
            return YeelightDeviceMap(devices.associateBy { it.id })
        } catch (jsEx: JSONException) {
            Log.e("PreferenceDeviceCache", "failed to restore state from json.")
            jsEx.printStackTrace()
        }
        return null
    }

    override fun cacheDevices(map: YeelightDeviceMap?) {
        if(map == null) return
        val jsArray = JSONArray().apply {
            map.values.forEach {
                put(it.toJson())
            }
        }
        val jsString = jsArray.toString()
        PreferenceManager.getDefaultSharedPreferences(application).edit {
            putString(SAVE_DEVICES, jsString)
        }
    }
}