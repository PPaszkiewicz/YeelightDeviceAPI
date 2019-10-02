package com.github.ppaszkiewicz.yeelightdeviceapi

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.ppaszkiewicz.yeelight.api.YLogCat
import com.github.ppaszkiewicz.yeelight.api.onReply
import com.github.ppaszkiewicz.yeelight.api.viewModel.YeelightViewModel
import com.github.ppaszkiewicz.yeelight.core.YeelightDevice
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightCommand
import com.github.ppaszkiewicz.yeelight.core.connection.YeelightReply
import com.github.ppaszkiewicz.yeelight.core.values.YeelightCron
import com.github.ppaszkiewicz.yeelight.core.values.YeelightFlow
import com.github.ppaszkiewicz.yeelight.core.values.YeelightProp
import com.github.ppaszkiewicz.yeelightdeviceapi.adapter.DevicesAdapterBase
import kotlinx.android.synthetic.main.activity_demo.*
import kotlinx.android.synthetic.main.scroll_content.*
import kotlin.math.roundToInt

/** Base of device control activity. */
abstract class DemoActivity : AppCompatActivity() {
    companion object {
        const val TAG = "DemoActivity"
    }

    /** Devices adapter */
    abstract val adapter : DevicesAdapterBase

    /** inject android logger into yeelight library */
    private val logCatter = YLogCat().install()

    /** View model. */
    abstract val yeelightViewModel: YeelightViewModel
    /** Keep reference to last command to enable copying it. */
    var lastSentCommand : YeelightCommand? = null

