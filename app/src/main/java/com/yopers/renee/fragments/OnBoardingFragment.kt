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
import io.paperdb.Paper
import kotlinx.android.synthetic.main.onboarding.*
import kotlinx.android.synthetic.main.step_credentials.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnBoardingFragment: Fragment(), StepperFormListener {
    lateinit var endpointStep: EndpointStep
    lateinit var credentialStep: CredentialStep

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
        stepper_form
            .setup(this, endpointStep, credentialStep)
            .displayBottomNavigation(false)
            .init()
    }

    override fun onCompletedForm() {
        GlobalScope.launch(Dispatchers.Main) {
            llProgressBar.visibility = View.VISIBLE
            val minioClient = MinioClient(
                endpointStep.userNameView.text.toString(),
                credentialStep.credentialsView.accessKey.text.toString(),
                credentialStep.credentialsView.secretKey.text.toString()
            )
            val buckets = pingHost(
                minioClient,
                endpointStep.userNameView.text.toString(),
                credentialStep.credentialsView.accessKey.text.toString(),
                credentialStep.credentialsView.secretKey.text.toString())
            if (buckets.isNotEmpty()) {
                val parentActivity = (activity as MainActivity)
                parentActivity.minioClient = minioClient
                parentActivity.loadFragment("yoga", "replace", buckets)
                llProgressBar.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCancelledForm() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private suspend fun pingHost(minioClient: MinioClient, endpoint: String, accessKey: String, secretKey: String): List<Bucket> {
        return withContext(Dispatchers.IO) {
            val buckets = minioClient.listBuckets()
            if (buckets.isNotEmpty()) {
                Paper.book().write("userConfig", mapOf<String, String>(
                    "endpoint" to endpoint,
                    "accessKey" to accessKey,
                    "secretKey" to secretKey
                ))
                buckets
            } else {
                emptyList<Bucket>()
            }
        }
    }
}