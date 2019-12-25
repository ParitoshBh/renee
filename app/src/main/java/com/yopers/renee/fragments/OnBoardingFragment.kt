package com.yopers.renee.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yopers.renee.MainActivity
import com.yopers.renee.R
import com.yopers.renee.onboarding.CredentialStep
import com.yopers.renee.onboarding.EndpointStep
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener
import io.minio.MinioClient
import io.minio.messages.Bucket
import kotlinx.android.synthetic.main.onboarding.*
import kotlinx.android.synthetic.main.step_credentials.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.google.android.material.snackbar.Snackbar
import com.yopers.renee.ObjectBox
import com.yopers.renee.models.User
import com.yopers.renee.onboarding.NicenameStep
import com.yopers.renee.utils.Builder
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception


class OnBoardingFragment: Fragment(), StepperFormListener {
    lateinit var endpointStep: EndpointStep
    lateinit var credentialStep: CredentialStep
    lateinit var nicenameStep: NicenameStep

    companion object {

        @JvmStatic
        fun newInstance() =
            OnBoardingFragment().apply {
                arguments = Bundle().apply {
                    // putInt(ARG_COLUMN_COUNT, columnCount)
//                    selectedBucket = bucketName
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            // columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.onboarding, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        endpointStep = EndpointStep("Endpoint")
        credentialStep = CredentialStep("Credentials")
        nicenameStep = NicenameStep("Name")
        stepper_form
            .setup(this, endpointStep, credentialStep, nicenameStep)
            .displayBottomNavigation(false)
            .lastStepNextButtonText("Connect")
            .init()
    }

    override fun onCompletedForm() {
        hideKeyboard()
        GlobalScope.launch(Dispatchers.Main) {
            llProgressBar.visibility = View.VISIBLE

            val parentActivity = (activity as MainActivity)
            val user = User(
                endPoint = endpointStep.userNameView.text.toString(),
                accessKey = credentialStep.credentialsView.accessKey.text.toString(),
                secretKey = credentialStep.credentialsView.secretKey.text.toString(),
                niceName = nicenameStep.niceNameView.text.toString(),
                isActive = true
            )

            parentActivity.user = user

            val minioClient  = Builder().minioClient(user)

            val buckets = pingHost(minioClient, user)

            if (buckets.isNotEmpty()) {
                parentActivity.buildNavigationDrawer(buckets)
                parentActivity.updateNavigationDrawerHeader()
                parentActivity.loadFragment(buckets[0].name())
            } else {
//                stepper_form.markOpenStepAsUncompleted(true, "Unable to connect. Please check details")
                stepper_form.cancelFormCompletionOrCancellationAttempt()
                Snackbar.make(parentActivity.root_layout, "Unable to connect. Please check details", Snackbar.LENGTH_LONG).show()
            }
            llProgressBar.visibility = View.INVISIBLE
        }
    }

    override fun onCancelledForm() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private suspend fun pingHost(minioClient: MinioClient, user: User): List<Bucket> {
        return withContext(Dispatchers.IO) {
            try {
                val buckets = minioClient.listBuckets()
                if (buckets.isNotEmpty()) {
                    val userBox: Box<User> = ObjectBox.boxStore.boxFor()
                    userBox.put(user)
                    buckets
                } else {
                    emptyList<Bucket>()
                }
            } catch (e: Exception) {
                emptyList<Bucket>()
            }
        }
    }

    private fun hideKeyboard() {
        val inputManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}