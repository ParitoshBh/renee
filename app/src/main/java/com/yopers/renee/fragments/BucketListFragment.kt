package com.yopers.renee.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.helpers.ActionModeHelper
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import com.mikepenz.materialize.util.UIUtils
import com.yopers.renee.BucketItem
import com.yopers.renee.MainActivity
import com.yopers.renee.R
import com.yopers.renee.utils.Bucket
import com.yopers.renee.utils.Database
import com.yopers.renee.utils.Download
import com.yopers.renee.utils.Upload
import io.minio.MinioClient
import io.minio.Result
import io.minio.messages.Item
import kotlinx.android.synthetic.main.object_list.*
import kotlinx.coroutines.*
import moe.feng.common.view.breadcrumbs.BreadcrumbsView
import moe.feng.common.view.breadcrumbs.DefaultBreadcrumbsCallback
import moe.feng.common.view.breadcrumbs.model.BreadcrumbItem
import timber.log.Timber
import java.lang.Exception

class BucketListFragment: Fragment() {
    private val INTENT_SELECT_PATH_REQUEST_CODE = 110
    private val INTENT_SELECT_FILE_REQUEST_CODE = 111
    var itemAdapter = ItemAdapter<BucketItem>()
    private lateinit var minioClient: MinioClient
    private lateinit var fastAdapter: FastAdapter<BucketItem>
    private lateinit var selectedBucket: String
    private var selectedBucketPrefix = ""
    private lateinit var userConfig: MutableMap<String, String>
    private lateinit var toolbar: Toolbar
    private lateinit var mBreadcrumbsView: BreadcrumbsView
    private lateinit var mActionModeHelper: ActionModeHelper<BucketItem>
    private lateinit var selectExtension: SelectExtension<BucketItem>

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    companion object {

        @JvmStatic
        fun newInstance(bucketName: String, mClient: MinioClient, mConfig: Map<String, String>) =
            BucketListFragment().apply {
                arguments = Bundle().apply {
                    // putInt(ARG_COLUMN_COUNT, columnCount)
                    selectedBucket = bucketName
                    minioClient = mClient
                    userConfig = mConfig.toMutableMap()
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
        return inflater.inflate(R.layout.object_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.tag("Minio: List Fragment")

        speedDial.inflate(R.menu.fab)
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
        selectExtension.multiSelect = false
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
                        userConfig.put("downloadLocation", data.data.toString())
                        Database().write("userConfig", userConfig)
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
                    Upload().bucketObject(data.data!!, context!!, coroutineScope, minioClient,
                        selectedBucket, selectedBucketPrefix, this)
                } else {
                    // Failed to save setting
                    Snackbar.make(
                        activity!!.findViewById(R.id.root_layout),
                        "Failed to select a file. Please try again",
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
            val bucketObjects = getOriginalBitmapAsync(selectedBucket, selectedBucketPrefix, minioClient)
            Timber.i("Bucket objects empty ${bucketObjects.isEmpty()}")
            onDataLoaded(bucketObjects, selectedBucketPrefix, selectedBucket)
        }
    }

    private suspend fun getOriginalBitmapAsync(selectedBucket: String, selectedBucketPrefix: String, minioClient: MinioClient) =
        withContext(Dispatchers.IO) {
            var bucketObjects: List<Result<Item>> = emptyList()
            try {
                bucketObjects = minioClient.listObjects(selectedBucket, selectedBucketPrefix, false).toList()
            } catch (e: Exception) {
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

    internal inner class ActionBarCallBack : ActionMode.Callback {

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.item_download -> {
                    for (pos in selectExtension.selections) {
                        Timber.i("Selected bucket object ${fastAdapter.getItem(pos)?.objectName} from bucket ${selectedBucket} and prefix ${selectedBucketPrefix}")
                        if (userConfig["downloadLocation"].isNullOrEmpty()) {
                            startActivityForResult(
                                Intent().setAction(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                INTENT_SELECT_PATH_REQUEST_CODE
                            )
                        } else {
                            Download().bucketObject(
                                selectedBucket,
                                selectedBucketPrefix,
                                fastAdapter.getItem(pos)?.objectName!!,
                                context!!,
                                coroutineScope,
                                minioClient,
                                activity!!,
                                userConfig["downloadLocation"]!!
                            )
                        }
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
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {}

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }
    }
}