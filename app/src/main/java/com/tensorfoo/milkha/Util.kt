package com.manna.milkha

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import android.widget.*
import java.time.Duration

fun runDuration(trackPoints: List<TrackPoint>): Duration {
    return Duration.between(trackPoints.first().time, trackPoints.last().time)
}

fun runPace(trackPoints: List<TrackPoint>): Duration {
    val runTime = runDuration(trackPoints)
    val runDist = trackPoints.last().dist - trackPoints.first().dist
    return runTime.dividedBy(runDist.toLong()).multipliedBy(1000)
}

data class Split(val startTrackPoint:TrackPoint, val endTrackPoint:TrackPoint, val time: Duration, val avgHR:Int)


fun distSplits(splitSize:Int, trackPoints: List<TrackPoint>) : List<Split> {
    var splits = ArrayList<Split>()
    var i = 1; var prevMarker = trackPoints.first().time
    var hrsum = 0; var hrcount =0
    var startTrackPoint = trackPoints.first()

    for (p in trackPoints)
    {
        if (p.HR > 0) { hrsum += p.HR; hrcount++ }
        if (p.dist/splitSize >= i)
        {
            val avgHR = if (hrcount > 0) hrsum/hrcount else -1
            splits.add(Split(startTrackPoint, p, Duration.between(prevMarker, p.time), avgHR))
            startTrackPoint = p
            prevMarker = p.time
            i++; hrcount = 0; hrsum = 0
        }
    }

    return splits
}
fun timeSplits(splitSize:Int, trackPoints: List<TrackPoint>) : List<Split> {
    var splits = ArrayList<Split>()
    var i = 1;
    var hrsum = 0; var hrcount =0
    var startTrackPoint = trackPoints.first()
    val firstTstamp = startTrackPoint.time

    for (p in trackPoints)
    {
        val delta = Duration.between(firstTstamp, p.time).seconds
        if (p.HR > 0) { hrsum += p.HR; hrcount++ }
        if (delta/splitSize >= i)
        {
            val avgHR = if (hrcount > 0) hrsum/hrcount else -1
            splits.add(Split(startTrackPoint, p, Duration.ofSeconds(splitSize.toLong()), avgHR))
            startTrackPoint = p
            i++; hrcount = 0; hrsum = 0
        }
    }

    return splits
}

fun splitTrackPoints(split:Split, trackPoints:List<TrackPoint>):List<TrackPoint> {
    val results = ArrayList<TrackPoint>()
    var startCollecting = false

    for(p in trackPoints) {
        if(startCollecting)
            results.add(p)

        if (p == split.endTrackPoint)
            break

        else if (p == split.startTrackPoint)
            startCollecting = true
    }
    return results
}


fun ordinal(n:Int) :String {
    val suffixes =  listOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th");
    when(n % 100) {
        11,12,13 -> return n.toString() + "th"
        else -> return n.toString() + suffixes[n%10]
    }
}

fun formatTime(d: Duration):String {
    val h = d.toHours()
    val m = d.toMinutes()
    val s = d.seconds
    val f = "%02d"
    if (h==0L) return String.format(f, m) + ":" +
            String.format(f, d.minusMinutes(m).seconds)
    val m_ = d.minusHours(h).toMinutes()
    return String.format(f, h) + ":" +
            String.format(f, m_) + ":" +
            String.format(f, d.minusMinutes(m_).seconds)
}


data class CanvasWidget(val canvas: Canvas, val shapeDrawable: ShapeDrawable, var minLat:Double,
    var maxLat:Double, var minLon:Double, var maxLon:Double)

