package com.yopers.renee.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.yopers.renee.ObjectBox
import com.yopers.renee.R
import com.yopers.renee.models.User
import com.yopers.renee.models.User_
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat() {
    private val INTENT_SELECT_PATH_REQUEST_CODE = 110
    private lateinit var user: User
    private val userBox: Box<User> = ObjectBox.boxStore.boxFor()
    private var defaultDownloadLocation: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        defaultDownloadLocation = findPreference("defaultDownloadLocation")

        val user = userBox.query().equal(User_.isActive, true).build().findFirst()
        if (user != null ) {
            this.user = user
            if (this.user.defaultDownloadLocation != null) {
                defaultDownloadLocation!!.summary = this.user.defaultDownloadLocation
            } else {
                defaultDownloadLocation!!.summary = "Tap here to choose download location"
            }
        }

        defaultDownloadLocation!!.setOnPreferenceClickListener {
            startActivityForResult(
                Intent().setAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
                INTENT_SELECT_PATH_REQUEST_CODE
            )

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            INTENT_SELECT_PATH_REQUEST_CODE -> {
                if (data != null) {
                    Timber.i("Selected download location ${data.data.toString()}")
                    this.user.defaultDownloadLocation = data.data.toString()
                    userBox.put(this.user)
                    defaultDownloadLocation!!.summary = data.data.toString()
                } else {
                    // Failed to save setting
                    Snackbar.make(
                        activity!!.findViewById(R.id.settings_layout),
                        "Failed to save selected download path. Please try again",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}