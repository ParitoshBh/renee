package com.yopers.renee.onboarding

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import ernestoyaquello.com.verticalstepperform.Step
import android.view.LayoutInflater
import com.yopers.renee.R
import kotlinx.android.synthetic.main.step_credentials.view.*
import timber.log.Timber

class CredentialStep: Step<String> {

    lateinit var credentialsView: View

    constructor(endpoint: String) : super(endpoint) {}


    override fun createStepContentLayout(): View {
        val inflater = LayoutInflater.from(context)

        credentialsView = inflater.inflate(R.layout.step_credentials, null, false)
        credentialsView.secretKey.setSingleLine()

        credentialsView.accessKey.hint = "Q3AM3UQ867SPQQA43P2F"
        credentialsView.secretKey.hint = "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG"

        credentialsView.accessKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                markAsCompletedOrUncompleted(true)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        credentialsView.secretKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                markAsCompletedOrUncompleted(true)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        return credentialsView
    }

    override fun isStepDataValid(stepData: String?): IsDataValid {
        Timber.i("Credentials input ${stepData.toString()}")

        if (stepData != null) {
            if (stepData.isNotEmpty()) {
                return IsDataValid(true)
            }
        }

        return IsDataValid(false)
    }

    override fun getStepData(): String {
        val accessKey: String = credentialsView.accessKey.text.toString().trim()
        val secretKey: String = credentialsView.secretKey.text.toString().trim()

        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            return ""
        }

        return accessKey
    }

    override fun getStepDataAsHumanReadableString(): String {
        if (stepData.isNotEmpty()) {
            return stepData
        }

        return "(Empty)"
    }

    override fun onStepOpened(animated: Boolean) {
        // This will be called automatically whenever the step gets opened.
    }

    override fun onStepClosed(animated: Boolean) {
        // This will be called automatically whenever the step gets closed.
    }

    override fun onStepMarkedAsCompleted(animated: Boolean) {
        // This will be called automatically whenever the step is marked as completed.
    }

    override fun onStepMarkedAsUncompleted(animated: Boolean) {
        // This will be called automatically whenever the step is marked as uncompleted.
    }

    override fun restoreStepData(data: String?) {
//        userNameView.setText(stepData)
    }
}