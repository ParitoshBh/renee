package com.yopers.renee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import io.minio.MinioClient
import io.minio.messages.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import com.yopers.renee.fragments.MyQuoteListFragment
import com.yopers.renee.fragments.OnBoardingFragment
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var minioClient: MinioClient
    private lateinit var headerResult: AccountHeader
    private lateinit var navigationDrawer: Drawer
    private lateinit var defaultProfile: ProfileDrawerItem
    lateinit var userConfig: Map<String, String>
    private var navigationDrawerSelectedItemPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initNavigationDrawer()

        GlobalScope.launch(Dispatchers.Main) {
            userConfig = getUserConfigs()
            if (userConfig.isEmpty()) {
                initOnboarding()
            } else {
                initApp(userConfig)
            }
        }
    }

    private fun initOnboarding() {
        updateNavigationDrawerHeader()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.root_layout, OnBoardingFragment.newInstance(), "bucket_list")
            .commit()
    }

    private fun updateNavigationDrawerHeader() {
        if (userConfig.isEmpty()) {
            defaultProfile = ProfileDrawerItem()
                .withEmail("Add Account")
                .withIcon(R.drawable.ic_user)
                .withNameShown(false)
        } else {
            defaultProfile = ProfileDrawerItem()
                .withName("Bucket Name")
                .withEmail(userConfig["endpoint"])
                .withIcon(R.drawable.ic_user)
        }

        headerResult = AccountHeaderBuilder()
            .withActivity(this)
            .withCompactStyle(true)
            .addProfiles(defaultProfile)
            .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
                override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                    return false
                }
            })
            .build()

        navigationDrawer.setHeader(
            headerResult.view, true
        )
        headerResult.setDrawer(navigationDrawer)
    }

    private fun initApp(userConfig: Map<String, String>) {
        Timber.i("Initialize app with user configs")
        buildMinioClient(userConfig)
        updateNavigationDrawerHeader()

        GlobalScope.launch(Dispatchers.Main) {
            mProgressBar.visibility = View.VISIBLE
            val (buckets, failure) = getBuckets()
            mProgressBar.visibility = View.GONE
            if (failure.isEmpty()) {
                val firstBucket = buckets[0].name()
                loadFragment(firstBucket, "add", buckets)
            } else {

                Snackbar.make(root_layout, failure, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun buildMinioClient(userConfig: Map<String, String>): MinioClient {
        minioClient = MinioClient(
            userConfig["endpoint"],
            userConfig["accessKey"],
            userConfig["secretKey"]
        )
        minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10),0,0)

        return minioClient
    }

    private fun initNavigationDrawer() {
        setSupportActionBar(toolbar)

        navigationDrawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withOnDrawerItemClickListener(object: Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    if (navigationDrawerSelectedItemPosition != position) {
                        navigationDrawerSelectedItemPosition = position
                        breadcrumbs_view.setItems(ArrayList())
                        val selectedBucket = (drawerItem as Nameable<*>).name.toString()
                        loadFragment(selectedBucket, "replace", emptyList())
                    }
                    return false
                }
            })
            .build()
    }

    fun loadFragment(bucketName: String, action: String, buckets: List<Bucket>) {
        if (buckets.isNotEmpty()) {
            Timber.i("Populate navigation drawer")
            updateNavigationDrawerHeader()
            for ((index, bucket) in buckets.withIndex()) {
                navigationDrawer.addItem(PrimaryDrawerItem()
                    .withIdentifier(index.toLong())
                    .withName(bucket.name())
                )
            }
        }

        when(action) {
            "add" -> addFragment(bucketName)
            "replace" -> replaceFragment(bucketName)
        }
    }

    private fun addFragment(bucketName: String) {
        navigationDrawer.setSelection(navigationDrawerSelectedItemPosition.toLong())
        supportFragmentManager
            .beginTransaction()
            .add(R.id.root_layout, MyQuoteListFragment.newInstance(bucketName, minioClient), "bucket_list")
            .commit()
    }

    private fun replaceFragment(bucketName: String) {
        Timber.i("Replace fragment with $bucketName objects")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, MyQuoteListFragment.newInstance(bucketName, minioClient), "bucket_list")
            .commit()
    }

    private suspend fun getBuckets(): Pair<List<Bucket>, String> {
        return withContext(Dispatchers.IO) {
            try {
                Pair(minioClient.listBuckets(), "")
            } catch (e: Exception) {
                Pair(emptyList<Bucket>(), e.toString())
            }
        }
    }

    private suspend fun getUserConfigs(): Map<String, String> {
        return withContext(Dispatchers.IO) {
            val userConfig: Map<String, String>? = Paper.book().read("userConfig")
            if (userConfig.isNullOrEmpty()) {
                emptyMap<String, String>()
            } else {
                userConfig
            }
        }
    }

    override fun onBackPressed() {
        //handle the back press, close the drawer first and if the drawer is closed close the activity
        if (navigationDrawer.isDrawerOpen) {
            navigationDrawer.closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}
