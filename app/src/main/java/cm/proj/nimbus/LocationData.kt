package cm.proj.nimbus

import android.location.Location

data class LocationData(val latitude: Double = 0.0, val longitude: Double = 0.0)

data class User(val uid: String = "", val name: String = "", val location: Location? = null)
