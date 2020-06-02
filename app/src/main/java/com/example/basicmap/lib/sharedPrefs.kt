package com.example.basicmap.lib

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.example.basicmap.ui.drones.Drone
import com.example.basicmap.ui.places.LivePlace
import com.example.basicmap.ui.places.Place
import com.google.gson.GsonBuilder

private val SHARED_PREF = "sharedPrefs"
private val DRONES = "saved_drones"
private val PLACES = "saved_places"

fun saveDrones(context: Context, drones: List<Drone>?): String? {
    val sharedPref: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF, AppCompatActivity.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPref.edit()
    val gson = GsonBuilder().create()
    val json = gson.toJson(drones)
    editor.putString(DRONES, json)
    editor.apply()
    return json
}

fun loadDrones(context: Context): MutableList<Drone> {
    val sharedPref: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF, AppCompatActivity.MODE_PRIVATE)
    val gson = GsonBuilder().create()
    val json = sharedPref.getString(DRONES, null)
    val drones = gson.fromJson(json, Array<Drone>::class.java) ?: return mutableListOf()
    return drones.toMutableList()
}

fun loadPlaces(context: Context): MutableList<LivePlace> {
    val sharedPrefPlaces: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF, AppCompatActivity.MODE_PRIVATE)
    val gson = GsonBuilder().create()
    val json = sharedPrefPlaces.getString(PLACES, null)
    val places = gson.fromJson(json, Array<Place>::class.java) ?: return mutableListOf()
    val livePlaces = places.map {
        val livePlace = LivePlace(context)
        livePlace.place.value = it
        livePlace
    }
    return livePlaces.toMutableList()
}

fun savePlaces(context: Context, livePlaces: List<LivePlace>): String? {
    val places = livePlaces.map {
        it.place.value
    }
    val sharedPref: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF, AppCompatActivity.MODE_PRIVATE)
    val editor: SharedPreferences.Editor = sharedPref.edit()
    val gson = GsonBuilder().create()
    val json = gson.toJson(places)
    editor.putString(PLACES, json)
    editor.apply()
    return json
}
