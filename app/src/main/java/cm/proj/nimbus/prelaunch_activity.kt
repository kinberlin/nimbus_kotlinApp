package cm.proj.nimbus

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import cm.proj.nimbus.place.Place
import cm.proj.nimbus.place.PlacesReader
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*

class prelaunch_activity : AppCompatActivity() {

    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }

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

        var textView = findViewById<TextView>(R.id.txt_hour)

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
        var trajet = listTrajet[id!!]
        depart_txt.text = trajet.departName
        arrival_txt.text = trajet.arrivalName
        var button = findViewById<Button>(R.id.button)
        button.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("trajet", id)
            startActivity(intent)
            finish()
        })


    }
}