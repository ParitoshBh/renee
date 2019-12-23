package com.yopers.renee

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import io.minio.MinioClient
import io.minio.messages.Bucket
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import com.yopers.renee.fragments.BucketListFragment
import com.yopers.renee.fragments.OnBoardingFragment
import com.yopers.renee.fragments.SettingsFragment
import com.yopers.renee.models.User
import com.yopers.renee.models.User_
import io.minio.errors.MinioException
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import moe.feng.common.view.breadcrumbs.BreadcrumbsView
import moe.feng.common.view.breadcrumbs.model.BreadcrumbItem
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var minioClient: MinioClient
    private lateinit var headerResult: AccountHeader
    private lateinit var navigationDrawer: Drawer
    private lateinit var mBreadcrumbsView: BreadcrumbsView
    lateinit var user: User
    private var navigationDrawerSelectedItemPosition: Int = 0

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBreadcrumbsView = findViewById(R.id.breadcrumbs_view)

        initNavigationDrawer()

        user = getUserConfigs()
        if (user.endPoint != null) {
            Timber.i("Loaded user configs - ${user}")
            initApp(user)
        } else {
            initOnboarding("add")
        }
    }

    private fun initOnboarding(action: String) {
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

    fun updateNavigationDrawerHeader() {
        if (user.endPoint.isNullOrEmpty()) {
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
                    ProfileDrawerItem().withName(user.niceName).withEmail(user.endPoint).withIcon(R.drawable.ic_user),
                    ProfileSettingDrawerItem().withName("Logout").withIcon(GoogleMaterial.Icon.gmd_cloud_off).withIdentifier(100001) ,
                    ProfileSettingDrawerItem().withName("Settings").withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(100002)
                )
                .withOnAccountHeaderListener(object : AccountHeader.OnAccountHeaderListener {
                    override fun onProfileChanged(view: View?, profile: IProfile<*>, current: Boolean): Boolean {
                        when (profile.identifier) {
                            100001.toLong() -> {
                                Timber.i("Account header clicked ${profile.identifier}")
                                logout()
                            }
                            100002.toLong() -> {
                                Timber.i("Account header clicked ${profile.identifier}")
                                openSettings()
                            }
                        }

                        return false
                    }
                })
                .build()
            navigationDrawer.addStickyFooterItem(
                SecondaryDrawerItem()
                    .withName(getString(R.string.nav_drawer_secondary_item_create_bucket))
                    .withIcon(GoogleMaterial.Icon.gmd_create)
            )
        }

        navigationDrawer.setHeader(
            headerResult.view, true
        )
        headerResult.setDrawer(navigationDrawer)
    }

    private fun logout() {
        // Clear database
        clearDatabase()

        // Clear user config
        user = User()

        // Clear toolbar subtitle
        toolbar.subtitle = ""

        // Hide breadcrumb view
        mBreadcrumbsView.visibility = View.GONE

        // Clear navigation drawer
        navigationDrawer.removeAllItems()
        navigationDrawer.removeAllStickyFooterItems()
        navigationDrawer.removeHeader()

        // Show onboarding
        initOnboarding("replace")
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun clearDatabase() {
        val boxStore = ObjectBox.boxStore

        boxStore.close()
        boxStore.deleteAllFiles()
    }

    private fun initApp(user: User) {
        Timber.i("Initialize app with user configs")
        buildMinioClient(user)
        updateNavigationDrawerHeader()

        GlobalScope.launch(Dispatchers.Main) {
            mProgressBar.visibility = View.VISIBLE
            val (buckets, failure) = getBuckets()
            mProgressBar.visibility = View.GONE
            if (failure.isEmpty()) {
                buildNavigationDrawer(buckets)
            } else {
                Snackbar.make(root_layout, failure, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun buildNavigationDrawer(buckets: List<Bucket>) {
        Timber.i("Build navigation drawer")

        for ((index, bucket) in buckets.withIndex()) {
            Timber.i("Add drawer item ${bucket.name()}")

            navigationDrawer.addItem(PrimaryDrawerItem()
                .withIdentifier(index.toLong())
                .withName(bucket.name())
            )
            if (index == 0) {
                navigationDrawer.setSelection(navigationDrawer.getDrawerItem(index.toLong())!!, true)
            }
        }
    }

    fun buildMinioClient(user: User): MinioClient {
        minioClient = MinioClient(user.endPoint, user.accessKey, user.secretKey)
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
                    val drawerItemName = (drawerItem as Nameable<*>).name.toString()
                    Timber.i("Nav drawer item click ${drawerItemName}")

                    if (drawerItemName.contentEquals(getString(R.string.nav_drawer_secondary_item_create_bucket))) {
                        MaterialDialog(this@MainActivity).show {
                            title(R.string.dialog_create_bucket_name)
                            input(
                                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                            ) { _, text ->
                                Timber.i("Dialog input ${text}")
                                com.yopers.renee.utils.Bucket().create(
                                    coroutineScope,
                                    minioClient,
                                    text.toString(),
                                    this@MainActivity
                                )
                            }
                            positiveButton(R.string.dialog_button_positive_create_directory_name)
                        }
                    } else {
                        if (navigationDrawerSelectedItemPosition != position) {
                            navigationDrawerSelectedItemPosition = position
                            Timber.i("Breadcrumb size ${mBreadcrumbsView.items.size}")
                            mBreadcrumbsView.setItems(ArrayList())
                            mBreadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(drawerItemName))
                            loadFragment(drawerItemName)
                        }
                    }

                    return false
                }
            })
            .build()
    }

    fun loadFragment(bucketName: String) {
        Timber.i("Fragments size ${supportFragmentManager.fragments.size}")

        mBreadcrumbsView.visibility = View.VISIBLE

        if (supportFragmentManager.fragments.size == 0) {
            addFragment(bucketName)
        } else {
            replaceFragment(bucketName)
        }
    }

    private fun addFragment(bucketName: String) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.root_layout, BucketListFragment.newInstance(bucketName, minioClient, user), "bucket_list")
            .commit()
    }

    private fun replaceFragment(bucketName: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, BucketListFragment.newInstance(bucketName, minioClient, user), "bucket_list")
            .commit()
    }

    private suspend fun getBuckets(): Pair<List<Bucket>, String> {
        return withContext(Dispatchers.IO) {
            try {
                Pair(minioClient.listBuckets(), "")
            } catch (e: MinioException) {
                Timber.i("Exception getting bucket list ${e}")
                Pair(emptyList<Bucket>(), e.message.orEmpty())
            } catch (s: SocketTimeoutException) {
                Timber.i("Exception getting bucket list ${s}")
                Pair(emptyList<Bucket>(), s.message.orEmpty())
            }
        }
    }

    private fun getUserConfigs(): User {
        val userBox: Box<User> = ObjectBox.boxStore.boxFor()
        val user: User? = userBox.query().equal(User_.isActive, true).build().findFirst()

        if (user != null ) {
            return user
        }

        return User()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            11 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(
                        this.findViewById(R.id.root_layout),
                        "Permission granted. Please try downloading again",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        this.findViewById(R.id.root_layout),
                        "Permission denied, Renee won't be able to download files",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (navigationDrawer.isDrawerOpen) {
            navigationDrawer.closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}
