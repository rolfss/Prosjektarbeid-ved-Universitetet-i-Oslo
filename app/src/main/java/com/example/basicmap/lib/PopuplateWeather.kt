package com.example.basicmap.lib

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.RadioButton
import androidx.core.text.bold
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basicmap.R
import com.example.basicmap.ui.places.HourlyWeatherListAdapter
import com.example.basicmap.ui.places.LivePlace
import kotlinx.android.synthetic.main.hourly_weather.view.*
import kotlinx.android.synthetic.main.weather.view.*
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun degToCompass(a : Double) : String {
    var b =((a/22.5)+.5).toInt()
    var c = arrayOf("N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW")
    return c[(b % 16)]
}

/*
    Hook up a view with weather.xml layout so it reacts to the various data in @LivePlace

    @param container View with weather.xml layout
 */
fun setupWeatherElement(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    livePlace: LivePlace,
    container: View
) {
    var dayToId: Map<DayOfWeek, Int>? = null
    var idToDay: Map<Int, LocalDate>? = null
    var now = LocalDate.now()
    fun setupRadioGroup() {
        now = LocalDate.now()
        dayToId = mapOf(
            now.dayOfWeek to R.id.today,
            now.plusDays(1).dayOfWeek to R.id.tommorow,
            now.plusDays(2).dayOfWeek to R.id.third,
            now.plusDays(3).dayOfWeek to R.id.fourth,
            now.plusDays(4).dayOfWeek to R.id.fifth,
            now.plusDays(5).dayOfWeek to R.id.sixth,
            now.plusDays(6).dayOfWeek to R.id.seventh
        )
        idToDay = mapOf(
            R.id.today to now,
            R.id.tommorow to now.plusDays(1),
            R.id.third to now.plusDays(2),
            R.id.fourth to now.plusDays(3),
            R.id.fifth to now.plusDays(4),
            R.id.sixth to now.plusDays(5),
            R.id.seventh to now.plusDays(6)
        )

        dayToId!!.forEach {
            val day = it.key
            container.findViewById<RadioButton>(it.value).text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }

        val day = livePlace.day.value!!
        // Select the correct day
        if (day.isBefore(now)) {
            container.dayBar.check(dayToId!!.get(day.dayOfWeek)!!)
        } else {
            container.dayBar.check(dayToId!!.get(now.dayOfWeek)!!)
        }
    }

    setupRadioGroup()
    livePlace.clock.observe(lifecycleOwner, Observer {
        // Redo the radio buttons if it's gone a day since it was last done.
        if (now.dayOfWeek == LocalDate.now().dayOfWeek)
            return@Observer
        setupRadioGroup()
    })

    fun populateWeather(weather: Met.Kall) {
        val utc = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))
        val timeseries = weather.properties.timeseries

        val days = mapOf(
            DayOfWeek.MONDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.TUESDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.WEDNESDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.THURSDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.FRIDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.SATURDAY to mutableListOf<Met.Numb>(),
            DayOfWeek.SUNDAY to mutableListOf<Met.Numb>()
        )
        val now = ZonedDateTime.now()
        for (data in timeseries) {
            val time = data.time

            val temp = utc.parse(time)
            val inst = Instant.from(temp)
            val date = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault())


            if (date.isAfter(now.plusDays(6)))
                break

            days[date.dayOfWeek]?.add(data)
        }

        val day = livePlace.day.value!!
        val dayTimeseries = days[day.dayOfWeek]!!

        // Calculate line of sight at 12:00 or at the current time if the current day is selected.
        for (data in dayTimeseries) {
            val time = data.time
            val temp = utc.parse(time)
            val inst = Instant.from(temp)
            val date = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault())

            if (date.hour == 12 || date.dayOfWeek == day.dayOfWeek) {
                val fog = data.data.instant.details.fog_area_fraction
                val lowClouds = data.data.instant.details.cloud_area_fraction_low
                val mediumClouds = data.data.instant.details.cloud_area_fraction_medium
                val highClouds = data.data.instant.details.cloud_area_fraction_high

                if (fog != null && lowClouds != null && mediumClouds != null && highClouds != null) {

                    val fogFloat = fog.toFloat()
                    val lowCloudsFloat = lowClouds.toFloat()
                    val mediumCloudsFloat = mediumClouds.toFloat()
                    val highCloudsFloat = highClouds.toFloat()

                    val combinedFraction =
                        fogFloat + lowCloudsFloat + mediumCloudsFloat + highCloudsFloat
                    val visibility = combinedFraction / 4

                    val visibilityText = 15 * (100-visibility) / 100
                    val visibilityFinalValue = visibilityText.roundToInt()

                    container.visibilityValue.text = "${visibilityFinalValue} km"
                    break
                }
            }
        }

        val viewAdapter = HourlyWeatherListAdapter(context, dayTimeseries)
        container.hourScrollView.adapter = viewAdapter
    }

    livePlace.day.observe(lifecycleOwner, Observer {
        val weather = livePlace.weather.value
        if (weather == null)
            return@Observer

        populateWeather(weather)
    })

    container.hourScrollView.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    livePlace.weather.observe(lifecycleOwner, Observer {
        if (it == null)
            return@Observer
        
        populateWeather(it)
    })

    livePlace.address.observe(lifecycleOwner, Observer {
        if (it == "") {
            container.locationNameView.text = context.getString(R.string.PW_mangelpaainfo)
            //container.locationNameView.text = "Ingen addresseinformasjon tilgjengelig"
        }
        else
            container.locationNameView.text = it
    })

    livePlace.astronomicalData.observe(lifecycleOwner, Observer {

        val rawSunData = livePlace.astronomicalData.value?.location?.time

        if (rawSunData != null) {
            for (data in rawSunData) {
                val utc = data.sunrise?.time
                val utc2 = data.sunset?.time
                if (utc != null && utc2 != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SSSZ")
                    val sdf2 = SimpleDateFormat("HH:mm")
                    val sunriseEpoch = sdf.parse(utc).time
                    val sunriseSeconds = sdf.parse(utc).time / 1000
                    val sunsetEpoch = sdf.parse(utc2).time
                    val sunsetSeconds = sdf.parse(utc2).time / 1000
                    val sunriseDate = Date(sunriseEpoch)
                    val sunsetDate = Date(sunsetEpoch)
                    val timenow = Instant.now().epochSecond

                    val timeSinceRise = (timenow - sunriseSeconds).toDouble()
                    val sunFullPeriod = (sunsetSeconds - sunriseSeconds).toDouble()

                    val percent = timeSinceRise / sunFullPeriod * 100

                    val sunriseHours = sdf2.format(sunriseDate)
                    val sunsetHours = sdf2.format(sunsetDate)

                    container.sunRiseValue.text = sunriseHours.toString()
                    container.sunSetValue.text = sunsetHours.toString()

                    container.sunGraph.setOnTouchListener(object : View.OnTouchListener {
                        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                            return true
                        }
                    })

                    if (percent > 0 && percent < 100) {
                        container.sunGraph.thumb.alpha = 255
                        container.sunGraph.progress = percent.toInt()
                    } else {
                        container.sunGraph.thumb.alpha = 0
                        container.sunGraph.progress = 0
                    }
                }
            }
        }
    })

    if (container.dayBar.checkedRadioButtonId == -1) {
        container.dayBar.check(dayToId!!.get(livePlace.day.value?.dayOfWeek) ?: -1)
    }
    container.dayBar.setOnCheckedChangeListener { group, checkedId ->
        if (checkedId < 0)
            return@setOnCheckedChangeListener
        livePlace.day.value = idToDay!!.get(checkedId) as LocalDate
    }

}

