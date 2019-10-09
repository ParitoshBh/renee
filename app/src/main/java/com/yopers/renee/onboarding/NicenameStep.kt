package com.yopers.renee.onboarding

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import ernestoyaquello.com.verticalstepperform.Step
import timber.log.Timber

class NicenameStep: Step<String> {

    lateinit var niceNameView: EditText

    constructor(endpoint: String) : super(endpoint) {}


    override fun createStepContentLayout(): View {
        niceNameView = EditText(context)
        niceNameView.setSingleLine()
        niceNameView.hint = "Nice name"

        niceNameView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                markAsCompletedOrUncompleted(true)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        return niceNameView
    }

    override fun isStepDataValid(stepData: String?): IsDataValid {
        Timber.i("Nice name input ${stepData.toString()}")

        if (stepData != null) {
            if (stepData.isNotEmpty()) {
                return IsDataValid(true)
            }
        }

        return IsDataValid(false)
    }

    override fun getStepData(): String {
        val niceName: String = niceNameView.text.toString().trim()

        if (niceName.isNotEmpty()) {
            return niceName
        }

        return ""
    }

    override fun getStepDataAsHumanReadableString(): String {
        if (stepData.isNotEmpty()) {
            return stepData.toString()
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