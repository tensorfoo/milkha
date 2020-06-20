package com.manna.milkha

import android.graphics.Color
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.android.synthetic.main.activity_dist.*
import kotlinx.android.synthetic.main.activity_time.splitSpinner
import kotlinx.android.synthetic.main.activity_time.splitsTable
import java.time.Duration


class DistActivity : AppCompatActivity() {

    fun initSplitSpinner(canvasWidget: CanvasWidget, trackPoints: List<TrackPoint>, initial:Int) {
        splitSpinner.adapter = ArrayAdapter.createFromResource(
            applicationContext,
            R.array.distSpinnerArray,
            android.R.layout.simple_spinner_item
        )
        splitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                splitLabel.text = selectedItem
                presentData(canvasWidget, trackPoints)
            }

            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        splitSpinner.setSelection(initial)
    }
    fun presentData(canvasWidget: CanvasWidget, trackPoints:List<TrackPoint>) {
        val splits = fillDistSplitsTable(trackPoints, applicationContext, splitsTable, splitSpinner)
        val data = CombinedData()
        var entries = ArrayList<BarEntry>()
        var i = 1
        val startTime = trackPoints.first().time

        drawRun(canvasWidget, trackPoints)

        for (s in splits) {
            entries.add(BarEntry(i++.toFloat(), s.time.seconds.toFloat()))
        }

        val valueFormatter = object: IndexAxisValueFormatter() {

            override fun getFormattedValue(value: Float): String {
                return formatTime(Duration.ofSeconds(value.toLong()))
            }

            override fun getBarLabel(barEntry: BarEntry?): String {
                return formatTime(Duration.ofSeconds(barEntry!!.y.toLong()))
            }
        }

        chart.axisLeft.valueFormatter = valueFormatter
        val set1 = BarDataSet(entries, "Splits (seconds)")
        set1.setColor(Color.parseColor("#6eba6e"))
        val barData = BarData(set1)
        barData.setValueFormatter(valueFormatter)

        chart.setOnChartValueSelectedListener(object:OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                val splitIndex = e.x.toInt()-1
                val split = splits.get(splitIndex)
                val splitTrackPoints = splitTrackPoints(split, trackPoints)
                overlaySplit(canvasWidget, splitTrackPoints)
                val row = splitsTable.getChildAt(splitIndex+1) // +1 because 1st row is header
                row.requestRectangleOnScreen(Rect(0, 0, 0, 0))
                row.setBackgroundColor(Color.YELLOW)
                imageView.invalidate()
            }

            override fun onNothingSelected() { }
        })

        chart.setData(barData)
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.legend.isEnabled = false
        if (splits.size < 25) {
            chart.xAxis.isEnabled = true
            chart.xAxis.labelCount = splits.size
        }
        if(splits.size > 50)
            chart.xAxis.isEnabled = false
        chart.invalidate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("does this get called?")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dist)
        supportActionBar?.hide()
        val trackPoints = MainActivity.instance
        val splitValue = intent.getIntExtra("com.manna.milkha.splitSpinner", 1000) as Int
        val canvasWidget = initCanvas(imageView, resources)
        initSplitSpinner(canvasWidget, trackPoints, splitValue)
        presentData(canvasWidget, trackPoints)
    }
}

