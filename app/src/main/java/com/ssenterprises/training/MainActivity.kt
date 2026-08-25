package com.ssenterprises.training

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private var isHindi = false

    private val navy = Color.rgb(5, 30, 65)
    private val darkBlue = Color.rgb(2, 18, 42)
    private val blue = Color.rgb(0, 105, 220)
    private val green = Color.rgb(0, 155, 85)
    private val gold = Color.rgb(238, 180, 25)
    private val purple = Color.rgb(105, 45, 210)
    private val cyan = Color.rgb(0, 150, 210)
    private val white = Color.WHITE
    private val lightText = Color.rgb(220, 230, 245)

    private val trainingRoomName =
        "ss-enterprises-training"

    private val tokenEndpoint =
        "https://ss-enterprises-training-backend.onrender.com/token"


    // =========================================================
    // SCREEN SHARE PERMISSION
    // =========================================================

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                toast(
                    if (isHindi)
                        "स्क्रीन शेयर रद्द किया गया"
                    else
                        "Screen sharing cancelled"
                )
                return@registerForActivityResult
            }

            val data = result.data

            if (data == null) {
                toast(
                    if (isHindi)
                        "स्क्रीन डेटा उपलब्ध नहीं है"
                    else
                        "Screen capture data not available"
                )
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {

                    if (room.state == Room.State.DISCONNECTED) {
                        return@launch
                    }

                    room.localParticipant.setScreenShareEnabled(
                        true,
                        ScreenCaptureParams(data)
                    )

                    toast(
                        if (isHindi)
                            "स्क्रीन शेयर शुरू हो गया"
                        else
                            "Screen sharing started"
                    )

                } catch (e: Exception) {

                    toast(
                        if (isHindi)
                            "स्क्रीन शेयर में समस्या"
                        else
                            "Screen share error"
                    )
                }
            }
        }


    // =========================================================
    // CAMERA + MICROPHONE PERMISSION
    // =========================================================

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

                toast(
                    if (isHindi)
                        "कैमरा और माइक्रोफोन की अनुमति जरूरी है"
                    else
                        "Camera and microphone permission required"
                )
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        room = LiveKit.create(applicationContext)

        startRoomEventListener()

        createHomeScreen()
    }


    // =========================================================
    // HOME SCREEN
    // =========================================================

    private fun createHomeScreen() {

        videoScreenShown = false

        val scroll = ScrollView(this)

        scroll.setBackgroundColor(darkBlue)

        mainLayout = LinearLayout(this)

        mainLayout.orientation =
            LinearLayout.VERTICAL

        mainLayout.setPadding(
            dp(18),
            dp(15),
            dp(18),
            dp(25)
        )


        // -----------------------------------------------------
        // TOP BAR
        // -----------------------------------------------------

        val topBar = LinearLayout(this)

        topBar.orientation =
            LinearLayout.HORIZONTAL

        topBar.gravity =
            Gravity.CENTER_VERTICAL


        val brand = TextView(this)

        brand.text =
            "SS ENTERPRISES"

        brand.textSize = 19f

        brand.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        brand.setTextColor(gold)

        brand.gravity =
            Gravity.CENTER_VERTICAL


        topBar.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                dp(50),
                1f
            )
        )


        val languageButton =
            TextView(this)

        languageButton.text =
            if (isHindi)
                "English"
            else
                "हिंदी"

        languageButton.textSize = 14f

        languageButton.setTextColor(white)

        languageButton.gravity =
            Gravity.CENTER

        languageButton.background =
            strokeBackground(
                Color.TRANSPARENT,
                gold,
                25f,
                2
            )

        languageButton.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        languageButton.setOnClickListener {

            isHindi = !isHindi

            createHomeScreen()
        }


        topBar.addView(
            languageButton,
            LinearLayout.LayoutParams(
                dp(90),
                dp(42)
            )
        )


        mainLayout.addView(topBar)


        // -----------------------------------------------------
        // LOGO
        // -----------------------------------------------------

        val logo =
            ImageView(this)

        logo.setImageResource(
            R.drawable.a_clean_professional_logo_branding_graphic_on_a_de
        )

        logo.scaleType =
            ImageView.ScaleType.CENTER_INSIDE


        mainLayout.addView(
            logo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        )


        // -----------------------------------------------------
        // TRAINING APP TITLE
        // -----------------------------------------------------

        val title =
            TextView(this)

        title.text =
            "TRAINING APP"

        title.textSize = 23f

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        title.setTextColor(navy)

        title.gravity =
            Gravity.CENTER


        val titleBox =
            LinearLayout(this)

        titleBox.gravity =
            Gravity.CENTER

        titleBox.background =
            roundedBackground(
                gold,
                35f
            )

        titleBox.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )


        mainLayout.addView(
            titleBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {

                setMargins(
                    dp(15),
                    0,
                    dp(15),
                    dp(12)
                )
            }
        )


        // -----------------------------------------------------
        // TAGLINE
        // -----------------------------------------------------

        val tagline =
            TextView(this)

        tagline.text =
            if (isHindi)
                "आपकी सेवा में हमारी खुशी"
            else
                "Aapki Seva Me Hamari Khushi"

        tagline.textSize = 14f

        tagline.setTextColor(gold)

        tagline.gravity =
            Gravity.CENTER

        mainLayout.addView(
            tagline,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )


        // =====================================================
        // START VIDEO
        // =====================================================

        mainLayout.addView(
            homeCard(
                "🎥",
                if (isHindi)
                    "वीडियो प्रशिक्षण शुरू करें"
                else
                    "Start Video Training",

                if (isHindi)
                    "वीडियो और ऑडियो के साथ ट्रेनिंग रूम"
                else
                    "Join training room with video & audio",

                blue
            ) {

                connectToTraining()
            }
        )


        // =====================================================
        // SHARE SCREEN
        // =====================================================

        mainLayout.addView(
            homeCard(
                "🖥",
                if (isHindi)
                    "स्क्रीन शेयर करें"
                else
                    "Share Screen",

                if (isHindi)
                    "प्रतिभागियों के साथ स्क्रीन साझा करें"
                else
                    "Share your screen with participants",

                green
            ) {

                startScreenShare()
            }
        )


        // =====================================================
        // MUTE
        // =====================================================

        mainLayout.addView(
            homeCard(
                "🎙",
                if (isHindi)
                    "माइक बंद / चालू"
                else
                    "Mute / Unmute",

                if (isHindi)
                    "अपने माइक्रोफोन को नियंत्रित करें"
                else
                    "Control your microphone",

                gold,
                true
            ) {

                toggleMute()
            }
        )


        // =====================================================
        // ADMIN PANEL
        // =====================================================

        mainLayout.addView(
            homeCard(
                "👑",
                if (isHindi)
                    "एडमिन पैनल"
                else
                    "Admin Panel",

                if (isHindi)
                    "सेशन और प्रतिभागियों को मैनेज करें"
                else
                    "Manage sessions & participants",

                purple
            ) {

                showAdminPanel()
            }
        )


        // =====================================================
        // PARTICIPANTS
        // =====================================================

        mainLayout.addView(
            homeCard(
                "👥",
                if (isHindi)
                    "प्रतिभागी"
                else
                    "Participants",

                if (isHindi)
                    "ट्रेनिंग रूम के प्रतिभागी देखें"
                else
                    "View training room participants",

                cyan
            ) {

                showParticipants()
            }
        )


        // =====================================================
        // FOOTER
        // =====================================================

        val footer =
            TextView(this)

        footer.text =
            if (isHindi)
                "\n✦  साथ मिलकर आगे बढ़ें  ✦\n\n" +
                        "👥 बेहतर सीखना     📈 मजबूत टीम     🤝 उज्जवल भविष्य\n\n" +
                        "© 2025 SS Enterprises • आपकी सेवा में हमारी खुशी"
            else
                "\n✦  TOGETHER WE GROW  ✦\n\n" +
                        "👥 Better Learning     📈 Strong Team     🤝 Brighter Future\n\n" +
                        "© 2025 SS Enterprises • Aapki Seva Me Hamari Khushi"

        footer.textSize = 13f

        footer.setTextColor(lightText)

        footer.gravity =
            Gravity.CENTER

        footer.setPadding(
            dp(5),
            dp(25),
            dp(5),
            dp(10)
        )

        mainLayout.addView(footer)


        scroll.addView(mainLayout)

        setContentView(scroll)
    }


    // =========================================================
    // HOME CARD
    // =========================================================

    private fun homeCard(
        icon: String,
        title: String,
        subtitle: String,
        color: Int,
        darkText: Boolean = false,
        action: () -> Unit
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(12),
            dp(10),
            dp(8),
            dp(10)
        )

        card.background =
            strokeBackground(
                color,
                Color.argb(
                    190,
                    255,
                    255,
                    255
                ),
                30f,
                2
            )


        val iconView =
            TextView(this)

        iconView.text =
            icon

        iconView.textSize =
            30f

        iconView.gravity =
            Gravity.CENTER

        iconView.background =
            strokeBackground(
                Color.argb(
                    30,
                    255,
                    255,
                    255
                ),
                Color.argb(
                    150,
                    255,
                    255,
                    255
                ),
                50f,
                2
            )


        card.addView(
            iconView,
            LinearLayout.LayoutParams(
                dp(70),
                dp(70)
            )
        )


        val textBox =
            LinearLayout(this)

        textBox.orientation =
            LinearLayout.VERTICAL

        textBox.setPadding(
            dp(15),
            0,
            dp(5),
            0
        )


        val titleView =
            TextView(this)

        titleView.text =
            title

        titleView.textSize =
            19f

        titleView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )


        val subtitleView =
            TextView(this)

        subtitleView.text =
            subtitle

        subtitleView.textSize =
            13f


        val titleColor =
            if (darkText)
                navy
            else
                white


        titleView.setTextColor(
            titleColor
        )

        subtitleView.setTextColor(
            if (darkText)
                Color.DKGRAY
            else
                lightText
        )


        textBox.addView(titleView)

        textBox.addView(
            subtitleView
        )


        card.addView(
            textBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )


        val arrow =
            TextView(this)

        arrow.text =
            "›"

        arrow.textSize =
            40f

        arrow.gravity =
            Gravity.CENTER

        arrow.setTextColor(
            titleColor
        )


        card.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(35),
                dp(70)
            )
        )


        card.setOnClickListener {
            action()
        }


        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(112)
            ).apply {

                setMargins(
                    0,
                    dp(7),
                    0,
                    dp(7)
                )
            }


        return card
    }


    // =========================================================
    // ADMIN PANEL
    // =========================================================

    private fun showAdminPanel() {

        val scroll =
            ScrollView(this)

        scroll.setBackgroundColor(
            darkBlue
        )


        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(18),
            dp(20),
            dp(18),
            dp(25)
        )


        root.addView(
            pageHeader(
                if (isHindi)
                    "👑  एडमिन पैनल"
                else
                    "👑  ADMIN PANEL"
            )
        )


        root.addView(
            infoCard(
                "👥",
                if (isHindi)
                    "Participants"
                else
                    "Participants",

                if (room.state ==
                    Room.State.DISCONNECTED
                )
                    if (isHindi)
                        "अभी कोई सेशन शुरू नहीं है"
                    else
                        "Session is not active"
                else
                    "${room.remoteParticipants.size} participant(s) connected",

                blue
            )
        )


        root.addView(
            adminButton(
                if (isHindi)
                    "🎥  वीडियो प्रशिक्षण शुरू करें"
                else
                    "🎥  Start Video Training",
                blue
            ) {

                connectToTraining()
            }
        )


        root.addView(
            adminButton(
                if (isHindi)
                    "👥  Participants देखें"
                else
                    "👥  View Participants",
                green
            ) {

                showParticipants()
            }
        )


        root.addView(
            adminButton(
                if (isHindi)
                    "🖥  Screen Share"
                else
                    "🖥  Share Screen",
                cyan
            ) {

                startScreenShare()
            }
        )


        root.addView(
            adminButton(
                if (isHindi)
                    "🎙  Mute / Unmute"
                else
                    "🎙  Mute / Unmute",
                gold,
                true
            ) {

                toggleMute()
            }
        )


        root.addView(
            adminButton(
                if (isHindi)
                    "⛔  Training समाप्त करें"
                else
                    "⛔  End Training",
                purple
            ) {

                leaveTraining()
            }
        )


        val language =
            Button(this)

        language.text =
            if (isHindi)
                "English"
            else
                "हिंदी"

        language.setTextColor(
            white
        )

        language.background =
            strokeBackground(
                Color.TRANSPARENT,
                gold,
                25f,
                2
            )

        language.setOnClickListener {

            isHindi = !isHindi

            showAdminPanel()
        }


        root.addView(
            language,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {

                setMargins(
                    0,
                    dp(18),
                    0,
                    dp(8)
                )
            }
        )


        val back =
            Button(this)

        back.text =
            if (isHindi)
                "← होम पर वापस जाएँ"
            else
                "← Back to Home"

        back.setTextColor(
            white
        )

        back.background =
            strokeBackground(
                Color.TRANSPARENT,
                gold,
                25f,
                2
            )

        back.setOnClickListener {
            createHomeScreen()
        }


        root.addView(
            back,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )


        scroll.addView(root)

        setContentView(scroll)
    }


    // =========================================================
    // PARTICIPANTS
    // =========================================================

    private fun showParticipants() {

        val scroll =
            ScrollView(this)

        scroll.setBackgroundColor(
            darkBlue
        )


        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            dp(18),
            dp(20),
            dp(18),
            dp(25)
        )


        root.addView(
            pageHeader(
                if (isHindi)
                    "👥  प्रतिभागी"
                else
                    "👥  PARTICIPANTS"
            )
        )


        val count =
            if (room.state ==
                Room.State.DISCONNECTED
            )
                0
            else
                room.remoteParticipants.size


        root.addView(
            infoCard(
                "👥",
                if (isHindi)
                    "कुल प्रतिभागी"
                else
                    "Total Participants",

                "$count",

                blue
            )
        )


        if (count == 0) {

            root.addView(
                infoCard(
                    "👤",
                    if (isHindi)
                        "अभी कोई प्रतिभागी नहीं"
                    else
                        "No participants yet",

                    if (isHindi)
                        "Training शुरू होने पर प्रतिभागी यहाँ दिखाई देंगे।"
                    else
                        "Participants will appear here after training starts.",

                    green
                )
            )

        } else {

            room.remoteParticipants.values.forEach {

                root.addView(
                    infoCard(
                        "👤",
                        it.identity?.value
                               ?: "Participant",

                        if (isHindi)
                            "Connected"
                        else
                            "Connected",

                        green
                    )
                )
            }
        }


        root.addView(
            adminButton(
                if (isHindi)
                    "＋  व्यक्ति जोड़ें"
                else
                    "＋  Add Person",
                purple
            ) {

                toast(
                    if (isHindi)
                        "नए व्यक्ति को Training Room link भेजें"
                    else
                        "Send the Training Room link to the new participant"
                )
            }
        )


        val back =
            Button(this)

        back.text =
            if (isHindi)
                "← होम पर वापस जाएँ"
            else
                "← Back to Home"

        back.setTextColor(
            white
        )

        back.background =
            strokeBackground(
                Color.TRANSPARENT,
                gold,
                25f,
                2
            )

        back.setOnClickListener {
            createHomeScreen()
        }


        root.addView(
            back,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {

                setMargins(
                    0,
                    dp(10),
                    0,
                    0
                )
            }
        )


        scroll.addView(root)

        setContentView(scroll)
    }


    // =========================================================
    // CONNECT TO LIVEKIT
    // =========================================================

    private fun connectToTraining() {

        if (isConnecting) {

            toast(
                if (isHindi)
                    "कनेक्ट हो रहा है..."
                else
                    "Connecting, please wait..."
            )

            return
        }


        if (room.state !=
            Room.State.DISCONNECTED
        ) {

            showVideoScreen()

            return
        }


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

                val tokenSource =
                    TokenSource.fromEndpoint(
                        tokenEndpoint,
                        "POST"
                    )


                val response =
                    tokenSource.fetch(
                        TokenRequestOptions(
                            roomName =
                                trainingRoomName
                        )
                    ).getOrThrow()


                room.connect(
                    url =
                        response.serverUrl,

                    token =
                        response.participantToken,

                    options =
                        ConnectOptions(
                            audio = true,
                            video = true
                        )
                )


                showVideoScreen()

                attachLocalVideo()

                attachExistingRemoteVideos()


                toast(
                    if (isHindi)
                        "Training Room से कनेक्ट हो गया"
                    else
                        "Connected to training room"
                )


            } catch (e: Exception) {

                toast(
                    if (isHindi)
                        "Connection failed"
                    else
                        "Connection failed: ${e.message}"
                )

            } finally {

                isConnecting = false
            }
        }
    }


    // =========================================================
    // ROOM EVENTS
    // =========================================================

    private fun startRoomEventListener() {

        lifecycleScope.launch {

            room.events.collect { event ->

                when (event) {

                    is RoomEvent.ParticipantConnected -> {

                        runOnUiThread {

                            toast(
                                if (isHindi)
                                    "Participant जुड़ा"
                                else
                                    "Participant joined"
                            )
                        }


                        attachParticipantVideo(
                            event.participant
                        )
                    }


                    is RoomEvent.TrackSubscribed -> {

                        val track =
                            event.track

                        if (track is VideoTrack) {

                            attachRemoteTrack(
                                track
                            )
                        }
                    }


                    is RoomEvent.ParticipantDisconnected -> {

                        runOnUiThread {

                            toast(
                                if (isHindi)
                                    "Participant बाहर गया"
                                else
                                    "Participant left"
                            )
                        }
                    }


                    else -> {
                    }
                }
            }
        }
    }


    // =========================================================
    // LOCAL VIDEO
    // =========================================================

    private fun attachLocalVideo() {

        val renderer =
            localRenderer
                ?: return


        val localTrack =
            room.localParticipant
                .getTrackPublication(
                    Track.Source.CAMERA
                )
                ?.track
                as? LocalVideoTrack


        if (localTrack != null) {

            localTrack.addRenderer(
                renderer
            )
        }
    }


    // =========================================================
    // REMOTE VIDEOS
    // =========================================================

    private fun attachExistingRemoteVideos() {

        for (participant in
            room.remoteParticipants.values
        ) {

            attachParticipantVideo(
                participant
            )
        }
    }


    private fun attachParticipantVideo(
        participant:
        io.livekit.android.room.participant.RemoteParticipant
    ) {

        val track =
            participant
                .getTrackPublication(
                    Track.Source.CAMERA
                )
                ?.track
                as? VideoTrack


        if (track != null) {

            attachRemoteTrack(
                track
            )
        }
    }


    private fun attachRemoteTrack(
        track: VideoTrack
    ) {

        runOnUiThread {

            val renderer =
                remoteRenderer
                    ?: return@runOnUiThread


            try {

                track.addRenderer(
                    renderer
                )

            } catch (e: Exception) {

                toast(
                    if (isHindi)
                        "Remote video error"
                    else
                        "Remote video error"
                )
            }
        }
    }


    // =========================================================
    // VIDEO SCREEN
    // =========================================================

    private fun showVideoScreen() {

        if (videoScreenShown) {
            return
        }

        videoScreenShown = true


        val root =
            FrameLayout(this)

        root.setBackgroundColor(
            Color.BLACK
        )


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


        val newLocalRenderer =
            SurfaceViewRenderer(this)

        localRenderer =
            newLocalRenderer


        room.initVideoRenderer(
            newLocalRenderer
        )


        val localParams =
            FrameLayout.LayoutParams(
                dp(130),
                dp(190)
            )


        localParams.gravity =
            Gravity.TOP or Gravity.END


        localParams.setMargins(
            0,
            dp(25),
            dp(15),
            0
        )


        root.addView(
            newLocalRenderer,
            localParams
        )


        val title =
            TextView(this)

        title.text =
            "SS ENTERPRISES • LIVE TRAINING"

        title.textSize =
            14f

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        title.setTextColor(
            white
        )

        title.gravity =
            Gravity.CENTER

        title.background =
            roundedBackground(
                Color.argb(
                    190,
                    5,
                    30,
                    65
                ),
                25f
            )

        title.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )


        val titleParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
            )


        titleParams.gravity =
            Gravity.TOP or Gravity.START


        titleParams.setMargins(
            dp(15),
            dp(20),
            0,
            0
        )


        root.addView(
            title,
            titleParams
        )


        // -----------------------------------------------------
        // BOTTOM CONTROLS
        // -----------------------------------------------------

        val controls =
            LinearLayout(this)

        controls.orientation =
            LinearLayout.HORIZONTAL

        controls.gravity =
            Gravity.CENTER

        controls.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        controls.background =
            roundedBackground(
                Color.argb(
                    220,
                    5,
                    30,
                    65
                ),
                35f
            )


        val mute =
            Button(this)

        mute.text =
            "🎙"

        mute.textSize =
            20f

        mute.setTextColor(
            white
        )

        mute.background =
            roundedBackground(
                navy,
                30f
            )


        val share =
            Button(this)

        share.text =
            "🖥"

        share.textSize =
            20f

        share.setTextColor(
            white
        )

        share.background =
            roundedBackground(
                green,
                30f
            )


        val leave =
            Button(this)

        leave.text =
            "✕"

        leave.textSize =
            20f

        leave.setTextColor(
            white
        )

        leave.background =
            roundedBackground(
                purple,
                30f
            )


        controls.addView(
            mute,
            LinearLayout.LayoutParams(
                dp(70),
                dp(58)
            )
        )

        controls.addView(
            share,
            LinearLayout.LayoutParams(
                dp(70),
                dp(58)
            )
        )

        controls.addView(
            leave,
            LinearLayout.LayoutParams(
                dp(70),
                dp(58)
            )
        )


        val controlsParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(78)
            )


        controlsParams.gravity =
            Gravity.BOTTOM or
                    Gravity.CENTER_HORIZONTAL


        controlsParams.setMargins(
            0,
            0,
            0,
            dp(20)
        )


        root.addView(
            controls,
            controlsParams
        )


        setContentView(root)


        mute.setOnClickListener {
            toggleMute()
        }


        share.setOnClickListener {
            startScreenShare()
        }


        leave.setOnClickListener {
            leaveTraining()
        }
    }


    // =========================================================
    // MUTE
    // =========================================================

    private fun toggleMute() {

        if (room.state ==
            Room.State.DISCONNECTED
        ) {

            toast(
                if (isHindi)
                    "पहले वीडियो प्रशिक्षण शुरू करें"
                else
                    "Please start video training first"
            )

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


                toast(
                    if (enabled)
                        if (isHindi)
                            "माइक्रोफोन बंद"
                        else
                            "Microphone muted"
                    else
                        if (isHindi)
                            "माइक्रोफोन चालू"
                        else
                            "Microphone unmuted"
                )

            } catch (e: Exception) {

                toast(
                    if (isHindi)
                        "माइक्रोफोन में समस्या"
                    else
                        "Microphone error"
                )
            }
        }
    }


    // =========================================================
    // SCREEN SHARE
    // =========================================================

    private fun startScreenShare() {

        if (room.state ==
            Room.State.DISCONNECTED
        ) {

            toast(
                if (isHindi)
                    "पहले वीडियो प्रशिक्षण शुरू करें"
                else
                    "Please start video training first"
            )

            return
        }


        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager


        screenCaptureLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }


    // =========================================================
    // LEAVE TRAINING
    // =========================================================

    private fun leaveTraining() {

        if (room.state !=
            Room.State.DISCONNECTED
        ) {

            room.disconnect()
        }


        remoteRenderer =
            null

        localRenderer =
            null

        videoScreenShown =
            false


        createHomeScreen()


        toast(
            if (isHindi)
                "Training समाप्त हो गई"
            else
                "Training ended"
        )
    }


    // =========================================================
    // ADMIN BUTTON
    // =========================================================

    private fun adminButton(
        text: String,
        color: Int,
        darkText: Boolean = false,
        action: () -> Unit
    ): Button {

        val button =
            Button(this)

        button.text =
            text

        button.textSize =
            16f

        button.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        button.setTextColor(
            if (darkText)
                navy
            else
                white
        )

        button.background =
            roundedBackground(
                color,
                25f
            )

        button.setOnClickListener {
            action()
        }


        button.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {

                setMargins(
                    0,
                    dp(7),
                    0,
                    dp(7)
                )
            }


        return button
    }


    // =========================================================
    // INFO CARD
    // =========================================================

    private fun infoCard(
        icon: String,
        title: String,
        subtitle: String,
        color: Int
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(15),
            dp(10),
            dp(15),
            dp(10)
        )

        card.background =
            strokeBackground(
                color,
                Color.argb(
                    170,
                    255,
                    255,
                    255
                ),
                25f,
                2
            )


        val iconView =
            TextView(this)

        iconView.text =
            icon

        iconView.textSize =
            27f

        iconView.gravity =
            Gravity.CENTER


        card.addView(
            iconView,
            LinearLayout.LayoutParams(
                dp(60),
                dp(60)
            )
        )


        val text =
            LinearLayout(this)

        text.orientation =
            LinearLayout.VERTICAL

        text.setPadding(
            dp(14),
            0,
            0,
            0
        )


        val titleView =
            TextView(this)

        titleView.text =
            title

        titleView.textSize =
            18f

        titleView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        titleView.setTextColor(
            white
        )


        val sub =
            TextView(this)

        sub.text =
            subtitle

        sub.textSize =
            13f

        sub.setTextColor(
            lightText
        )


        text.addView(
            titleView
        )

        text.addView(
            sub
        )


        card.addView(
            text,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )


        card.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(85)
            ).apply {

                setMargins(
                    0,
                    dp(8),
                    0,
                    dp(12)
                )
            }


        return card
    }


    // =========================================================
    // PAGE HEADER
    // =========================================================

    private fun pageHeader(
        text: String
    ): TextView {

        val header =
            TextView(this)

        header.text =
            text

        header.textSize =
            22f

        header.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        header.setTextColor(
            white
        )

        header.gravity =
            Gravity.CENTER

        header.background =
            roundedBackground(
                navy,
                28f
            )


        header.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
            )


        return header
    }


    // =========================================================
    // BACKGROUND
    // =========================================================

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                radius
        }
    }


    private fun strokeBackground(
        color: Int,
        strokeColor: Int,
        radius: Float,
        width: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                radius

            setStroke(
                width,
                strokeColor
            )
        }
    }


    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }


    // =========================================================
    // TOAST
    // =========================================================

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private fun hasPermissions(): Boolean {

        val camera =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) ==
                    PackageManager.PERMISSION_GRANTED


        val microphone =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) ==
                    PackageManager.PERMISSION_GRANTED


        return camera &&
                microphone
    }


    // =========================================================
    // DESTROY
    // =========================================================

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
