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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

// Create a Handler and a Runnable to launch the new activity after 5 seconds
        Handler().postDelayed({
            val intent = Intent(this, prelaunch_activity::class.java)
            startActivity(intent)
            finish()
        }, 7000) // 5000 milliseconds = 5 seconds
    }
}