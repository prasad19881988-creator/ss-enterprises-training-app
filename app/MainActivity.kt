package com.ssenterprises.training

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "SS Enterprises Training"
        title.textSize = 28f

        val videoButton = Button(this)
        videoButton.text = "Start Video Training"

        val adminButton = Button(this)
        adminButton.text = "Admin Panel"

        layout.addView(title)
        layout.addView(videoButton)
        layout.addView(adminButton)

        setContentView(layout)
    }
}
