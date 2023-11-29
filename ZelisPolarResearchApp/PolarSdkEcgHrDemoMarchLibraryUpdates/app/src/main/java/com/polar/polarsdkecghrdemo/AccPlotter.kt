package com.polar.polarsdkecghrdemo

import android.graphics.Color
import android.graphics.Paint
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.XYSeriesFormatter
import com.polar.sdk.api.model.PolarAccelerometerData
import java.util.*


//import com.androidplot.xy.AdvancedLineAndPointRenderer

class AccPlotter {
    companion object {
        private const val TAG = "AccPlotter"
        //may need new number values to determine the distance of plotting for accelerometer data
        private const val NVALS = 300 //15 = 15 seconds; 300 = 5 min
        private const val RR_SCALE = .1
    }



    private var listener: PlotterListener? = null

    //val timeFormatter: XYSeriesFormatter<*>
    val xFormatter: XYSeriesFormatter<*>
    val yFormatter: XYSeriesFormatter<*>
    val zFormatter: XYSeriesFormatter<*>

    //val xTimeSeries: SimpleXYSeries
    //val yTimeSeries: SimpleXYSeries
    //val zTimeSeries: SimpleXYSeries
    val xSeries: SimpleXYSeries
    val ySeries: SimpleXYSeries
    val zSeries: SimpleXYSeries

    private val xTimeAccVals = MutableList(NVALS) { 0.0 }
    private val yTimeAccVals = MutableList(NVALS) { 0.0 }
    private val zTimeAccVals = MutableList(NVALS) { 0.0 }
    private val xAccVals = MutableList(NVALS) { 0.0 }
    private val yAccVals = MutableList(NVALS) { 0.0 }
    private val zAccVals = MutableList(NVALS) { 0.0 }

    //val hrFormatter: XYSeriesFormatter<*>
    //val rrFormatter: XYSeriesFormatter<*>

    //val hrSeries: SimpleXYSeries
    //val rrXYSeries: SimpleXYSeries

    //private val xHrVals = MutableList(NVALS) { 0.0 }
    //private val yHrVals = MutableList(NVALS) { 0.0 }
    //private val xRrVals = MutableList(NVALS) { 0.0 }
    //private val yRrVals = MutableList(NVALS) { 0.0 }



    init {
        //variables to help calculate the start, end, and delta time (may need new math for acceleterometer data)+
        val now = Date()
        val endTime = now.time.toDouble()
        val startTime = endTime - NVALS * 1000
        val delta = (endTime - startTime) / (NVALS - 1)

        //specify initial values to keep it from auto sizing
        for (i in 0 until NVALS) {
            xTimeAccVals[i] = startTime + i * delta
            xAccVals[i] = -0.1
            yTimeAccVals[i] = startTime + i * delta
            yAccVals[i] = -1.0
            zTimeAccVals[i] = startTime + i * delta
            zAccVals[i] = -0.2

            //xHrVals[i] = startTime + i * delta
            //yHrVals[i] = 60.0
            //xRrVals[i] = startTime + i * delta
            //yRrVals[i] = 100.0
        }

        //plotting timeStamp vs. x-values with a red line
        xFormatter = LineAndPointFormatter(Color.RED, null, null, null)
        //xFormatter.isLegendIconEnabled()
        xSeries = SimpleXYSeries(xTimeAccVals, xAccVals, "x")

        //plotting timeStamp vs. y-values with a blue line
        yFormatter = LineAndPointFormatter(Color.BLUE, null, null, null)
        //yFormatter.isLegendIconEnabled()
        ySeries = SimpleXYSeries(yTimeAccVals, yAccVals, "y")

        //plotting timeStamp vs. z-values with a green line
        zFormatter = LineAndPointFormatter(Color.GREEN, null, null, null)
        //zFormatter.isLegendIconEnabled()
        zSeries = SimpleXYSeries(zTimeAccVals, zAccVals, "z")


    }


    /**
     * Implements a strip chart by moving series data backwards and adding
     * new data at the end.
     *
     * @param polarAccelerometerData The Accelerometer data that came in.
     */



    fun addValues(polarAccelerometerData: PolarAccelerometerData.PolarAccelerometerDataSample) {
        val now = Date()
        val time = now.time
        for (i in 0 until NVALS - 1) {
            xTimeAccVals[i] = xTimeAccVals[i + 1]
            yTimeAccVals[i] = yTimeAccVals[i + 1]
            zTimeAccVals[i] = zTimeAccVals[i + 1]
            xAccVals[i] = xAccVals[i + 1]
            yAccVals[i] = yAccVals[i + 1]
            zAccVals[i] = zAccVals[i + 1]

            xSeries.setXY(xTimeAccVals[i], xAccVals[i], i)
            ySeries.setXY(yTimeAccVals[i], yAccVals[i], i)
            zSeries.setXY(zTimeAccVals[i], zAccVals[i], i)

            //hrSeries.setXY(xHrVals[i], yHrVals[i], i)

            //xHrVals[i] = xHrVals[i + 1]
            //yHrVals[i] = yHrVals[i + 1]
            //xRrVals[i] = xRrVals[i + 1]
            //yRrVals[i] = yRrVals[i + 1]

            //hrSeries.setXY(xHrVals[NVALS - 1], yHrVals[NVALS - 1], NVALS - 1)

        }

        xTimeAccVals[NVALS - 1] = time.toDouble()
        xAccVals[NVALS - 1] = polarAccelerometerData.x.toDouble()
        xSeries.setXY(xTimeAccVals[NVALS - 1], xAccVals[NVALS - 1], NVALS - 1)

        yTimeAccVals[NVALS - 1] = time.toDouble()
        yAccVals[NVALS - 1] = polarAccelerometerData.y.toDouble()
        ySeries.setXY(yTimeAccVals[NVALS - 1], yAccVals[NVALS - 1], NVALS - 1)

        zTimeAccVals[NVALS - 1] = time.toDouble()
        zAccVals[NVALS - 1] = polarAccelerometerData.z.toDouble()
        zSeries.setXY(zTimeAccVals[NVALS - 1], zAccVals[NVALS - 1], NVALS - 1)

        //below here begins the impolementation of RR data, which I will not focus on right now
        //     as we don't know at what time the RR intervals start

        listener?.update()
    }



    fun setListener(listener: PlotterListener?) {
        this.listener = listener
    }

}