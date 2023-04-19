package cm.proj.nimbus

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cm.proj.nimbus.place.Place
import cm.proj.nimbus.place.PlacesReader
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    lateinit var listTrajet: MutableList<Trajet>
    private lateinit var map: GoogleMap
    private val fileName = "location.nimbus"
    private val fileName2 = "activity.nimbus"
    var state = 0;
    private var file: File = File(fileName)
    private var file2: File = File(fileName2)
    val UPDATE_INTERVAL = 10000L // milliseconds
    val MAP_ZOOM_LEVEL = 15f
    var countoffline = 1
    var id: Int? = null
    lateinit var activities : Activity

    // Get the Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()
    val activity = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        //initialise loal storage file
        listTrajet = mutableListOf(
            Trajet(places[0], places[1], "Elf Axe Lourd", "Salle des fêtes d'Akwa, Douala"),
            Trajet(places[0], places[2], "Elf Axe Lourd", "Carrefour Dallip"),
            Trajet(places[5], places[4], "Ndokoti", "Poste Centrale de Bonanjo"),
            Trajet(places[5], places[3], "Ndokoti", "Délégation Régionale Des PTT Bonanjo"),
            Trajet(places[6], places[4], "Bonabéri", "Poste Centrale de Bonanjo"),
            Trajet(places[6], places[5], "Bonabéri", "Ndokoti"),
            Trajet(places[6], places[7], "Bonabéri", "Marché Centrale de Douala"),
            Trajet(places[10], places[9], "Carrefour des douanes du Cameroun", "PK 14"),
            Trajet(places[5], places[9], "Ndokoti", "PK 14")
        )
        file = File(this@MapsActivity.getFilesDir(), fileName)
        file2 = File(this@MapsActivity.getFilesDir(), fileName2)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Check if the ACCESS_FINE_LOCATION permission was granted before requesting a location
        if (ContextCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If the permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
        }
        // checking if the intent has extra
        if (intent.hasExtra("trajet")) {
            // get the Serializable data model class with the details in it
            (intent.getSerializableExtra("trajet") as Int).also { id = it }
        }

        setupLocClient()
    }

    private lateinit var fusedLocClient: FusedLocationProviderClient
    // use it to request location updates and get the latest location

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap //initialise map
        getCurrentLocation()
        startUpdatingLocation(this, map)
    }

    private fun setupLocClient() {
        fusedLocClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    // prompt the user to grant/deny access
    private fun requestLocPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION), //permission in the manifest
            REQUEST_LOCATION
        )
    }

    companion object {
        private const val REQUEST_LOCATION =
            1 //request code to identify specific permission request
        private const val TAG = "MapsActivity" // for debugging
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // call requestLocPermissions() if permission isn't granted
            requestLocPermissions()
        } else {
            fusedLocClient.lastLocation.addOnCompleteListener {
                // lastLocation is a task running in the background
                val location = it.result //obtain location
                //reference to the database
                // val database: FirebaseDatabase = FirebaseDatabase.getInstance()
                // val ref: DatabaseReference = database.getReference("users")
                if (location != null) {
                    Log.d(TAG, "Location is not null")

                    //Add the data to the "users" collection with an auto-generated document ID
                    val latLng = LatLng(location.latitude, location.longitude)

                    //Recent position textView Field
                    var updst = findViewById<TextView>(R.id.txt_updateStatus)
                    // create a marker at the exact location
                    if (state == 1) {
                        map.addMarker(
                            MarkerOptions().position(latLng)
                                .title("Initial Position!") //You are currently here
                        )
                    } else {
                        /* map.addMarker(
                             MarkerOptions().position(latLng)
                                 .title("You are now here!")
                         )*/
                        if (state == 2) {
                            map.addMarker(
                                MarkerOptions().position(listTrajet[id!!].depart.latLng)
                                    .title("Bus Drop Point!")
                            )
                            map.addMarker(
                                MarkerOptions().position(listTrajet[id!!].arrival.latLng)
                                    .title("Bus Drop Point!")
                            )
                            drawRouteOnMap(
                                listTrajet[id!!].depart.latLng,
                                listTrajet[id!!].arrival.latLng,
                                map
                            )
                            // create an object that will specify how the camera will be updated
                            //val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                            val update =
                                CameraUpdateFactory.newLatLngZoom(
                                    listTrajet[id!!].depart.latLng,
                                    10.0f
                                )
                            //Log.d("Firebase", location.toString())
                            map.moveCamera(update)
                        }
                        var current = getDateTime()
                        countoffline = 0
                        updst.text = "Last Known position was on : $current"
                        Log.d(TAG, current)
                    }


                    //Save the location data to the database
                    if (!checkFile(file)) {
                        addData(location, "buses",1)
                    } else {
                        var ids = writeFile("")
                        updateDocument(ids, location, "buses")
                    }
                    //Save the activity data to the database
                    activities = Activity(id!!,getDateTime() , writeFile(""))
                    if (!checkFile(file2)) {
                        addData(activities, "activity",2)
                    } else {
                        var ids = writeFile2("")
                        updateDocument(ids, activities, "activity")
                    }
                } else {
                    // if location is null , log an error message
                    Log.d(TAG, "Location is null")
                    if (countoffline == 3) {
                        val intent = Intent(this, ErrorActivity::class.java)
                        intent.putExtra("trajet", id!!)
                        startActivity(intent)
                        finish()
                    }
                    countoffline += 1
                }

            }
        }
    }
    fun getDateTime() : String{
        // Getting latest recent position time and date
        var time = Calendar.getInstance().time
        var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return formatter.format(time)
    }

    fun addData(location: Any, collectionName: String, item : Int) {
        val collectionRef = db.collection(collectionName)
        if(item ==1){
        collectionRef.add(location)
            .addOnSuccessListener {
                println("Data added successfully!")
                writeFile(it.id)
            }
            .addOnFailureListener { e ->
                println("Error adding data: $e")
            }}
        else if(item ==2){
            collectionRef.add(location)
                .addOnSuccessListener {
                    println("Data added successfully!")
                    writeFile2(it.id)
                }
                .addOnFailureListener { e ->
                    println("Error adding data: $e")
                }}
    }

    fun updateDocument(id: String, newDocument: Any, collectionName: String) {
        val documentRef = db.collection(collectionName).document(id)

        documentRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {

                documentRef.set(newDocument!!).addOnSuccessListener {
                    println("Document updated successfully")
                }.addOnFailureListener { e ->
                    println("Error updating document")
                    e.printStackTrace()
                }
            } else {
                println("Document does not exist")
            }
        }.addOnFailureListener { e ->
            println("Error getting document")
            e.printStackTrace()
        }
    }

    fun checkFile(files: File): Boolean {
        return (files.exists())
    }

    fun writeFile(text: String): String {
        // check if file exists

        if (checkFile(file)) {
            // read text from file
            val texte = file.readText()
            println("File exists. Text from file: ${texte}")
            return texte
        } else {
            // create new file and write text to it
            file.createNewFile()
            file.writeText(text)
            println("File created. Text written to file: ${text}")
            return text
        }
    }
    fun writeFile2(text: String): String {
        // check if file exists
        if (checkFile(file2)) {
            // read text from file
            val texte = file2.readText()
            println("File exists. Text from file: ${texte}")
            return texte
        } else {
            // create new file and write text to it
            file2.createNewFile()
            file2.writeText(text)
            println("File created. Text written to file: ${text}")
            return text
        }
    }

    // Function to draw a road on Google Map between two points
    fun drawRouteOnMap(origin: LatLng, destination: LatLng, googleMap: GoogleMap) {
        val endpointBuilder = StringBuilder()
        endpointBuilder.append("https://maps.googleapis.com/maps/api/directions/json?")
        endpointBuilder.append("origin=${origin.latitude},${origin.longitude}&")
        endpointBuilder.append("destination=${destination.latitude},${destination.longitude}&")
        endpointBuilder.append("key=AIzaSyC5rTG-2EgEQNPQpvlo5zwEh6_5sncUero")

        val url = endpointBuilder.toString()
        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonResponse = JSONObject(response.body()?.string())
                    val status = jsonResponse.getString("status")
                    Log.d(TAG, status)
                    Log.d(TAG, endpointBuilder.toString())
                    if (status == "OK") {
                        val routesArray = jsonResponse.getJSONArray("routes")
                        val route = routesArray.getJSONObject(0)
                        val overviewPolyline = route.getJSONObject("overview_polyline")

                        val encodedString = overviewPolyline.getString("points")
                        val pointsList = PolyUtil.decode(encodedString)

                        val polylineOptions = PolylineOptions()
                        polylineOptions.addAll(pointsList)
                        polylineOptions.color(Color.BLUE)
                        polylineOptions.width(10f)
                        activity.runOnUiThread(java.lang.Runnable { map.addPolyline(polylineOptions) })
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.d(TAG, e.message!!)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.d(TAG, e.message!!)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //check if the request code matches the REQUEST_LOCATION
        if (requestCode == REQUEST_LOCATION) {
            //check if grantResults contains PERMISSION_GRANTED.If it does, call getCurrentLocation()
            if (grantResults.size == 1 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {
                getCurrentLocation()
            } else {
                //if it doesn`t log an error message
                Log.e(TAG, "Location permission has been denied")
            }
        }
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with file operations
            } else {
                // Permission denied, show explanation or handle failure gracefully
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startUpdatingLocation(context: Context, map: GoogleMap) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val job = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Launched Coroutine")
            while (isActive) {
                getCurrentLocation()
                state += 1
                Log.d(TAG, "Set up")
                delay(UPDATE_INTERVAL)
            }
        }
    }
}