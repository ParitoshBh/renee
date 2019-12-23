package com.yopers.renee

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yopers.renee.fragments.SettingsFragment
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.subtitle = "Settings"

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_layout, SettingsFragment())
            .commit()
    }
}