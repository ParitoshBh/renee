package com.yopers.renee.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.input
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.helpers.ActionModeHelper
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.mikepenz.materialize.util.UIUtils
import com.yopers.renee.BucketItem
import com.yopers.renee.MainActivity
import com.yopers.renee.ObjectBox
import com.yopers.renee.R
import com.yopers.renee.background.SyncManager
import com.yopers.renee.models.Task
import com.yopers.renee.models.Task_
import com.yopers.renee.models.User
import com.yopers.renee.utils.*
import io.minio.MinioClient
import io.minio.Result
import io.minio.errors.MinioException
import io.minio.messages.Item
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.dialog_sync_overview.*
import kotlinx.android.synthetic.main.object_list.*
import kotlinx.coroutines.*
import moe.feng.common.view.breadcrumbs.BreadcrumbsView
import moe.feng.common.view.breadcrumbs.DefaultBreadcrumbsCallback
import moe.feng.common.view.breadcrumbs.model.BreadcrumbItem
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BucketListFragment: Fragment() {
    private val INTENT_SELECT_PATH_REQUEST_CODE = 110
    private val INTENT_SELECT_FILE_REQUEST_CODE = 111
    private val INTENT_SELECT_DESTINATION_PATH_REQUEST_CODE = 112
    var itemAdapter = ItemAdapter<BucketItem>()
    private lateinit var minioClient: MinioClient
    private lateinit var fastAdapter: FastAdapter<BucketItem>
    private lateinit var selectedBucket: String
    private var selectedBucketPrefix = ""
    private lateinit var user: User
    private lateinit var toolbar: Toolbar
    private lateinit var mBreadcrumbsView: BreadcrumbsView
    private lateinit var mActionModeHelper: ActionModeHelper<BucketItem>
    private lateinit var selectExtension: SelectExtension<BucketItem>

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val taskBox: Box<Task> = ObjectBox.boxStore.boxFor()
    private lateinit var optionsMenu: Menu

    companion object {

        @JvmStatic
        fun newInstance(bucketName: String, mClient: MinioClient, mConfig: User) =
            BucketListFragment().apply {
                arguments = Bundle().apply {
                    // putInt(ARG_COLUMN_COUNT, columnCount)
                    selectedBucket = bucketName
                    minioClient = mClient
                    user = mConfig
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
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.object_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.tag("Minio: List Fragment")

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(
                R.id.fab_upload_object,
                IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_file_upload)
            ).create()
        )
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(
                R.id.fab_create_directory,
                IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_create_new_folder)
            ).create()
        )

        speedDial.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_upload_object -> {
                    speedDial.close(true)
                    startActivityForResult(
                        Intent().setAction(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        },
                        INTENT_SELECT_FILE_REQUEST_CODE
                    )
                    return@OnActionSelectedListener true
                }
                R.id.fab_create_directory -> {
                    speedDial.close(true)
                    MaterialDialog(context!!).show {
                        title(R.string.dialog_create_directory_name)
                        input(
                            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                        ) { _, text ->
                            Timber.i("Dialog input ${text}")
                            Bucket().createDirectory(
                                coroutineScope,
                                minioClient,
                                selectedBucket,
                                selectedBucketPrefix,
                                text.toString(),
                                this@BucketListFragment
                            )
                        }
                        positiveButton(R.string.dialog_button_positive_create_directory_name)
                    }
                    return@OnActionSelectedListener true
                }
            }
            false
        })

        fragmentProgressBar.visibility = View.VISIBLE

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)

        // Gets (or creates and attaches if not yet existing) the extension from the given `FastAdapter`
        selectExtension = fastAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        selectExtension.multiSelect = true
        selectExtension.selectOnLongClick = true

        fastAdapter.onPreClickListener = { _: View?, _: IAdapter<BucketItem>, item: BucketItem, _: Int ->
            //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
            val res = mActionModeHelper.onClick(item)
            res ?: false
        }

        fastAdapter.onLongClickListener = { view, adapter, item, position ->
            false
        }

        fastAdapter.onClickListener = { view, adapter, item, position ->
            Timber.i("Action mode active ${mActionModeHelper.isActive}")
            if (mActionModeHelper.isActive.not()) {
                if (item.isDir!!) {
                    fragmentProgressBar.visibility = View.VISIBLE
                    selectedBucketPrefix = item.objectPath.orEmpty()
                    loadBucketObjects(selectedBucketPrefix)
                }
            }
            false
        }

        fastAdapter.onPreLongClickListener = { _: View, _: IAdapter<BucketItem>, item: BucketItem, position: Int ->
            Timber.i("On long click listener ${item.isDir} at ${position}")
            val actionMode = mActionModeHelper.onLongClick(activity as MainActivity, position)
            if (actionMode != null) {
                activity!!
                    .findViewById<View>(R.id.action_mode_bar)
                    .setBackgroundColor(UIUtils.getThemeColorFromAttrOrRes(
                        activity as MainActivity,
                        R.attr.colorPrimary,
                        R.color.colorControlNormal
                    ))
            }
            //if we have no actionMode we do not consume the event
            actionMode != null
        }

        //we init our ActionModeHelper
        mActionModeHelper = ActionModeHelper(fastAdapter, R.menu.cab, ActionBarCallBack())

        toolbar = activity!!.findViewById(R.id.toolbar)
        mBreadcrumbsView = activity!!.findViewById(R.id.breadcrumbs_view)

        mBreadcrumbsView.setCallback(object : DefaultBreadcrumbsCallback<BreadcrumbItem>() {
            override fun onNavigateBack(item: BreadcrumbItem, position: Int) {
                Timber.i("Breadcrumb onNavigateBack ${item.selectedItem} at ${position} with total ${mBreadcrumbsView.items.size}")
                if (mBreadcrumbsView.items.size != 1) {
                    mBreadcrumbsView.removeLastItem()
                }

                selectedBucketPrefix = buildUpdatedBucketPrefix(selectedBucketPrefix, item.selectedItem, position)
                loadBucketObjects(selectedBucketPrefix)
            }

            override fun onNavigateNewLocation(newItem: BreadcrumbItem, changedPosition: Int) {
                Timber.i("Breadcrumb onNavigateBack ${newItem.selectedIndex} at ${changedPosition}")
            }
        })

        list.adapter = fastAdapter

        loadBucketObjects("")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            INTENT_SELECT_PATH_REQUEST_CODE -> {
                if (data != null) {
                    Timber.i("Selected download location ${data.data.toString()}")
                    coroutineScope.launch(Dispatchers.Main) {
                        Snackbar.make(
                            activity!!.findViewById(R.id.root_layout),
                            "Saved selected download path. Please download object(s) again",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Failed to save setting
                    Snackbar.make(
                        activity!!.findViewById(R.id.root_layout),
                        "Failed to save selected download path. Please try again",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            INTENT_SELECT_FILE_REQUEST_CODE -> {
                if (data != null) {
                    Upload().bucketObject(
                        data.data!!,
                        context!!,
                        coroutineScope,
                        minioClient,
                        selectedBucket,
                        selectedBucketPrefix,
                        this
                    )
                } else {
                    // Failed to save setting
                    Snackbar.make(
                        activity!!.findViewById(R.id.root_layout),
                        "Failed to select a file. Please try again",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            INTENT_SELECT_DESTINATION_PATH_REQUEST_CODE -> {
                if (data != null) {
                    Timber.i("Selected destination path ${data.data.toString()}")
                    MaterialDialog(context!!).show {
                        customView(R.layout.dialog_sync_overview)

                        syncOverviewSource.text = "$selectedBucket/$selectedBucketPrefix"
                        syncOverviewDestination.text = data.data.toString()

                        positiveButton (text = "Enable") {
                            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncManager>(15, TimeUnit.MINUTES).build()

                            taskBox.put(
                                Task(
                                    userId = user.id,
                                    taskId = syncWorkRequest.id.toString(),
                                    source = "$selectedBucket/$selectedBucketPrefix",
                                    destination = data.data.toString()
                                )
                            )

                            WorkManager.getInstance(context).enqueue(syncWorkRequest)

                            // Update menu
                            optionsMenu.findItem(R.id.menuDisableSync).isVisible = true
                            optionsMenu.findItem(R.id.menuEnableSync).isVisible = false

                            Snackbar.make(
                                activity!!.findViewById(R.id.root_layout),
                                "Added sync",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        negativeButton(text = "Cancel")
                    }
                } else {
                    // Failed to save setting
                    Snackbar.make(
                        activity!!.findViewById(R.id.root_layout),
                        "Failed to get selected path. Please try again",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun buildUpdatedBucketPrefix(selectedBucketPrefix: String, selectedCrumb: String, selectedCrumbPosition: Int): String {
        var updatedPath = ""

        if (selectedCrumbPosition != 0) {
            val path = selectedBucketPrefix.split("/")
            path.forEach {
                if (it.isNotEmpty()) {
                    updatedPath += "$it/"
                    if (it.contentEquals(selectedCrumb)) {
                        return updatedPath
                    }
                }
            }
        }

        return updatedPath
    }

    fun loadBucketObjects(selectedBucketPrefix: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val bucketObjects = getAllBuckets(selectedBucket, selectedBucketPrefix, minioClient)
            Timber.i("Bucket objects empty ${bucketObjects.isEmpty()}")
            onDataLoaded(bucketObjects, selectedBucketPrefix, selectedBucket)
        }
    }

    private suspend fun getAllBuckets(selectedBucket: String, selectedBucketPrefix: String, minioClient: MinioClient) =
        withContext(Dispatchers.IO) {
            var bucketObjects: List<Result<Item>> = emptyList()
            try {
                bucketObjects = minioClient.listObjects(selectedBucket, selectedBucketPrefix, false).toList()
            } catch (e: MinioException) {
                Timber.i("Exception occurred ${e}")
            }

            return@withContext bucketObjects
        }

    private fun onDataLoaded(results: List<Result<Item>>?, bucketPrefix: String, bucket: String) {
        toolbar.subtitle = results?.size.toString() + " objects"

        buildBreadcrumbs(bucket, bucketPrefix)

        itemAdapter.clear()

        for ((index, bucketObj) in results!!.withIndex()) {
            var objectName = ""
            var objectSize = ""

            val objectPath = bucketObj.get().objectName().split("/")
            if (bucketObj.get().isDir) {
                objectName = "${objectPath[objectPath.size - 2]}/"
                Timber.i("Object path ${objectPath} and size ${objectPath.size}")
            } else {
                objectName = objectPath.last()
                objectSize = android.text.format.Formatter.formatShortFileSize(context, bucketObj.get().objectSize())
            }

            itemAdapter.add(BucketItem().build(
                objectName,
                bucketObj.get().isDir,
                objectSize,
                bucketObj.get().objectName()
            ))
        }

        fastAdapter.notifyAdapterDataSetChanged()

        if (fragmentProgressBar != null) {
            fragmentProgressBar.visibility = View.INVISIBLE
        }
    }

    private fun buildBreadcrumbs(bucketName: String, bucketPrefix: String) {
        if (bucketPrefix.isEmpty() && (mBreadcrumbsView.items.size == 0)) {
            mBreadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(bucketName))
        } else {
            val path = bucketPrefix.split('/')
            if (path.size >= 2) {
                mBreadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(path[path.size - 2]))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options, menu)
        Timber.i("Create options menu")

        optionsMenu = menu
        val enableSync = menu.findItem(R.id.menuEnableSync)
        val disableSync = menu.findItem(R.id.menuDisableSync)

        disableSync.icon = IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_sync).actionBar().colorInt(Color.WHITE)
        enableSync.icon = IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_sync_disabled).actionBar().colorInt(Color.WHITE)

        // Check if current bucket (path) has been synchronized
        val task = taskBox.query().equal(Task_.source, "$selectedBucket/$selectedBucketPrefix").build().findFirst()

        // Assign menu item sync based on that
        if (task != null) {
            disableSync.isVisible = true
        } else {
            enableSync.isVisible = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menuEnableSync -> {
                Timber.i("Enable sync for given path ${selectedBucket}/${selectedBucketPrefix}")
                showEnableSyncDialog()
                true
            }
            R.id.menuDisableSync -> {
                Timber.i("Disable sync for given path ${selectedBucket}/${selectedBucketPrefix}")
                showDisableSyncDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEnableSyncDialog() {
        MaterialDialog(context!!).show {
            title(text = "Choose Sync Target")
            message(text = "Please select a folder to which you want to sync contents of '${selectedBucket}/${selectedBucketPrefix}'")
            positiveButton (text = "Select") { dialog ->
                showDestinationPicker()
            }
        }
    }

    private fun showDisableSyncDialog() {
        MaterialDialog(context!!).show {
            title(text = "Confirm Action")
            message(text = "Are you sure you want to remove sync job?")
            positiveButton (text = "Confirm") { dialog ->
                // Check if current bucket (path) has been synchronized
                val task = taskBox.query().equal(Task_.source, "$selectedBucket/$selectedBucketPrefix").build().findFirst()

                // Assign menu item sync based on that
                if (task != null) {
                    taskBox.remove(task.id)
                }

                optionsMenu.findItem(R.id.menuEnableSync).isVisible = true
                optionsMenu.findItem(R.id.menuDisableSync).isVisible = false
            }
            negativeButton(text = "Cancel")
        }
    }

    private fun showDestinationPicker() {
        startActivityForResult(
            Intent().setAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
            INTENT_SELECT_DESTINATION_PATH_REQUEST_CODE
        )
    }

    internal inner class ActionBarCallBack : ActionMode.Callback {

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.item_download -> {
                    val notification = Notification()
                    notification.createChannel(
                        context!!,
                        false,
                        context!!.getString(R.string.app_name),
                        "App notification channel"
                    )
                    notification.create(
                        context!!,
                        "${context!!.packageName}-${context!!.getString(R.string.app_name)}",
                        "Downloading selected object(s)",
                        "",
                        selectExtension.selections.size
                    )

                    // Check if download location is set
                    if (user.defaultDownloadLocation != null) {
                        for (pos in selectExtension.selections) {
                            Timber.i("Selected bucket object ${fastAdapter.getItem(pos)?.objectName} from bucket ${selectedBucket} and prefix ${selectedBucketPrefix}")
                            Download().bucketObject(
                                selectedBucket,
                                selectedBucketPrefix,
                                fastAdapter.getItem(pos)?.objectName!!,
                                context!!,
                                coroutineScope,
                                minioClient,
                                activity!!,
                                user.defaultDownloadLocation!!,
                                notification
                            )
                        }
                    } else {
                        Snackbar.make(
                            activity!!.findViewById(R.id.root_layout),
                            "Default download location not set. Please select one in settings and try again",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                }
                R.id.item_delete -> {
                    val deleteCandidates = selectExtension.selections
                    MaterialDialog(context!!).show {
                        title(text = "Confirm deletion")
                        message(text = "This is a destructive action and will permanently delete files. Please confirm your action")
                        positiveButton(text = "Delete") {
                            for (pos in deleteCandidates) {
                                Timber.i("Selected bucket object ${fastAdapter.getItem(pos)?.objectName} from bucket ${selectedBucket} and prefix ${selectedBucketPrefix}")
                                Bucket().removeObject(
                                    coroutineScope,
                                    minioClient,
                                    selectedBucket,
                                    selectedBucketPrefix,
                                    fastAdapter.getItem(pos)?.objectName.orEmpty(),
                                    this@BucketListFragment
                                )
                            }
                        }
                        negativeButton(text = "Cancel")
                    }
                }
            }

            //as we no longer have a selection so the actionMode can be finished
            mode.finish()

            //we consume the event
            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.findItem(R.id.item_download).icon = IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_file_download).actionBar().colorInt(Color.WHITE)
            menu.findItem(R.id.item_delete).icon = IconicsDrawable(context!!).icon(GoogleMaterial.Icon.gmd_delete).actionBar().colorInt(Color.WHITE)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {}

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }
    }
}