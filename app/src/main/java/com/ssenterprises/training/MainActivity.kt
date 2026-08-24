package com.ssenterprises.training

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var room: Room

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {

                val data = result.data!!

                lifecycleScope.launch {

                    try {

                        room.localParticipant.setScreenShareEnabled(
                            true,
                            data
                        )

                        Toast.makeText(
                            this@MainActivity,
                            "Screen sharing started",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@MainActivity,
                            "Screen share error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        room = LiveKit.create(applicationContext)

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            40,
            60,
            40,
            40
        )

        val title = TextView(this)

        title.text = "SS Enterprises Training"

        title.textSize = 28f

        val videoButton = Button(this)

        videoButton.text = "Start Video Training"

        val screenButton = Button(this)

        screenButton.text = "Share Screen"

        val muteButton = Button(this)

        muteButton.text = "Mute / Unmute"

        val adminButton = Button(this)

        adminButton.text = "Admin Panel"

        layout.addView(title)

        layout.addView(videoButton)

        layout.addView(screenButton)

        layout.addView(muteButton)

        layout.addView(adminButton)

        setContentView(layout)

        videoButton.setOnClickListener {

            connectToTraining()
        }

        muteButton.setOnClickListener {

            toggleMute()
        }

        screenButton.setOnClickListener {

            startScreenShare()
        }

        adminButton.setOnClickListener {

            Toast.makeText(
                this,
                "Admin Panel - Coming Soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun connectToTraining() {

        if (!hasPermissions()) {

            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )

            return
        }

        lifecycleScope.launch {

            try {

                /*
                 * Our Render backend token endpoint
                 */
                val tokenSource =
                    io.livekit.android.token.TokenSource.fromEndpoint(
                        "https://ss-enterprises-training-backend.onrender.com/token",
                        "GET"
                    )

                val response =
                    tokenSource.fetch(
                        io.livekit.android.token.TokenRequestOptions(
                            roomName = "ss-enterprises-training"
                        )
                    )

                room.connect(
                    url = response.serverUrl,
                    token = response.participantToken,
                    options = ConnectOptions(
                        audio = true,
                        video = true
                    )
                )

                Toast.makeText(
                    this@MainActivity,
                    "Connected to training room",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Connection failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun toggleMute() {

        lifecycleScope.launch {

            try {

                val enabled =
                    room.localParticipant.isMicrophoneEnabled

                room.localParticipant.setMicrophoneEnabled(
                    !enabled
                )

                Toast.makeText(
                    this@MainActivity,
                    if (enabled)
                        "Microphone muted"
                    else
                        "Microphone unmuted",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Microphone error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startScreenShare() {

        if (!hasPermissions()) {

            Toast.makeText(
                this,
                "Please start video training first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val mediaProjectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as android.media.projection.MediaProjectionManager

        screenCaptureLauncher.launch(
            mediaProjectionManager.createScreenCaptureIntent()
        )
    }

    private fun hasPermissions(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

                &&

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted =
                permissions[
                    Manifest.permission.CAMERA
                ] == true

            val audioGranted =
                permissions[
                    Manifest.permission.RECORD_AUDIO
                ] == true

            if (cameraGranted && audioGranted) {

                connectToTraining()

            } else {

                Toast.makeText(
                    this,
                    "Camera and microphone permission required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onDestroy() {

        room.disconnect()

        room.release()

        super.onDestroy()
    }
}
