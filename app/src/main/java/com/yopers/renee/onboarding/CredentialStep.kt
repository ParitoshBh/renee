package com.yopers.renee.onboarding

import android.view.View
import ernestoyaquello.com.verticalstepperform.Step
import android.view.LayoutInflater
import com.yopers.renee.R
import kotlinx.android.synthetic.main.step_credentials.view.*

class CredentialStep: Step<String> {

    lateinit var credentialsView: View

    constructor(endpoint: String) : super(endpoint) {}


    override fun createStepContentLayout(): View {
        val inflater = LayoutInflater.from(context)
        credentialsView = inflater.inflate(R.layout.step_credentials, null, false)
        credentialsView.secretKey.setSingleLine()

        credentialsView.accessKey.hint = "Q3AM3UQ867SPQQA43P2F"
        credentialsView.secretKey.hint = "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG"

        return credentialsView
    }

    override fun isStepDataValid(stepData: String?): IsDataValid {
//        // The step's data (i.e., the user name) will be considered valid only if it is longer than
//        // three characters. In case it is not, we will display an error message for feedback.
//        // In an optional step, you should implement this method to always return a valid value.
//        boolean isNameValid = stepData.length() >= 3;
//        String errorMessage = !isNameValid ? "3 characters minimum" : "";
//
//        return new IsDataValid(isNameValid, errorMessage);
        return IsDataValid(true)
    }

    override fun getStepData(): String {
        // We get the step's data from the value that the user has typed in the EditText view.
//        val userName: Editable = userNameView.text
//        return userName != null ? userName.toString() : "";
        return ""
    }

    override fun getStepDataAsHumanReadableString(): String {
        // Because the step's data is already a human-readable string, we don't need to convert it.
        // However, we return "(Empty)" if the text is empty to avoid not having any text to display.
        // This string will be displayed in the subtitle of the step whenever the step gets closed.
//        String userName = getStepData();
//        return !userName.isEmpty() ? userName : "(Empty)";
        return ""
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