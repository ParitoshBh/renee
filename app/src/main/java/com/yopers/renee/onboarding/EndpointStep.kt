package com.yopers.renee.onboarding

import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import ernestoyaquello.com.verticalstepperform.Step
import timber.log.Timber
import android.widget.TextView
import android.text.Editable
import android.util.Patterns
import android.view.KeyEvent


class EndpointStep: Step<String> {

    lateinit var userNameView: EditText

    constructor(endpoint: String) : super(endpoint) {}


    override fun createStepContentLayout(): View {
        userNameView = EditText(context)
        userNameView.setSingleLine()
        userNameView.hint = "https://play.min.io/minio"

        userNameView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                markAsCompletedOrUncompleted(true)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        userNameView.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
                formView.goToNextStep(true)
                return false
            }
        })

        return userNameView
    }

    override fun isStepDataValid(stepData: String?): IsDataValid {
        Timber.i("Endpoint input ${stepData.toString()}")

        if (Patterns.WEB_URL.matcher(stepData).matches()) {
            return IsDataValid(true)
        } else {
            return IsDataValid(false)
        }
    }

    override fun getStepData(): String {
        val endpoint: Editable = userNameView.text

        if (endpoint.isEmpty()) {
            return ""
        }

        return endpoint.toString()
    }

    override fun getStepDataAsHumanReadableString(): String {
        val endpoint = getStepData();

        if (endpoint.isEmpty()) {
            return "(Empty)"
        }

        return endpoint
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