fun initCanvas(imageView: ImageView, resources:Resources):CanvasWidget  {
    val bitmap = createBitmap(320, 250, Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    var shapeDrawable = ShapeDrawable(RectShape())
    shapeDrawable.setBounds(0, 0, 320, 250)
    shapeDrawable.draw(canvas)
    imageView.background = BitmapDrawable(resources, bitmap)
    shapeDrawable.paint.color = Color.parseColor("#6eba6e")
    shapeDrawable.paint.strokeWidth = 2.0f
    shapeDrawable.paint.isAntiAlias = true
    return CanvasWidget(canvas, shapeDrawable, 0.0, 0.0, 0.0, 0.0)
}

fun drawRun(canvasWidget: CanvasWidget, trackPoints: List<TrackPoint>) {
    val canvas = canvasWidget.canvas
    val shapeDrawable = canvasWidget.shapeDrawable
    val minLat = trackPoints.minBy { p -> p.lat }!!.lat
    val maxLat = trackPoints.maxBy { p -> p.lat }!!.lat
    val minLon = trackPoints.minBy { p -> p.lon }!!.lon
    val maxLon = trackPoints.maxBy { p -> p.lon }!!.lon

    for(p in trackPoints)
    {
        val lat = (p.lat - minLat) / (maxLat - minLat)
        val lon = (p.lon - minLon) / (maxLon - minLon)
        canvas.drawPoint(10 + 300 * lat.toFloat(),
                         10 + 230 * lon.toFloat(),
            shapeDrawable.paint)
    }
    canvasWidget.minLat = minLat; canvasWidget.maxLat = maxLat
    canvasWidget.minLon = minLon; canvasWidget.maxLon = maxLon
}

fun overlaySplit(canvasWidget: CanvasWidget, splitTrackPoints:List<TrackPoint>) {
    val canvas = canvasWidget.canvas
    val shapeDrawable = canvasWidget.shapeDrawable
    val minLat = canvasWidget.minLat; val maxLat = canvasWidget.maxLat
    val minLon = canvasWidget.minLon; val maxLon = canvasWidget.maxLon

    shapeDrawable.paint.strokeWidth = 5.0f
    shapeDrawable.paint.color = Color.parseColor("#013220")

    for(p in splitTrackPoints)
    {
        val lat = (p.lat - minLat) / (maxLat - minLat)
        val lon = (p.lon - minLon) / (maxLon - minLon)
        canvas.drawPoint(
            10 + 300 * lat.toFloat(),
            10 + 230 * lon.toFloat(),
            shapeDrawable.paint)
    }
    shapeDrawable.paint.strokeWidth = 2.0f
}

fun initSplitSpinner(trackPoints: List<TrackPoint>, context:Context, splitSpinner: Spinner, splitsTable: TableLayout ) {
    splitSpinner.adapter = ArrayAdapter.createFromResource(
        context,
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
            fillDistSplitsTable(trackPoints, context, splitsTable, splitSpinner)
        }

        override fun onNothingSelected(parent: AdapterView<*>) { }
    }
}


fun readSplitSpinner(spinner:Spinner):Int {
    return Integer.parseInt(spinner.selectedItem.toString().split("m")[0])
}

fun fillDistSplitsTable(trackPoints: List<TrackPoint>, applicationContext: Context, splitsTable:TableLayout, splitSpinner:Spinner):List<Split> {
    val context = applicationContext
    val splitSize = readSplitSpinner(splitSpinner)
    while(splitsTable.childCount > 1)  // remove the existing rows except the header row
        splitsTable.removeView(splitsTable.getChildAt(splitsTable.childCount -1))

    var i=1;
    val splits = distSplits(splitSize, trackPoints)
    val startTrack = trackPoints.first()
    val endTrack = trackPoints.last()

    for (split in splits) {
        val row = TableRow(context)
        row.isClickable = true
        var textView = TextView(context)
        textView.text = ordinal(i++)
        textView.setPadding(10, 0, 0, 0)
        row.addView(textView, 0)

        textView = TextView(context)
        textView.text = formatTime(split.time)
        row.addView(textView, 1)

        textView = TextView(context)
        textView.setPadding(35, 0, 0, 0)
        textView.text = formatTime(split.time.multipliedBy((1000/splitSize).toLong())) + "/km"
        row.addView(textView, 2)

        textView = TextView(context)
        textView.text = if (split.avgHR > 0) split.avgHR.toString() +" bpm" else "n/a"
        textView.setPadding(35, 0, 0,0)
        row.addView(textView, 3)

        textView = TextView(context)
        textView.text = formatTime(Duration.between(startTrack.time, split.endTrackPoint.time))
        textView.setPadding(45, 0, 0,0)
        row.addView(textView, 4)

        textView = TextView(context)
        textView.setPadding(35, 0, 0, 0)
        textView.text = "%.2f".format(Math.min(Math.ceil(split.endTrackPoint.dist/splitSize*splitSize),
                                               endTrack.dist)/1000.0) + "km"
        row.addView(textView, 5)

        if (i % 2 == 0) row.setBackgroundColor(Color.parseColor("#6eba6e"))

        splitsTable.addView(row)
    }
    return splits
}
