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
            if (!json.has("id") || json.getString("id").isBlank()) throw IllegalArgumentException("Missing or empty 'id'")
            if (!json.has("name") || json.getString("name").isBlank()) throw IllegalArgumentException("Missing or empty 'name'")
            if (!json.has("shortName") || json.getString("shortName").isBlank()) throw IllegalArgumentException("Missing or empty 'shortName'")
            if (!json.has("map")) throw IllegalArgumentException("Missing 'map' object")

            val id = json.getString("id")
            val name = json.getString("name")
            val shortName = json.getString("shortName")
            val mapJson = json.getJSONObject("map")

            if (mapJson.length() == 0) throw IllegalArgumentException("'map' cannot be empty")

            val map = mutableMapOf<Int, KeyTranslation>()
            val keys = mapJson.keys()
            while (keys.hasNext()) {
                val keyCodeStr = keys.next()
                val keyCode = keyCodeStr.toIntOrNull() ?: throw IllegalArgumentException("Key code '$keyCodeStr' is not a valid integer")
                
                val array = mapJson.optJSONArray(keyCodeStr) ?: throw IllegalArgumentException("Value for key '$keyCodeStr' must be an array")
                if (array.length() == 0) throw IllegalArgumentException("Array for key '$keyCodeStr' cannot be empty")

                val normal = array.getString(0)
                val shift = if (array.length() > 1) array.getString(1) else normal
                val altGr = if (array.length() > 2) array.getString(2) else null
                val altGrShift = if (array.length() > 3) array.getString(3) else null
                
                map[keyCode] = KeyTranslation(normal, shift, altGr, altGrShift)
            }

            return LayoutModel(id, name, shortName, map)
        }
    }
}
