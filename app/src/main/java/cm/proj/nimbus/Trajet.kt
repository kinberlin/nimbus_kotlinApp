package cm.proj.nimbus

import cm.proj.nimbus.place.Place

data class Trajet(val depart: Place, val arrival: Place, val departName: String, val arrivalName: String ) :java.io.Serializable
data class Activity(val trajet : Int, val dates : String, val bus : String) :java.io.Serializable
