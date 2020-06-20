package com.manna.milkha

import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {lateinit var instance :List<TrackPoint>}

    fun presentData(canvasWidget: CanvasWidget, trackPoints:List<TrackPoint>) {
        drawRun(canvasWidget, trackPoints)
        val runDist = trackPoints.last().dist
        val runTime = runDuration(trackPoints)
        val pace = runTime.dividedBy(runDist.toLong()).multipliedBy(1000)
        val maxHR = trackPoints.maxBy {p -> p.HR}
        val validHRs = trackPoints.filter{p -> p.HR > 0}
        val minHR = validHRs.minBy{p -> p.HR}
        val avgHR = validHRs.sumBy {p -> p.HR} / validHRs.size

        distView.text = "dist: " + "%.2f".format(runDist/1000.0) + " km"
        aveHRView.text = "avg. HR: " + avgHR + " bpm"
        timeView.text = "time: " + formatTime(runTime)
        paceView.text = "pace: " + formatTime(pace) + " min/km"
        maxHRView.text = "max HR: " + maxHR?.HR + " bpm"
        minHRView.text = "min HR: " + minHR?.HR + " bpm"

        fillDistSplitsTable(trackPoints, applicationContext, splitsTable, splitSpinner)
    }

    fun loadData(uri: Uri) :List <TrackPoint>  {
        val stream = contentResolver.openInputStream(uri!!)
        return parse(stream!!)
    }

    fun initButton(button: ImageButton, trackPoints: List<TrackPoint>, klass:Class<*>) {
        button.setOnClickListener() {
            val intent = Intent(this, klass).apply {
                putExtra("com.manna.milkha.splitSpinner", splitSpinner.selectedItemPosition)
                instance = trackPoints
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val canvasWidget = initCanvas(imageView, resources)
        supportActionBar?.hide()
        val uri = intent.data
        if (uri != null) {
            val trackPoints = loadData(uri)
            initSplitSpinner(trackPoints, this, splitSpinner, splitsTable)
            initButton(timeButton, trackPoints, TimeActivity::class.java)
            initButton(distButton, trackPoints, DistActivity::class.java)
            presentData(canvasWidget, trackPoints)
        }
        else
        {
            val toast = Toast.makeText(applicationContext, "Sorry! Milkha must be used via Gadgetbridge.",
                                              Toast.LENGTH_LONG)
            toast.show()
            finish()
        }
    }
}

