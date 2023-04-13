package cm.proj.nimbus

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val fileName = "location.nimbus"
    var state = 1;
    private var file: File = File(fileName)
    val UPDATE_INTERVAL = 10000L // milliseconds
    val MAP_ZOOM_LEVEL = 15f

    // Get the Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        //initialise loal storage file
        file = File(this@MapsActivity.getFilesDir(), fileName)
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
                    // Getting latest recent position time and date
                    var time = Calendar.getInstance().time
                    var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
                    var updst = findViewById<TextView>(R.id.txt_updateStatus)
                    var current = formatter.format(time)

                    //Add the data to the "users" collection with an auto-generated document ID
                    val latLng = LatLng(location.latitude, location.longitude)
                    // create a marker at the exact location
                    if(state ==1){
                    map.addMarker(
                        MarkerOptions().position(latLng)
                            .title("Initial Position!") //You are currently here
                    )}else{
                        map.addMarker(
                            MarkerOptions().position(latLng)
                                .title("You are now here!")

                        )
                        updst.text = "Last Known position was on : $current"
                        Log.d(TAG, current)
                    }

                    // create an object that will specify how the camera will be updated
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    //Log.d("Firebase", location.toString())
                    map.moveCamera(update)
                    //Save the location data to the database
                    // ref.setValue(location)
                    if (!checkFile()) {
                        addData(location, "buses")
                    } else {
                        var ids = writeFile("")
                        updateDocument(ids, location, "buses")
                    }
                } else {
                    // if location is null , log an error message
                    Log.d(TAG, "Location is null")

                }


            }
        }
    }

    fun addData(location: Location, collectionName: String) {
        val collectionRef = db.collection(collectionName)
        collectionRef.add(location)
            .addOnSuccessListener {
                println("Data added successfully!")
                writeFile(it.id)
            }
            .addOnFailureListener { e ->
                println("Error adding data: $e")
            }
    }

    fun updateDocument(id: String, newLocation: Location, collectionName: String) {
        val documentRef = db.collection(collectionName).document(id)

        documentRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {

                documentRef.set(newLocation!!).addOnSuccessListener {
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

    fun checkFile(): Boolean {
        return (file.exists())
    }

    fun writeFile(text: String): String {
        // check if file exists

        if (checkFile()) {
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
                state =2
                Log.d(TAG, "Set up")
                delay(UPDATE_INTERVAL)
            }
        }
    }

    fun updateMapMarker(map: GoogleMap, latLng: LatLng) {
        Log.d(TAG, latLng.toString())
        map.clear()
        map.addMarker(MarkerOptions().position(latLng))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM_LEVEL))
    }
}