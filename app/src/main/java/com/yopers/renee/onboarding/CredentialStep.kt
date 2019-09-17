package com.yopers.renee.onboarding

import android.text.Editable
import android.view.View
import android.widget.EditText
import ernestoyaquello.com.verticalstepperform.Step
import android.view.LayoutInflater
import com.yopers.renee.R


class CredentialStep: Step<String> {

    //    lateinit var userNameView: EditText
    lateinit var credentialsView: View

    constructor(endpoint: String) : super(endpoint) {
//        this.userNameView = endpoint
    }


    override fun createStepContentLayout(): View {
        // Here we generate the view that will be used by the library as the content of the step.
        // In this case we do it programmatically, but we could also do it by inflating an XML layout.
//        userNameView = new EditText(getContext());
//        userNameView = EditText(context)
//        userNameView.setSingleLine()
//        userNameView.hint = "https://storage.minio.com"
        val inflater = LayoutInflater.from(context)
        credentialsView = inflater.inflate(R.layout.step_credentials, null, false)
//        ...
//        userNameView.addTextChangedListener(new TextWatcher() {
//            ...
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // Whenever the user updates the user name text, we update the state of the step.
//                // The step will be marked as completed only if its data is valid, which will be
//                // checked automatically by the form with a call to isStepDataValid().
//                markAsCompletedOrUncompleted(true);
//            }
//        });

//        return userNameView;
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