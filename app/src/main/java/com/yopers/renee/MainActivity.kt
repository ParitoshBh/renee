package com.yopers.renee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import io.minio.MinioClient
import io.minio.messages.Bucket
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import com.yopers.renee.fragments.MyQuoteListFragment
import com.yopers.renee.fragments.OnBoardingFragment
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var minioClient: MinioClient
    private lateinit var headerResult: AccountHeader
    private lateinit var navigationDrawer: Drawer
    lateinit var userConfig: Map<String, String>
    private var navigationDrawerSelectedItemPosition: Int = 0

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initNavigationDrawer()

        GlobalScope.launch(Dispatchers.Main) {
            userConfig = getUserConfigs()
            if (userConfig.isEmpty()) {
                initOnboarding("add")
            } else {
                initApp(userConfig)
            }
        }
    }

    private fun initOnboarding(action: String) {
        updateNavigationDrawerHeader()

        if (action.equals("add")) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.root_layout, OnBoardingFragment.newInstance(), "bucket_list")
                .commit()
        } else {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.root_layout, OnBoardingFragment.newInstance(), "bucket_list")
                .commit()
        }
    }

    private fun updateNavigationDrawerHeader() {
        if (userConfig.isEmpty()) {
            headerResult = AccountHeaderBuilder()
                .withActivity(this)
                .withCompactStyle(true)
                .addProfiles(ProfileDrawerItem().withEmail("Add Account").withIcon(R.drawable.ic_user).withNameShown(false))
                .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
                    override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                        return false
                    }
                })
                .build()
        } else {
            headerResult = AccountHeaderBuilder()
                .withActivity(this)
                .withCompactStyle(true)
                .addProfiles(
                    ProfileDrawerItem().withName("Bucket Name").withEmail(userConfig["endpoint"]).withIcon(R.drawable.ic_user),
                    ProfileSettingDrawerItem().withName("Logout").withIcon(GoogleMaterial.Icon.gmd_cloud_off).withIdentifier(100001)
                )
                .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
                    override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                        if (profile.identifier == 100001.toLong()) {
                            Timber.i("Account header clicked ${profile.identifier}")
                            logout()
                        }
                        return false
                    }
                })
                .build()
        }

        navigationDrawer.setHeader(
            headerResult.view, true
        )
        headerResult.setDrawer(navigationDrawer)
    }

    private fun logout() {
        coroutineScope.launch(Dispatchers.Main) {
            // Clear database
            clearDatabase()

            // Clear user config
            userConfig = emptyMap()

            // Clear toolbar subtitle
            toolbar.subtitle = ""

            // Hide breadcrumb view
            breadcrumbs_view.visibility = View.GONE

            // Clear navigation drawer
            navigationDrawer.removeAllItems()

            // Show onboarding
            initOnboarding("replace")
        }
    }

    private suspend fun clearDatabase() =
        withContext(Dispatchers.IO) {
            return@withContext Paper.book().destroy()
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
                loadFragment(firstBucket, "add", buckets, true)
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
                        loadFragment(selectedBucket, "replace", emptyList(), true)
                    }
                    return false
                }
            })
            .build()
    }

    fun loadFragment(bucketName: String, action: String, buckets: List<Bucket>, showBreadcrumb: Boolean) {
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

//        if (showBreadcrumb) {
            breadcrumbs_view.visibility = View.VISIBLE
//        } else {
//            breadcrumbs_view.visibility = View.GONE
//        }

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
