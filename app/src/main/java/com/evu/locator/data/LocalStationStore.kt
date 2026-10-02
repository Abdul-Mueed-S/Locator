package com.evu.locator.data

import android.content.Context
import com.evu.locator.data.models.Station
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object LocalStationStore {
    private const val PREFS_NAME = "locator_local_stations"
    private const val KEY_STATIONS = "stations_json"

    fun getAll(context: Context): List<Station> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_STATIONS, null) ?: return emptyList()
        val arr = JSONArray(json)
        val list = mutableListOf<Station>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Station(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    radius = obj.getDouble("radius").toFloat(),
                    route = null,
                    overridesCloudId = if (obj.has("overridesCloudId") && !obj.isNull("overridesCloudId"))
                        obj.getString("overridesCloudId") else null
                )
            )
        }
        return list
    }

    fun saveAll(context: Context, stations: List<Station>) {
        val arr = JSONArray()
        stations.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("lat", s.lat)
            obj.put("lng", s.lng)
            obj.put("radius", s.radius.toDouble())
            if (s.overridesCloudId != null) obj.put("overridesCloudId", s.overridesCloudId)
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATIONS, arr.toString()).apply()
    }

    fun add(context: Context, station: Station): Station {
        val withId = if (station.id.isBlank()) station.copy(id = UUID.randomUUID().toString()) else station
        val current = getAll(context).toMutableList()
        current.add(withId)
        saveAll(context, current)
        return withId
    }

    fun update(context: Context, station: Station) {
        val current = getAll(context).toMutableList()
        val idx = current.indexOfFirst { it.id == station.id }
        if (idx >= 0) {
            current[idx] = station
            saveAll(context, current)
        }
    }

    fun delete(context: Context, id: String) {
        val current = getAll(context).toMutableList()
        current.removeAll { it.id == id }
        saveAll(context, current)
    }

    fun getById(context: Context, id: String): Station? =
        getAll(context).find { it.id == id }
}