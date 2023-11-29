package com.polar.polarsdkecghrdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYPlot
import com.androidplot.Plot
import com.androidplot.xy.*
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl.defaultImplementation
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import com.polar.sdk.api.PolarBleApi.PolarDeviceDataType
import com.polar.sdk.api.PolarBleApi.PolarBleSdkFeature
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.text.DecimalFormat
import java.util.*
import com.androidplot.xy.XYGraphWidget
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarBleApiDefaultImpl.defaultImplementation
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.format.DateTimeParseException
import java.util.*


//HR: import com.androidplot.xy.XYGraphWidget
//import com.polar.sdk.api.model.PolarHrData
//import java.text.DecimalFormat

//ECG: import com.polar.sdk.api.model.PolarSensorSetting
//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
//import io.reactivex.rxjava3.disposables.Disposable



//create a stop buttton to stop recording data
//create a save button to save the data in the .txt file





class ACCActivity : AppCompatActivity(), PlotterListener {
    companion object {
        private const val TAG = "ACCActivity"
    }


    //creating variables for the api
    private lateinit var api: PolarBleApi

    //creating variables for printing out the HR and RR data
    private lateinit var textViewHR: TextView
    private lateinit var textViewRR: TextView

    //creating variables for printing out the accelerometer data for each x, y, and z variable
    private lateinit var textViewAccX: TextView
    private lateinit var textViewAccY: TextView
    private lateinit var textViewAccZ: TextView

    //creating other variables for the components of the connected device
    private lateinit var textViewDeviceId: TextView
    private lateinit var textViewBattery: TextView
    private lateinit var textViewFwVersion: TextView

    //creating variables for plotting
    private lateinit var plot: XYPlot
    private lateinit var accPlotter: AccPlotter
    private var accDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null

    //variable for the connection of the specific Polar Device
    private lateinit var deviceId: String

    //variable for stopping and resuming the data stream
    private lateinit var stopStream: Button
    private lateinit var  resumeStream: Button

    //creating the variables for taking the data and putting it onto a .txt file
    private lateinit var showTextFile: Button
    private lateinit var viewTextFileBox: TextView

