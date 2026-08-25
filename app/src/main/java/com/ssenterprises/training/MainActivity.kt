package com.ssenterprises.training

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource

import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var room: Room

    private lateinit var mainLayout: LinearLayout

    private lateinit var remoteRenderer: SurfaceViewRenderer
    private lateinit var localRenderer: SurfaceViewRenderer

    private var isConnecting = false

    /*
     * Screen sharing permission
     */
    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    this,
                    "Screen sharing cancelled",
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            val data = result.data

            if (data == null) {
                Toast.makeText(
                    this,
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
    private val permissionLauncher =
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

        room = LiveKit.create(applicationContext)

        createHomeScreen()
    }

    /*
     * Simple home screen for now
     */
    private fun createHomeScreen() {

        mainLayout = LinearLayout(this)

        mainLayout.orientation =
            LinearLayout.VERTICAL

        mainLayout.setPadding(
            40,
            60,
            40,
            40
        )

        val title = TextView(this)

        title.text =
            "SS Enterprises Training"

        title.textSize = 28f

        title.gravity =
            Gravity.CENTER

        val videoButton =
            Button(this)

        videoButton.text =
            "START VIDEO TRAINING"

        val screenButton =
            Button(this)

        screenButton.text =
            "SHARE SCREEN"

        val muteButton =
            Button(this)

        muteButton.text =
            "MUTE / UNMUTE"

        mainLayout.addView(title)

        mainLayout.addView(
            videoButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        mainLayout.addView(
            screenButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        mainLayout.addView(
            muteButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(mainLayout)

        /*
         * Start video
         */
        videoButton.setOnClickListener {

            connectToTraining()
        }

        /*
         * Screen share
         */
        screenButton.setOnClickListener {

            startScreenShare()
        }

        /*
         * Mute
         */
        muteButton.setOnClickListener {

            toggleMute()
        }
    }

    /*
     * Connect to LiveKit
     */
    private fun connectToTraining() {

        /*
         * Prevent double click / duplicate connect
         */
        if (isConnecting) {

            Toast.makeText(
                this,
                "Connecting, please wait...",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * Already connected
         */
        if (room.state != Room.State.DISCONNECTED) {

            Toast.makeText(
                this,
                "Already connected to training",
                Toast.LENGTH_SHORT
            ).show()

            showVideoScreen()

            return
        }

        /*
         * Check permissions
         */
        if (!hasPermissions()) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )

            return
        }

        isConnecting = true

        lifecycleScope.launch {

            try {

                /*
                 * Token server
                 */
                val tokenSource =
                    TokenSource.fromEndpoint(
                        "https://ss-enterprises-training-backend.onrender.com/token",
                        "POST"
                    )

                /*
                 * Get token
                 */
                val response =
                    tokenSource.fetch(
                        TokenRequestOptions(
                            roomName =
                                "ss-enterprises-training"
                        )
                    ).getOrThrow()

                /*
                 * Connect
                 */
                room.connect(
                    url = response.serverUrl,
                    token = response.participantToken,
                    options = ConnectOptions(
                        audio = true,
                        video = true
                    )
                )

                /*
                 * Show video screen
                 */
                showVideoScreen()

                /*
                 * Attach local camera
                 */
                val localTrack =
                    room.localParticipant
                        .getTrackPublication(
                            Track.Source.CAMERA
                        )
                        ?.track as? LocalVideoTrack

                if (localTrack != null) {

                    localTrack.addRenderer(
                        localRenderer
                    )
                }

                /*
                 * Check existing remote participant
                 */
                val remoteTrack =
                    room.remoteParticipants
                        .values
                        .firstOrNull()
                        ?.getTrackPublication(
                            Track.Source.CAMERA
                        )
                        ?.track as? VideoTrack

                if (remoteTrack != null) {

                    remoteTrack.addRenderer(
                        remoteRenderer
                    )
                }

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

            } finally {

                isConnecting = false
            }
        }
    }

    /*
     * Listen for remote video
     */
    private fun listenForRemoteVideo() {

        lifecycleScope.launch {

            room.events.collect { event ->

                when (event) {

                    is RoomEvent.TrackSubscribed -> {

                        val track =
                            event.track

                        if (track is VideoTrack) {

                            track.addRenderer(
                                remoteRenderer
                            )
                        }
                    }

                    else -> {
                    }
                }
            }
        }
    }

    /*
     * Video call screen
     */
    private fun showVideoScreen() {

        val root =
            FrameLayout(this)

        /*
         * Remote video
         */
        remoteRenderer =
            SurfaceViewRenderer(this)

        room.initVideoRenderer(
            remoteRenderer
        )

        root.addView(
            remoteRenderer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * Local small video
         */
        localRenderer =
            SurfaceViewRenderer(this)

        room.initVideoRenderer(
            localRenderer
        )

        val localParams =
            FrameLayout.LayoutParams(
                320,
                480
            )

        localParams.gravity =
            Gravity.TOP or Gravity.END

        localParams.setMargins(
            0,
            40,
            20,
            0
        )

        root.addView(
            localRenderer,
            localParams
        )

        /*
         * Bottom controls
         */
        val controls =
            LinearLayout(this)

        controls.orientation =
            LinearLayout.HORIZONTAL

        controls.gravity =
            Gravity.CENTER

        val muteButton =
            Button(this)

        muteButton.text =
            "Mute"

        val screenButton =
            Button(this)

        screenButton.text =
            "Share"

        val leaveButton =
            Button(this)

        leaveButton.text =
            "Leave"

        controls.addView(muteButton)

        controls.addView(screenButton)

        controls.addView(leaveButton)

        val controlsParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

        controlsParams.gravity =
            Gravity.BOTTOM

        root.addView(
            controls,
            controlsParams
        )

        setContentView(root)

        /*
         * Start listening for remote video
         */
        listenForRemoteVideo()

        /*
         * Mute
         */
        muteButton.setOnClickListener {

            toggleMute()
        }

        /*
         * Share screen
         */
        screenButton.setOnClickListener {

            startScreenShare()
        }

        /*
         * Leave
         */
        leaveButton.setOnClickListener {

            leaveTraining()
        }
    }

    /*
     * Mute / Unmute
     */
    private fun toggleMute() {

        if (room.state ==
            Room.State.DISCONNECTED
        ) {

            Toast.makeText(
                this,
                "Please start video training first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val enabled =
                    room.localParticipant
                        .isMicrophoneEnabled

                room.localParticipant
                    .setMicrophoneEnabled(
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
     * Start screen sharing
     */
    private fun startScreenShare() {

        if (room.state ==
            Room.State.DISCONNECTED
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
            ) as MediaProjectionManager

        screenCaptureLauncher.launch(
            mediaProjectionManager
                .createScreenCaptureIntent()
        )
    }

    /*
     * Leave training
     */
    private fun leaveTraining() {

        if (room.state !=
            Room.State.DISCONNECTED
        ) {

            room.disconnect()
        }

        createHomeScreen()

        Toast.makeText(
            this,
            "Training ended",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * Permission check
     */
    private fun hasPermissions(): Boolean {

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) ==
                PackageManager.PERMISSION_GRANTED

        val audioGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) ==
                PackageManager.PERMISSION_GRANTED

        return cameraGranted &&
                audioGranted
    }

    override fun onDestroy() {

        if (::room.isInitialized) {

            if (room.state !=
                Room.State.DISCONNECTED
            ) {

                room.disconnect()
            }

            room.release()
        }

        super.onDestroy()
    }
}
