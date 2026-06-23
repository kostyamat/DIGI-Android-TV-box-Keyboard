package com.kostyamat.r2r_q

import org.json.JSONObject

data class KeyTranslation(
    val normal: String,
    val shift: String,
    val altGr: String? = null,
    val altGrShift: String? = null
)

data class LayoutModel(
    val id: String,
    val name: String,
    val shortName: String,
    val map: Map<Int, KeyTranslation>
) {
    companion object {
        fun fromJson(jsonString: String): LayoutModel {
            val json = JSONObject(jsonString)
            val id = json.getString("id")
            val name = json.getString("name")
            val shortName = json.getString("shortName")
            val mapJson = json.getJSONObject("map")

            val map = mutableMapOf<Int, KeyTranslation>()
            val keys = mapJson.keys()
            while (keys.hasNext()) {
                val keyCodeStr = keys.next()
                val array = mapJson.getJSONArray(keyCodeStr)
                val normal = array.getString(0)
                val shift = array.getString(1)
                val altGr = if (array.length() > 2) array.getString(2) else null
                val altGrShift = if (array.length() > 3) array.getString(3) else null
                
                map[keyCodeStr.toInt()] = KeyTranslation(normal, shift, altGr, altGrShift)
            }

            return LayoutModel(id, name, shortName, map)
        }
    }
}
