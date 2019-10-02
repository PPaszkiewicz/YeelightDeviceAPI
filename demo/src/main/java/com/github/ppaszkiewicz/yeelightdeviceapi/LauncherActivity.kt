package com.github.ppaszkiewicz.yeelightdeviceapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_launcher.*


const val PREF_SELECTED_ACTIVITY = "PREF_SELECTED_ACTIVITY"

class LauncherActivity : AppCompatActivity() {
    // demo that will be reselected when app is restarted
    enum class SelectedDemo {
        LIVE, AUTO, NOSCAN;
        companion object{
            val values = values()
        }
    }

    private var isBasicActivitySelected: SelectedDemo?
        get() =
            preferences.run {
                if (contains(PREF_SELECTED_ACTIVITY))
                    SelectedDemo.values.getOrNull(getInt(PREF_SELECTED_ACTIVITY, -1))
                else
                    null
            }
        set(value) {
            preferences.edit().apply {
                if (value != null)
                    putInt(PREF_SELECTED_ACTIVITY, value.ordinal)
                else
                    remove(PREF_SELECTED_ACTIVITY)
            }.apply()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        btnSimple.setOnClickListener {
            isBasicActivitySelected = null
            startActivity(Intent(this, DemoSimpleActivity::class.java))
        }

        btnStateful.setOnClickListener {
            isBasicActivitySelected = SelectedDemo.LIVE
            startActivity(Intent(this, DemoLiveActivity::class.java))
        }
        btnStateless.setOnClickListener {
            isBasicActivitySelected = SelectedDemo.AUTO
            startActivity(Intent(this, DemoAutoActivity::class.java))
        }
        btnNoScan.setOnClickListener {
            isBasicActivitySelected = SelectedDemo.NOSCAN
            startActivity(Intent(this, DemoNoScanActivity::class.java))
        }


//         restore previous selection automatically
        isBasicActivitySelected?.let {
            startActivity(Intent(this,
                    when(it){
                        SelectedDemo.LIVE -> DemoLiveActivity::class.java
                        SelectedDemo.AUTO -> DemoAutoActivity::class.java
                        SelectedDemo.NOSCAN -> DemoNoScanActivity::class.java
                    }))
        }
    }
}

// small extension
val Context.preferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)