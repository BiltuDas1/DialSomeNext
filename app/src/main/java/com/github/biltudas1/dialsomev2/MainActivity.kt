package com.github.biltudas1.dialsomev2

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the lock screen wake-up flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Load the new Precision Architect UI
        setContentView(R.layout.activity_main)

        // TODO later: Initialize the new views here
        // val etSearch = findViewById<EditText>(R.id.etSearch)
        // val rvContacts = findViewById<RecyclerView>(R.id.rvContacts)
        // val fabAddContact = findViewById<FloatingActionButton>(R.id.fabAddContact)
    }
}