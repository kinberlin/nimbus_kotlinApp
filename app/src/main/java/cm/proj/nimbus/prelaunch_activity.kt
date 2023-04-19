package cm.proj.nimbus

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cm.proj.nimbus.place.Place
import cm.proj.nimbus.place.PlacesReader
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class prelaunch_activity : AppCompatActivity() {

    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    private val fileName = "services.nimbus"
    private val fileName2 = "activity.nimbus"
    var state = 0;
    private var file: File = File(fileName)
    private var file2: File = File(fileName2)
    // Get the Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()
    var times = Times()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prelaunch)
        var listTrajet : MutableList<Trajet> = mutableListOf(
            Trajet(places[0],places[1],"Elf Axe Lourd", "Salle des fêtes d'Akwa, Douala"),
            Trajet(places[0],places[2],"Elf Axe Lourd", "Carrefour Dallip"),
            Trajet(places[5],places[4],"Ndokoti", "Poste Centrale de Bonanjo"),
            Trajet(places[5],places[3],"Ndokoti", "Délégation Régionale Des PTT Bonanjo"),
            Trajet(places[6],places[4],"Bonabéri", "Poste Centrale de Bonanjo"),
            Trajet(places[6],places[5],"Bonabéri", "Ndokoti"),
            Trajet(places[6],places[7],"Bonabéri", "Marché Centrale de Douala"),
            Trajet(places[10],places[9],"Carrefour des douanes du Cameroun", "PK 14"),
            Trajet(places[5],places[9],"Ndokoti", "PK 14")
        )
        file = File(this@prelaunch_activity.getFilesDir(), fileName)
        var textView = findViewById<TextView>(R.id.txt_hour)
        var button = findViewById<Button>(R.id.button)
        button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                textView.text = sdf.format(Date())
                handler.postDelayed(this, 1000) // 1000 milliseconds = 1 second
            }
        }
        handler.postDelayed(runnable, 0)

        //Video Play

        var videoView = findViewById<VideoView>(R.id.videoView)

        // Set MediaController to enable play, pause, seek operations
        var mediaController: MediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // Point the VideoView to our video source
        val videoFile = "android.resource://" + packageName + "/" + R.raw.location
        videoView.setVideoURI(Uri.parse(videoFile))

        // Start the video
        videoView.start()

        // Set the Repeat video.
        val job = CoroutineScope(Dispatchers.IO).launch {
            Log.d("TAG", "Launched Coroutine")
            while (isActive) {
                videoView.seekTo(0)
                videoView.start()
                delay(3000)
            }
        }

        var id:Int?=null

        // checking if the intent has extra
        if(intent.hasExtra("trajet")){
            // get the Serializable data model class with the details in it
            (intent.getSerializableExtra("trajet") as Int).also { id = it }
        }
        var depart_txt = findViewById<TextView>(R.id.prelaunch_departure_txt)
        var arrival_txt = findViewById<TextView>(R.id.prelaunch_arrival_txt)
        var viewmap = findViewById<TextView>(R.id.view_map)
        viewmap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("trajet", id)
            startActivity(intent)
            //finish()
        }
        var mySpinner: Spinner = findViewById(R.id.mySpinner)
        var trajet = listTrajet[id!!]
        depart_txt.text = trajet.departName
        arrival_txt.text = trajet.arrivalName
        var stringl : MutableList<String> = mutableListOf(trajet.departName, trajet.arrivalName)
        var spinnerArrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item, stringl
        )
        mySpinner.adapter = spinnerArrayAdapter
        button.setOnClickListener(View.OnClickListener {
            if(button.text == "Start Service"){
                var s = Service(id!!, times.getDate(),times.getHour(),mySpinner.selectedItem.toString(),"Moving")
                //Saving service data to the database
                if (!checkFile(file)) {
                    if (addData(s, "service")) {
                        button.text = "Arrived"
                        button.setBackgroundColor(resources.getColor(R.color.green))
                    } else {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Network Error")
                        builder.setMessage("Please Check your Internet Connection and retry")
                        builder.setPositiveButton("OK", null)
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
                else{
                    var ids = writeFile("")
                    if (updateDocument(ids, s, "service")) {
                        button.text = "Arrived"
                        button.setBackgroundColor(resources.getColor(R.color.green))
                    } else {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Network Error")
                        builder.setMessage("Please Check your Internet Connection and retry")
                        builder.setPositiveButton("OK", null)
                        val dialog = builder.create()
                        dialog.show()
                    }

                }
            }
            else {
                var ndep = ""
                var l = mySpinner.selectedItemPosition
                if(l==0){
                    var temp = stringl[0]
                    stringl[0] = stringl[1]
                    stringl[1] = temp
                    ndep = stringl[0]
                }
                else{
                    var temp = stringl[1]
                    stringl[1] = stringl[0]
                    stringl[0] = temp
                    ndep=stringl[1]
                }
                spinnerArrayAdapter.notifyDataSetChanged()
                var s = Service(id!!, times.getDate(),times.getHour(),ndep,"Parked")
                var ids = writeFile("")
                if (updateDocument(ids, s, "service")) {
                    button.text = "Start Service"
                    button.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Network Error")
                    builder.setMessage("Please Check your Internet Connection and retry")
                    builder.setPositiveButton("OK", null)
                    val dialog = builder.create()
                    dialog.show()
                }
            }
        })
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

    fun addData(location: Any, collectionName: String) : Boolean{
        var success : Boolean = false
        val collectionRef = db.collection(collectionName)
            collectionRef.add(location)
                .addOnSuccessListener {
                    println("Data added successfully!")
                    writeFile(it.id)
                    success= true
                }
                .addOnFailureListener { e ->
                    println("Error adding data: $e")
                    success = false
                }
        return success
    }
    fun updateDocument(id: String, newDocument: Any, collectionName: String) : Boolean{
        val documentRef = db.collection(collectionName).document(id)
        var success = false

        documentRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {

                documentRef.set(newDocument!!).addOnSuccessListener {
                    println("Document updated successfully")
                    success = true
                }.addOnFailureListener { e ->
                    println("Error updating document")
                    success = false
                    e.printStackTrace()
                }
            } else {
                println("Document does not exist")
                success = false
            }
        }.addOnFailureListener { e ->
            println("Error getting document")
            e.printStackTrace()
            success = false
        }
        return success
    }
}