    //variable to convert the time stamp to the date and time for the .txt file
    private val simpleDateFormat = SimpleDateFormat("yyy-MM-dd HH:mm:ss", Locale.ENGLISH)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc) // this is where the intent takes places to go to the accelerometer activity

        //variable for the device's ID
        deviceId = intent.getStringExtra("id")
            ?: throw Exception("ACCActivity couldn't be created, no deviceId given")

        //variables for HR and RR data
        textViewHR = findViewById(R.id.hr)
        textViewRR = findViewById(R.id.rr)

        //variables for accelerometer's data: x, y, and z
        textViewAccX = findViewById(R.id.acc_view_x)
        textViewAccY = findViewById(R.id.acc_view_y)
        textViewAccZ = findViewById(R.id.acc_view_z)

        //variables for the components of the connected device
        textViewDeviceId = findViewById(R.id.acc_view_deviceId)
        textViewBattery = findViewById(R.id.acc_view_battery_level)
        textViewFwVersion = findViewById(R.id.acc_view_fw_version)

        //variable for helping generate the plot
        plot = findViewById(R.id.acc_view_plot)

        //variable for stopping and resuming the data stream
        stopStream = findViewById(R.id.stopStreamButton)
        resumeStream = findViewById(R.id.resumeStreamButton)

        //creating the variables for taking the data and putting it onto a .txt file
        showTextFile = findViewById(R.id.showTextFileButton)
        viewTextFileBox = findViewById(R.id.textFileTextView)



        //function that stops the streaming when the user clicks the button to stop streaming
        stopStream.setOnClickListener {
            //stops the accelerometer data streaming
            accDisposable?.dispose()
            accDisposable =  null

            //stops the HR data streaming
            hrDisposable?.dispose()
            hrDisposable = null
        }



        resumeStream.setOnClickListener {
            //resumes the accelerometer data streaming
            streamACC()

            //resumes the HR data streaming
            streamHR()
        }



        //function that shows the content of the dataFile.txt file
        showTextFile.setOnClickListener {
            try {
                val fileInputStream = openFileInput("dataFile.txt")
                val inputReader = InputStreamReader(fileInputStream)
                val output = inputReader.readText()

                //Data is displayed in the TextView
                viewTextFileBox.text = output
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



        //api section where different features are enabled
        api = PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext,
            setOf(
                PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )

        //api section where different features are enabled
        api.setApiLogger { str: String -> Log.d("SDK", str) }

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BluetoothStateChanged $powered")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connected ${polarDeviceInfo.deviceId}")
                Toast.makeText(applicationContext, R.string.connected, Toast.LENGTH_SHORT).show()
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device connecting ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "Device disconnected ${polarDeviceInfo.deviceId}")
            }

            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleApi.PolarBleSdkFeature
            ) {
                Log.d(TAG, "feature ready $feature")

                when (feature) {
                    PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING -> {
                        streamHR();
                        streamACC();
                        //streamECG();
                    }

                    else -> {}
                }
            }

            override fun hrFeatureReady(identifier: String) {
                //deprecated (updated github - 04/15/2023)
                //Log.d(TAG, "HR Feature ready $identifier")
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                if (uuid == UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")) {
                    val msg = "Firmware: " + value.trim { it <= ' ' }
                    Log.d(TAG, "Firmware: " + identifier + " " + value.trim { it <= ' ' })
                    textViewFwVersion.append(msg.trimIndent())
                }
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "Battery level $identifier $level%")
                val batteryLevelText = "Battery level: $level%"
                textViewBattery.append(batteryLevelText)
            }

            /*override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                //deprecated (updated github - 04/15/2023)
                //Log.d(TAG, "HR " + data.hr)
            }
             */

            override fun polarFtpFeatureReady(identifier: String) {
                //deprecated (updated github - 04/15/2023)
                //Log.d(TAG, "Polar FTP ready $identifier")
            }

        }) //ends onCreate and setApiCallback


        try {
            api.connectToDevice(deviceId)
        } catch (a: PolarInvalidArgument) {
            a.printStackTrace()
        }

        val deviceIdText = "ID: $deviceId"
        textViewDeviceId.text = deviceIdText


        accPlotter = AccPlotter()
        accPlotter.setListener(this)

        //I think I have to add a time series for each formatter
        //plot.addSeries(accPlotter.xTimeSeries, accPlotter.xFormatter)
        plot.addSeries(accPlotter.xSeries, accPlotter.xFormatter)

        //plot.addSeries(accPlotter.yTimeSeries, accPlotter.yFormatter)
        plot.addSeries(accPlotter.ySeries, accPlotter.yFormatter)

        //plot.addSeries(accPlotter.zTimeSeries, accPlotter.zFormatter)
        plot.addSeries(accPlotter.zSeries, accPlotter.zFormatter)

        plot.setRangeBoundaries(-3000, 3000, BoundaryMode.FIXED)
        plot.setDomainBoundaries(0, 360000, BoundaryMode.AUTO)

        //Left labels will increment by 250.00
        plot.setRangeStep(StepMode.INCREMENT_BY_VAL, 500.00)
        plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 60000.0)

        with(plot.domainTitle.text) {

        }

        plot.legend.isVisible

        //Make left labels into an integer (no decimal places)
        plot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format = DecimalFormat("#")

        //These don't seem to have an effect
        plot.linesPerRangeLabel = 2

    }


    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
    }



    //function that streams the accelerometer data (used both the PolarAdkEcgHrDemo app's streamECG() function in
    //     in the ECGActivity.kt file and the code in the AndroidBleSdkTestApp's accButton.setOnClicklistener)
    fun streamACC() {
        val fileOutputStream = openFileOutput("dataFile.txt", Context.MODE_PRIVATE)
        val outputWriter = OutputStreamWriter(fileOutputStream)
        val isDisposed = accDisposable?.isDisposed ?: true
        if (isDisposed) {
            //toggleButtonDown(accButton, R.string.stop_acc_stream) //don't believe I need this line from the function from AndroidBleSdkTestApp bc I am not clicking a start accelerometer data extraction button in this file's .xml file
            //accDisposable = api.requestStreamSettings(deviceId, sensorSetting.maxSettings()) //DeviceStreamingFeature.ACC exists in line 260 of the PolarBleApi.DeviceStreamingFeature.html file in the github
            accDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
                .toFlowable() //unsure if I need this or not
                .flatMap { sensorSetting: PolarSensorSetting ->
                    api.startAccStreaming(
                        deviceId,
                        sensorSetting.maxSettings()
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarAccelerometerData: PolarAccelerometerData ->
                        Log.d(TAG, "accelerometer update")
                        for (data in polarAccelerometerData.samples) {
                            Log.d(
                                TAG,
                                "ACC:    x: ${data.x} y: ${data.y} z: ${data.z} timeStamp: ${data.timeStamp}"
                            )
                            //attempting to add controls for SAMPLE_RATE and RANGE in the Polar Sensor Settings
                            val settings: MutableMap<PolarSensorSetting.SettingType, Int> =
                                mutableMapOf()
                            //settings[PolarSensorSetting.SettingType.SAMPLE_RATE] = 208
                            //settings[PolarSensorSetting.SettingType.RANGE] = 16
                            textViewAccX.text = "x = " + data.x.toString()
                            textViewAccY.text = "y = " + data.y.toString()
                            textViewAccZ.text = "z = " + data.z.toString()
                            accPlotter.addValues(data)


                            //this code below is going to create a text file and write string into it, else it opens the file and writes the string, and also saves the information to the .txt file
                            if (data.timeStamp.toString().isNotEmpty() && data.x.toString().isNotEmpty() &&
                                data.y.toString().isNotEmpty() && data.z.toString().isNotEmpty()) {
                                try {
                                    //outputWriter.write("Time Stamp: " + data.timeStamp. + "\n")
                                    outputWriter.write("Time: " + getDateString(data.timeStamp) + "\n")
                                    outputWriter.write("X: " + data.x.toString() + "\n")
                                    outputWriter.write("Y: " + data.y.toString() + "\n")
                                    outputWriter.write("Z: " + data.z.toString() + "\n")
                                    outputWriter.write("\n")
                                    //for (data in polarAccelerometerData.samples){
                                    //    outputWriter.write(data.timeStamp.toString() + " " + data.x.toString() + " " +
                                    //                        data.y.toString() + " " + data.z.toString() + "\n")
                                    //}

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            } else {
                                Toast.makeText(applicationContext, "No input?", Toast.LENGTH_SHORT)
                                    .show()
                            } //end of reading data into .txt file

                        //See if I can stop the stream if they click the "Stop Stream" button
                        //  which will also finish the streaming

                        //See if I can put an if condition if the save button is clicked to ensure the stream
                        // has stopped running before opening up .txt file

                        } // end of reading in data

                    //on click of the show text file button


                    },
                    { error: Throwable ->
                        Log.e(TAG, "Accelerometer stream failed $error")
                        accDisposable = null
                    },
                    {
                        Log.d(TAG, "Accelerometer stream complete")

                        outputWriter.close()
                        Toast.makeText(
                            baseContext,
                            "File saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
        } else {
            //NOTE stops streaming if it is "running
            accDisposable?.dispose()
            accDisposable = null
            //didn't include line from AndroidBleSdkTestApp accelerometer button function that says "toggleButtonUp(accButton, R.string.start_acc_stream)
        }
    }

    private fun getDateString(time: Long) : String = simpleDateFormat.format(time * 1000L)
    private fun getDateString(time: Int) : String = simpleDateFormat.format(time * 1000L)


    //function that streams the HR data (used both the PolarAdkEcgHrDemo app's streamHR() function in
    //     in the ECGActivity.kt file and the code in the AndroidBleSdkTestApp's hrButton.setOnClicklistener
    fun streamHR(){
        val isDisposed = hrDisposable?.isDisposed ?: true
        if (isDisposed) {
            hrDisposable = api.startHrStreaming(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR " + sample.hr)
                            if (sample.rrsMs.isNotEmpty()) {
                                val rrText = "(${sample.rrsMs.joinToString(separator = "ms, ")}ms)"
                                textViewRR.text = rrText
                            }

                            textViewHR.text = sample.hr.toString()
                            //plot.addValues(sample)

                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "HR stream failed. Reason $error")
                        hrDisposable = null
                    },
                    { Log.d(TAG, "HR stream complete")}
                )
        } else {
            //NOTE stops streaming if it is "running"
            hrDisposable?.dispose()
            hrDisposable = null
        }
    }



    override fun update() {
        runOnUiThread { plot.redraw() }
    }

    //private fun onClickConnectEcg(view: View) {
    //    streamACC().cancel
    //}


/*
    ***my attempt to make a function for inputting data into a .txt file but as of now, 04/21/2023,
        I am deciding to just put my code into the actual streamACC() function***

    fun dataToTxt () {
        if (data.x.toString().isNotEmpty()){
            try{
                val fileOutputStream = openFileOutput("dataFile.txt", Context.MODE_PRIVATE)
                val outputWriter = OutputStreamWriter(fileOutputStream)
                outputWriter.write(data.x.toString())
            }
        }
    }
 */


}
