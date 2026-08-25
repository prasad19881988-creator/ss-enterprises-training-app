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

    private var remoteRenderer: SurfaceViewRenderer? = null
    private var localRenderer: SurfaceViewRenderer? = null

    private var isConnecting = false
    private var videoScreenShown = false

    /*
     * LiveKit room name
     */
    private val trainingRoomName =
        "ss-enterprises-training"

    /*
     * Token backend
     */
    private val tokenEndpoint =
        "https://ss-enterprises-training-backend.onrender.com/token"

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

                    if (room.state ==
                        Room.State.DISCONNECTED
                    ) {
                        return@launch
                    }

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

        /*
         * Start listening BEFORE connecting.
         * This is important so remote participant/video events
         * are not missed during connection.
         */
        startRoomEventListener()

        createHomeScreen()
    }

    /*
     * Home screen
     */
    private fun createHomeScreen() {

        videoScreenShown = false

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
        if (room.state !=
            Room.State.DISCONNECTED
        ) {

            showVideoScreen()

            return
        }

        /*
         * Permissions
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
                 * Token source
                 */
                val tokenSource =
                    TokenSource.fromEndpoint(
                        tokenEndpoint,
                        "POST"
                    )

                /*
                 * Get LiveKit token
                 */
                val response =
                    tokenSource.fetch(
                        TokenRequestOptions(
                            roomName =
                                trainingRoomName
                        )
                    ).getOrThrow()

                /*
                 * Connect to LiveKit
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
                 * First create video screen.
                 */
                showVideoScreen()

                /*
                 * Attach our local camera.
                 */
                attachLocalVideo()

                /*
                 * Attach any remote participant
                 * that was already present.
                 */
                attachExistingRemoteVideos()

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
     * LiveKit room events
     */
    private fun startRoomEventListener() {

        lifecycleScope.launch {

            room.events.collect { event ->

                when (event) {

                    /*
                     * A participant joined.
                     */
                    is RoomEvent.ParticipantConnected -> {

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity,
                                "Participant joined",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        attachParticipantVideo(
                            event.participant
                        )
                    }

                    /*
                     * A participant's video track
                     * became available.
                     */
                    is RoomEvent.TrackSubscribed -> {

                        val track =
                            event.track

                        if (track is VideoTrack) {

                            attachRemoteTrack(track)
                        }
                    }

                    /*
                     * Remote participant left.
                     */
                    is RoomEvent.ParticipantDisconnected -> {

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity,
                                "Participant left",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    else -> {
                        // Other LiveKit events are not needed here.
                    }
                }
            }
        }
    }

    /*
     * Attach local camera
     */
    private fun attachLocalVideo() {

        val renderer =
            localRenderer ?: return

        val localTrack =
            room.localParticipant
                .getTrackPublication(
                    Track.Source.CAMERA
                )
                ?.track as? LocalVideoTrack

        if (localTrack != null) {

            localTrack.addRenderer(
                renderer
            )
        }
    }

    /*
     * Attach already connected participants
     */
    private fun attachExistingRemoteVideos() {

        for (participant in
            room.remoteParticipants.values
        ) {

            attachParticipantVideo(
                participant
            )
        }
    }

    /*
     * Attach participant's camera
     */
    private fun attachParticipantVideo(
        participant: io.livekit.android.room.participant.RemoteParticipant
    ) {

        val track =
            participant
                .getTrackPublication(
                    Track.Source.CAMERA
                )
                ?.track as? VideoTrack

        if (track != null) {

            attachRemoteTrack(track)
        }
    }

    /*
     * Attach remote track to remote renderer
     */
    private fun attachRemoteTrack(
        track: VideoTrack
    ) {

        runOnUiThread {

            val renderer =
                remoteRenderer ?: return@runOnUiThread

            try {

                track.addRenderer(
                    renderer
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Remote video error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /*
     * Video call screen
     */
    private fun showVideoScreen() {

        if (videoScreenShown) {
            return
        }

        videoScreenShown = true

        val root =
            FrameLayout(this)

        /*
         * Remote video
         */
        val newRemoteRenderer =
            SurfaceViewRenderer(this)

        remoteRenderer =
            newRemoteRenderer

        room.initVideoRenderer(
            newRemoteRenderer
        )

        root.addView(
            newRemoteRenderer,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * Local preview
         */
        val newLocalRenderer =
            SurfaceViewRenderer(this)

        localRenderer =
            newLocalRenderer

        room.initVideoRenderer(
            newLocalRenderer
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
            newLocalRenderer,
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

        controls.addView(
            muteButton
        )

        controls.addView(
            screenButton
        )

        controls.addView(
            leaveButton
        )

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
         * Controls
         */
        muteButton.setOnClickListener {

            toggleMute()
        }

        screenButton.setOnClickListener {

            startScreenShare()
        }

        leaveButton.setOnClickListener {

            leaveTraining()
        }
    }

    /*
     * Mute / Unmute microphone
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

        remoteRenderer = null
        localRenderer = null
        videoScreenShown = false

        createHomeScreen()

        Toast.makeText(
            this,
            "Training ended",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * Check permissions
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
