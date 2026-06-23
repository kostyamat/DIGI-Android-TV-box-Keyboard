package com.kostyamat.r2r_q

import org.json.JSONObject

data class LayoutModel(
    val id: String,
    val name: String,
    val shortName: String,
    val map: Map<Int, Pair<String, String>>
) {
    companion object {
        fun fromJson(jsonString: String): LayoutModel {
            val json = JSONObject(jsonString)
            val id = json.getString("id")
            val name = json.getString("name")
            val shortName = json.getString("shortName")
            val mapJson = json.getJSONObject("map")

            val map = mutableMapOf<Int, Pair<String, String>>()
            val keys = mapJson.keys()
            while (keys.hasNext()) {
                val keyCodeStr = keys.next()
                val pairArray = mapJson.getJSONArray(keyCodeStr)
                map[keyCodeStr.toInt()] = Pair(pairArray.getString(0), pairArray.getString(1))
            }

            return LayoutModel(id, name, shortName, map)
        }
    }
}
