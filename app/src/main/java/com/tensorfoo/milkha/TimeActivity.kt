package com.manna.milkha

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.android.synthetic.main.activity_time.*
import java.time.Duration
import android.widget.TableLayout as TableLayout1

class TimeActivity : AppCompatActivity() {

    fun fillSplitsTable(trackPoints: List<TrackPoint>, applicationContext: Context, splitsTable: TableLayout1, splitSpinner: Spinner):List<Split> {
        val context = applicationContext
        val splitSize = readSplitSpinner(splitSpinner)
        while(splitsTable.childCount > 1)  // remove the existing rows except the header row
            splitsTable.removeView(splitsTable.getChildAt(splitsTable.childCount -1))

        var i=1;
        val splits = timeSplits(splitSize, trackPoints)
        val startTrack = trackPoints.first()
        val endTrack = trackPoints.last()

        for (split in splits) {
            val row = TableRow(context)
            val splitTracks = splitTrackPoints(split, trackPoints)
            val dist = splitTracks.last().dist - splitTracks.first().dist
            var textView = TextView(context)

            textView.text = ordinal(i++)
            textView.setPadding(10, 0, 0,0)
            row.addView(textView, 0)

            textView = TextView(context)
            textView.text = "%.1fm".format(dist)
            row.addView(textView, 1)

            textView = TextView(context)
            textView.text = formatTime(Duration.ofSeconds((1000/dist* splitSize).toLong())) + "/km"
            textView.setPadding(70, 0, 0, 0)
            row.addView(textView, 2)

            textView = TextView(context)
            textView.text = if (split.avgHR > 0) split.avgHR.toString() + " bpm" else "n/a"
            textView.setPadding(10, 0, 0, 0)
            row.addView(textView, 3)

            textView = TextView(context)
            textView.text = formatTime(Duration.between(startTrack.time, split.endTrackPoint.time))
            textView.setPadding(40, 0, 0,0)
            row.addView(textView, 4)

            textView = TextView(context)
            textView.setPadding(25, 0, 0, 0)
            textView.text = "%.2f".format(split.endTrackPoint.dist/1000) + "km"
            row.addView(textView, 5)

            if (i % 2 == 0) row.setBackgroundColor(Color.parseColor("#6eba6e"))

            splitsTable.addView(row)
        }
        return splits
    }


    fun initSplitSpinner(canvasWidget: CanvasWidget, trackPoints: List<TrackPoint>) {

        R.array.timeSpinnerArray
        splitSpinner.adapter = ArrayAdapter.createFromResource(
            applicationContext,
            R.array.timeSpinnerArray,
            android.R.layout.simple_spinner_item
        )
        splitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long) {
                if (view != null) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    splitLabel.text = selectedItem
                    fillSplitsTable(trackPoints, applicationContext, splitsTable, splitSpinner)
                    presentData(canvasWidget, trackPoints)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        splitSpinner.setSelection(5) // make the default split 3min
    }

    fun readSplitSpinner(spinner:Spinner):Int {
        val item = spinner.selectedItem.toString().split(" ")[0].split(":")
        return Integer.parseInt(item[0])* 60 + Integer.parseInt(item[1])
    }

    fun presentData(canvasWidget: CanvasWidget, trackPoints:List<TrackPoint>) {
        val splits = fillSplitsTable(trackPoints, applicationContext, splitsTable, splitSpinner)
        var entries = ArrayList<BarEntry>()
        var i = 1

        drawRun(canvasWidget, trackPoints)

        for (split in splits) {
            val splitTracks = splitTrackPoints(split, trackPoints)
            val delta = splitTracks.last().dist - splitTracks.first().dist
            entries.add(BarEntry(i++.toFloat(), delta.toFloat()))
        }

        val valueFormatter = object: IndexAxisValueFormatter() {

            fun formatVal(value: Float):String {
                return "%.1fm".format(value)
            }
            override fun getFormattedValue(value: Float): String {
                return formatVal(value)
            }

            override fun getBarLabel(barEntry: BarEntry?): String {
                return formatVal(barEntry!!.y)
            }

        }

        chart.axisLeft.valueFormatter = valueFormatter

        val set1 = BarDataSet(entries, "Splits (seconds)")
        set1.setColor(Color.parseColor("#6eba6e"))
        val barData = BarData(set1)
        barData.setValueFormatter(valueFormatter)

        chart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                val splitIndex = e.x.toInt() - 1
                val split = splits.get(splitIndex)
                val splitTrackPoints = splitTrackPoints(split, trackPoints)
                overlaySplit(canvasWidget, splitTrackPoints)
                val row = splitsTable.getChildAt(splitIndex+1) // +1 because 1st row is header
                row.requestRectangleOnScreen(Rect(0, 0, 0, 0))
                row.setBackgroundColor(Color.YELLOW)
                imageView.invalidate()
            }

            override fun onNothingSelected() {
            }
        })
        chart.setData(barData)
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.legend.isEnabled = false
        if (splits.size < 25)
            chart.xAxis.labelCount = splits.size
        chart.invalidate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)
        supportActionBar?.hide()
        val trackPoints = MainActivity.instance
        val canvasWidget = initCanvas(imageView, resources)
        initSplitSpinner(canvasWidget, trackPoints)
        presentData(canvasWidget, trackPoints)
    }
}
