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
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)



        var textView = findViewById<TextView>(R.id.txt_hour1)

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

        var videoView = findViewById<VideoView>(R.id.videoView1)

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
        var button = findViewById<Button>(R.id.button1)
        button.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        })

    }
}