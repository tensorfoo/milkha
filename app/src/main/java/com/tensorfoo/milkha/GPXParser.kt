package com.manna.milkha

import java.io.InputStream
import java.io.Serializable
import java.lang.Double.parseDouble
import java.lang.Integer.parseInt
import java.time.LocalDateTime
import java.time.temporal.Temporal

data class TrackPoint(val lat:Double, val lon:Double, val time: Temporal, val HR:Int, var dist:Double) :Serializable

    // distance between two trackpoints using Haversine formula:
    fun dist(p1: TrackPoint, p2: TrackPoint): Double {
        return dist(p1.lat, p1.lon, p2.lat, p2.lon)
    }

    fun dist(p:TrackPoint, lat:Double, lon:Double): Double {
       return dist(p.lat, p.lon, lat, lon)
    }

    fun dist(p1lat:Double, p1lon:Double, p2lat:Double, p2lon:Double): Double {
        val R = 6371e3 // metres
        val x = (p1lat - p2lat)
        val y = (p1lon - p2lon)* Math.cos(Math.toRadians(p1lat))
        return Math.sqrt(x*x + y*y) * R*Math.PI/180
    }

    fun parse(inputStream: InputStream):List<TrackPoint> {
        val s = inputStream.bufferedReader().use { it.readText() }
        var i= s.indexOf("lon=") + 5; var j= i
        var prevTrackPoint:TrackPoint? = null
        var results = ArrayList<TrackPoint>()
        var initialized = false
        lateinit var trackPoint:TrackPoint

        while(true) {
            j = s.indexOf('\"', i+1)
            val lon = parseDouble(s.substring(i, j))

            i = s.indexOf('\"', j+1)
            j = s.indexOf('\"', i+1)
            val lat = parseDouble(s.substring(i+1, j))

            i = j+7
            j = s.indexOf('<', i+8)
            val ele = parseDouble(s.substring(i, j))

            i = j+12 // skip characters to the start of the time field
            j = s.indexOf('<', i+1)
            val time = LocalDateTime.parse(s.substring(i, j-1))

            j+= 8; var hr = 0
            if (s[j] == 'e') //  HR extension present
            {
                i = j + 50
                j = s.indexOf('<', i+1)
                hr = parseInt(s.substring(i, j))
                j += 63 // skip ahead to the end of the track
            }
            else
                j +=8  // skip ahead to the end of the track

            if (s[j] == 't') // look ahead to see if we have another track?
                i = j+11 // yes. skip ahead to the next lon field
            else
                break // no more tracks left

            if (initialized)
                trackPoint = TrackPoint(lat, lon, time, hr,
                    prevTrackPoint!!.dist + dist(prevTrackPoint!!, lat, lon))
            else{
                trackPoint = TrackPoint(lat, lon, time, hr, 0.0)
                initialized = true
            }

            results.add(trackPoint)
            prevTrackPoint = trackPoint
        }
        return results
    }

