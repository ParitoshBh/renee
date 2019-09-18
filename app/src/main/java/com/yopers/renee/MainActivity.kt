package com.yopers.renee

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import io.minio.MinioClient
import io.minio.messages.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.Nameable
import com.yopers.renee.fragments.OnBoardingFragment
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    lateinit var minioClient: MinioClient
    private lateinit var navigationDrawer: Drawer
    private lateinit var userConfig: Map<String, String>

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
        supportFragmentManager
            .beginTransaction()
            .add(R.id.root_layout, OnBoardingFragment.newInstance(), "bucket_list")
            .commit()
    }

    private fun initApp(userConfig: Map<String, String>) {
        minioClient = MinioClient(
            userConfig["endpoint"],
            userConfig["accessKey"],
            userConfig["secretKey"]
        )

        GlobalScope.launch(Dispatchers.Main) {
            val buckets = getBuckets()
            val firstBucket = buckets[0].name()
            loadFragment(firstBucket, "add", buckets)
        }
    }

    fun initNavigationDrawer() {
        setSupportActionBar(toolbar)

        navigationDrawer = DrawerBuilder()
            .withActivity(this)
            .withToolbar(toolbar)
            .withOnDrawerItemClickListener(object: Drawer.OnDrawerItemClickListener {
                override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                    breadcrumbs_view.setItems(ArrayList())
                    val selectedBucket = (drawerItem as Nameable<*>).name.toString()
                    loadFragment(selectedBucket, "replace", emptyList())
                    return false
                }
            })
            .build()
    }

    fun loadFragment(bucketName: String, action: String, buckets: List<Bucket>) {
        if (buckets.isNotEmpty()) {
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
        supportFragmentManager
            .beginTransaction()
            .add(R.id.root_layout, MyQuoteListFragment.newInstance(bucketName, minioClient), "bucket_list")
            .commit()
    }

    private fun replaceFragment(bucketName: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, MyQuoteListFragment.newInstance(bucketName, minioClient), "bucket_list")
            .commit()
    }

    private suspend fun getBuckets(): List<Bucket> {
        return withContext(Dispatchers.IO) {
            minioClient.listBuckets()
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
}
