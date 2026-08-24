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
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var room: Room

    /*
     * Screen capture result
     */
    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    this@MainActivity,
                    "Screen sharing cancelled",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            val data = result.data

            if (data == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Screen capture data not available",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }

            lifecycleScope.launch {

                try {

                    room.localParticipant.setScreenShareEnabled(
                        true,
                        ScreenCaptureParams(data)
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

    /*
     * Camera + microphone permission
     */
    private val requestPermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted =
                permissions[Manifest.permission.CAMERA] == true

            val audioGranted =
                permissions[Manifest.permission.RECORD_AUDIO] == true

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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        /*
         * Create LiveKit room
         */
        room = LiveKit.create(applicationContext)

        /*
         * Main layout
         */
        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setPadding(
            40,
            60,
            40,
            40
        )

        /*
         * Title
         */
        val title = TextView(this)

        title.text = "SS Enterprises Training"

        title.textSize = 28f

        /*
         * Video button
         */
        val videoButton = Button(this)

        videoButton.text = "Start Video Training"

        /*
         * Screen share button
         */
        val screenButton = Button(this)

        screenButton.text = "Share Screen"

        /*
         * Mute button
         */
        val muteButton = Button(this)

        muteButton.text = "Mute / Unmute"

        /*
         * Admin button
         */
        val adminButton = Button(this)

        adminButton.text = "Admin Panel"

        /*
         * Add views
         */
        layout.addView(title)

        layout.addView(videoButton)

        layout.addView(screenButton)

        layout.addView(muteButton)

        layout.addView(adminButton)

        setContentView(layout)

        /*
         * Video training
         */
        videoButton.setOnClickListener {

            connectToTraining()
        }

        /*
         * Mute / unmute
         */
        muteButton.setOnClickListener {

            toggleMute()
        }

        /*
         * Screen share
         */
        screenButton.setOnClickListener {

            startScreenShare()
        }

        /*
         * Admin panel
         */
        adminButton.setOnClickListener {

            Toast.makeText(
                this,
                "Admin Panel - Coming Soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
     * Connect to LiveKit training room
     */
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
                 * Token endpoint
                 *
                 * Keep GET here because this is the endpoint
                 * currently configured in your project.
                 */
                val tokenSource =
                    TokenSource.fromEndpoint(
                        "https://ss-enterprises-training-backend.onrender.com/token",
                        "GET"
                    )

                /*
                 * Fetch LiveKit credentials
                 *
                 * LiveKit returns Result<TokenSourceResponse>,
                 * so getOrThrow() is required.
                 */
                val response =
                    tokenSource.fetch(
                        TokenRequestOptions(
                            roomName = "ss-enterprises-training"
                        )
                    ).getOrThrow()

                /*
                 * Connect using server URL + participant token
                 */
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

    /*
     * Mute / unmute microphone
     */
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

    /*
     * Start Android screen capture permission dialog
     */
    private fun startScreenShare() {

        if (!hasPermissions()) {

            Toast.makeText(
                this,
                "Please allow camera and microphone permission first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * User must be connected before sharing screen
         */
        if (!room.localParticipant.isMicrophoneEnabled &&
            !room.localParticipant.isCameraEnabled
        ) {

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

    /*
     * Check camera + microphone permissions
     */
    private fun hasPermissions(): Boolean {

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val audioGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        return cameraGranted && audioGranted
    }

    override fun onDestroy() {

        if (::room.isInitialized) {

            room.disconnect()

            room.release()
        }

        super.onDestroy()
    }
}