    @Suppress("NestedLambdaShadowedImplicitParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        //power buttons
        btnOn.setOnClickListener { forEachChecked { it.setPower(true) } }
        btnOff.setOnClickListener { forEachChecked { it.setPower(false) } }
        btnToggle.setOnClickListener { forEachChecked { it.togglePower() } }
        btnDefault.setOnClickListener { forEachChecked { it.setDefault() } }

        //adjust buttons
        btnBrightDec.setOnClickListener { forEachChecked { it.decreaseBrightness() } }
        btnBrightCircle.setOnClickListener { forEachChecked { it.circleBrightness() } }
        btnBrightInc.setOnClickListener { forEachChecked { it.increaseBrightness() } }
        btnTempDec.setOnClickListener { forEachChecked { it.decreaseColorTemp() } }
        btnTempCircle.setOnClickListener { forEachChecked { it.circleColorTemp() } }
        btnTempInc.setOnClickListener { forEachChecked { it.increaseColorTemp() } }
        btnColorCircle.setOnClickListener { forEachChecked { it.circleColor() } }

        //change brightness
        btnTestBrightness.setOnClickListener {
            val b = etBrightness.intValue(1, 100)
            forEachChecked { it.setBrightness(b) }
            seekBrightness.progress = b
        }
        seekBrightness.setOnSeekBarChangeListener { i, b ->
            if (b) {
                etBrightness.setText((i + 1).toString())
            }
        }

        //set color temp
        btnTemp.setOnClickListener {
            val t = etTemp.intValue(1700, 6500)
            forEachChecked { it.setColorTemp(t) }
            seekTemp.progress = t - 1700
        }
        btnTemp.setOnLongClickListener {
            val t = etTemp.intValue(1700, 6500)
            seekTemp.progress = t - 1700
            toast("Setting Temp scene")
            forEachChecked {
                it.setSceneColorTemp(t, etBrightness.intValue(1, 100))
            }
        }
        seekTemp.setOnSeekBarChangeListener { i, b ->
            if (b) etTemp.setText((i + 1700).toString())
        }

        //set color RGB
        val tw = ColorWatcher(viewColorRGBPreview, ::getRGBColor)
        etColorR.addTextChangedListener(tw)
        etColorG.addTextChangedListener(tw)
        etColorB.addTextChangedListener(tw)
        btnColorRGB.setOnClickListener {
            forEachChecked {
                it.setColor(getRGBColor())
            }
        }
        btnColorRGB.setOnLongClickListener {
            toast("Setting RGB scene")
            forEachChecked {
                it.setSceneColor(getRGBColor(), etBrightness.intValue(1, 100))
            }
        }
        viewColorRGBPreview.setOnClickListener {
            var rgbColor = getRGBColor()
            rgbColor = if (rgbColor == Color.BLACK) Color.WHITE else rgbColor
            // use white as default instead of black
            showColorPicker("RGB", rgbColor) { c ->
                etColorR.setText(Color.red(c).toString())
                etColorG.setText(Color.green(c).toString())
                etColorB.setText(Color.blue(c).toString())
            }
        }

        //set color HSV
        val hsvW = ColorWatcher(viewColorHSVPreview, ::getHSVColor)
        etColorH.addTextChangedListener(hsvW)
        etColorS.addTextChangedListener(hsvW)
        btnColorHSV.setOnClickListener {
            forEachChecked { it.setHSVColor(etColorH.intValue(360), etColorS.intValue(100)) }
        }
        btnColorHSV.setOnLongClickListener {
            toast("Setting HSV scene")
            forEachChecked {
                it.setSceneHSVColor(etColorH.intValue(360), etColorS.intValue(100), etBrightness.intValue(1, 100))
                // it.updateProps(YeelightProp.hue, YeelightProp.sat, YeelightProp.bright) // request props to update as well
            }
        }
        viewColorHSVPreview.setOnClickListener {
            showColorPicker("HSV", getHSVColor()) { c ->
                val hsv = FloatArray(3)
                Color.colorToHSV(c, hsv)
                etColorH.setText(hsv[0].roundToInt().toString())
                etColorS.setText((hsv[1] * 100).roundToInt().toString())
            }
        }

        //rename
        btnEditName.setOnClickListener {
            val newName = etEditName.text.toString()
            if (forEachChecked {
                        it.name = newName
                        it.updateProps(YeelightProp.name) // request name to update recycler
                    })
                toast("Renamed device to $newName")
        }

        //set fade time
        btnTestFade.setOnClickListener {
            val fadeTime = etFadeTime.intValue(10000)
            if (forEachChecked {
                        it.defaultEffect = YeelightCommand.Effect.of(fadeTime)
                        null
                    })
                toast("Changed fade time to $fadeTime")
        }

        //FLOW actions
        btnFlowSpectrum.setOnClickListener {
            val flow = YeelightFlow.Builder()
                    .color(2000, Color.BLUE, 20)
                    .color(2000, Color.RED, 20)
                    .color(2000, Color.GREEN, 20)
                    .repeatCount(YeelightFlow.REPEAT_INFINITE)
                    .build()
            forEachChecked {
                it.startColorFlow(flow)
            }
        }

        btnFlowFlash.setOnClickListener {
            val flow = YeelightFlow.Builder()
                    .temp(300, 6500, 100)
                    .sleep(100)
                    .temp(300, 1700, 1)
                    .sleep(100)
                    .repeatCount(2)
                    .build()
            forEachChecked {
                it.startColorFlow(flow)
            }
        }

        btnFlowOff.setOnClickListener {
            val flow = YeelightFlow.Builder()
                    .color(2000, Color.CYAN, 100)
                    .color(3000, Color.RED, 50)
                    .color(3000, Color.WHITE, 1)
                    .endAction(YeelightFlow.EndAction.turn_off)
                    .build()
            forEachChecked {
                it.startColorFlow(flow)
            }
        }

        btnFlowStop.setOnClickListener {
            forEachChecked {
                it.stopColorFlow()
            }
        }

        // SCENE actions
        btnSceneSpectrum.setOnClickListener {
            val flow = YeelightFlow.Builder()
                    .color(2000, Color.BLUE, 50)
                    .color(2000, Color.RED, 50)
                    .color(2000, Color.GREEN, 50)
                    .repeatCount(YeelightFlow.REPEAT_INFINITE)
                    .build()
            forEachChecked { it.setSceneColorFlow(flow) }
        }

        btnSceneSpectrum2.setOnClickListener {
            // flow with steps inheriting the brightness
            // note that the first step still needs to have brightness set
            val flow = YeelightFlow.Builder()
                    .color(4000, Color.BLUE, 100)
                    .colorChain(4000, Color.MAGENTA, Color.RED, Color.YELLOW,
                            Color.GREEN, Color.CYAN)
                    .repeatCount(YeelightFlow.REPEAT_INFINITE)
                    .build()
            forEachChecked { it.setSceneColorFlow(flow) }
        }


        // others
        btnSetCron1.setOnClickListener { forEachChecked { it.setCron(YeelightCron.Type.power_off, 1) } }
        btnSetCron2.setOnClickListener { forEachChecked { it.setCron(YeelightCron.Type.power_off, 5) } }
        // request cron asynchronously
        btnGetCron.setOnClickListener {
            forOneChecked {
                it.requestCron(YeelightCron.Type.power_off).onReply(this@DemoActivity) {
                    it.cron?.let { cron ->
                        // get message depending on CRON type
                        val msg = when (cron.type) {
                            YeelightCron.Type.power_off -> "off in ${cron.delay}min"
                            null -> "no crons"
                        }
                        uiToast(msg)
                    }
                }
            }
        }
        btnDelCron.setOnClickListener { forEachChecked { it.delCron(YeelightCron.Type.power_off) } }

        // prop getters
        btnGetColorMode.setOnClickListener {
            forOneChecked {
                it.updateProps(YeelightProp.color_mode).onReply(this@DemoActivity, ::onGetPropReply)
            }
        }
        btnGetRgb.setOnClickListener {
            forOneChecked {
                it.updateProps(YeelightProp.rgb).onReply(this@DemoActivity, ::onGetPropReply)
            }
        }
        btnGetBright.setOnClickListener {
            forOneChecked {
                it.updateProps(YeelightProp.bright).onReply(this@DemoActivity, ::onGetPropReply)
            }
        }

        // raw commands
        btnRawCommand.setOnClickListener {
            forEachChecked {
                it.sendCommandMessage(YeelightCommand.Raw(etRawCommand.text.toString()))
            }
        }

        // restore devices user had selected closing the app last time
        adapter.restoreCheckedState(this)

        // observe livedata to see devices *********************************************************
        yeelightViewModel.devices.observe(this, Observer { result ->
            if(result == null){
                Log.e(TAG, "Obtained null device list?")
                return@Observer
            }
            if (result.error != null) {
                Log.e(TAG, "error obtaining devices: ${result.error}")
                toast("${result.error!!.message}")
            } else {
                if (result.isNullOrEmpty()) {
                    toast("No devices found")
                }
            }
            if (result.isNotEmpty()) {
                adapter.changeDataSet(result.values.toTypedArray())
            }
        })
        // *****************************************************************************************

        recDevicesRecycler.let {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(this)
            it.setHasFixedSize(true)
        }
    }

    override fun onDestroy() {
        adapter.saveCheckedState(this)
        super.onDestroy()
    }

    /** Show color picker and execute [onOk] when it's accepted. */
    fun showColorPicker(title: String, from: Int, onOk: (Int) -> Unit) {
        ColorPickerDialogBuilder.with(this)
                .setTitle(title)
                .initialColor(from)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .setPositiveButton("ok") { dialog, selectedColor, allColors -> onOk(selectedColor) }
                .build()
                .show()
    }


    /** Used by [forEachChecked] to see if each device can indeed send commands. */
    abstract fun validateCanSendToDevice(device: YeelightDevice) : Boolean

    /** Run for each or print error*/
    inline fun forEachChecked(f: (YeelightDevice) -> YeelightCommand?): Boolean {
        val devices = adapter.checkedDevices
        return if (devices.isNotEmpty()) {
            devices.forEach {
                if (validateCanSendToDevice(it))
                    // execute and store sent command
                    f(it)?.let{comm ->
                        lastSentCommand = comm
                    }
                else {
                    Log.e("DemoActivity", "cannot send messages to $it")
                }
            }
            true
        } else {
            toast("Select at least one device")
            false
        }
    }

    inline fun forOneChecked(f: (YeelightDevice) -> YeelightCommand?): Boolean {
        if (adapter.checkedDevices.size > 1) {
            toast("Select only 1 device for get")
            return false
        }
        return forEachChecked(f)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh_button -> {
                toast("Rescanning")
                yeelightViewModel.devices.rescan()
                true
            }
            R.id.menu_copy -> {
                lastSentCommand?.let{
                    toast("Last command shown")
                    etRawCommand.setText(it.toJSON(), TextView.BufferType.NORMAL)
                    etRawCommand.requestFocus()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun getRGBColor() = Color.rgb(etColorR.intValue(255), etColorG.intValue(255), etColorB.intValue(255))
    fun getHSVColor() = Color.HSVToColor(floatArrayOf(etColorH.intValue(360).toFloat(), etColorS.intValue(100) / 100f, 1f))

    private fun onGetPropReply(reply: YeelightReply) {
        // this needs to be printed!
        reply.propHashMap?.let { map ->
            when {
                map.isEmpty() -> {
                    uiToast("Invalid get_prop result")
                }
                map.size > 1 -> uiToast(map.values.joinToString())
                else -> {
                    val k = map.keys.toTypedArray()[0]
                    if (k == YeelightProp.rgb) {
                        uiToast("$k = ${map.getColorString(k)}")
                    } else
                        uiToast("$k = ${map[k]}")
                }
            }
        }
    }

    /** Colors preview when text changes*/
    inner class ColorWatcher(val preview: ImageView, val colorCalc: () -> Int) : TextWatcher {
        override fun afterTextChanged(p0: Editable?) = preview.setColorFilter(colorCalc())
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit
    }
}