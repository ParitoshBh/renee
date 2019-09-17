package com.yopers.renee

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.helpers.ActionModeHelper
import com.mikepenz.fastadapter.select.getSelectExtension
import io.minio.MinioClient
import io.minio.Result
import io.minio.messages.Item
import kotlinx.android.synthetic.main.myquote_list.*
import moe.feng.common.view.breadcrumbs.BreadcrumbsView
import moe.feng.common.view.breadcrumbs.model.BreadcrumbItem

class MyQuoteListFragment: Fragment(), BackgroundBucketTask.DataLoadListener {
    var itemAdapter = ItemAdapter<BucketItem>()
    private lateinit var minioClient: MinioClient
    private lateinit var fastAdapter: FastAdapter<BucketItem>
    private lateinit var selectedBucket: String
    private lateinit var toolbar: Toolbar
    private lateinit var mBreadcrumbsView: BreadcrumbsView
    private lateinit var mActionModeHelper: ActionModeHelper<BucketItem>

    companion object {

        @JvmStatic
        fun newInstance(bucketName: String, mClient: MinioClient) =
            MyQuoteListFragment().apply {
                arguments = Bundle().apply {
                    // putInt(ARG_COLUMN_COUNT, columnCount)
                    selectedBucket = bucketName
                    minioClient = mClient
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
        return inflater.inflate(R.layout.myquote_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)

        // Gets (or creates and attaches if not yet existing) the extension from the given `FastAdapter`
        val selectExtension = fastAdapter.getSelectExtension()
        selectExtension.isSelectable = true
        selectExtension.multiSelect = true
        selectExtension.selectOnLongClick = true

        fastAdapter.onPreClickListener = { _: View?, _: IAdapter<BucketItem>, item: BucketItem, _: Int ->
            //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
            val res = mActionModeHelper.onClick(item)
            res ?: false
        }

        fastAdapter.onLongClickListener = { view, adapter, item, position ->
            if (item.isSelected) {
                view.setBackgroundColor(Color.GREEN)
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
            false
        }

        fastAdapter.onClickListener = { view, adapter, item, position ->
            if (item.isDir as Boolean) {
                loadBucketObjects(item.name.orEmpty())
            }
            false
        }

        fastAdapter.onPreLongClickListener = { _: View, _: IAdapter<BucketItem>, _: BucketItem, position: Int ->
            val actionMode = mActionModeHelper.onLongClick(activity as MainActivity, position)
            if (actionMode != null) {
                //we want color our CAB
//                findViewById<View>(R.id.action_mode_bar).setBackgroundColor(UIUtils.getThemeColorFromAttrOrRes(this@MultiselectSampleActivity, R.attr.colorPrimary, R.color.material_drawer_primary))
            }
            //if we have no actionMode we do not consume the event
            actionMode != null
        }

        //we init our ActionModeHelper
        mActionModeHelper = ActionModeHelper(fastAdapter, R.menu.cab, ActionBarCallBack())

        toolbar = activity!!.findViewById(R.id.toolbar)
        mBreadcrumbsView = activity!!.findViewById(R.id.breadcrumbs_view)

//        breadcrumbs_view.setCallback(object : DefaultBreadcrumbsCallback<BreadcrumbItem>() {
//            override fun onNavigateBack(item: BreadcrumbItem, position: Int) {
////                val nTask = BucketTask()
////                nTask.rvList = list
////                nTask.adapter = adapter
////                nTask.bucket = bucket
////                nTask.bucketPath = item.getSelectedItem()
////                nTask.mToolbar = toolbar
////                nTask.breadcrumbs_view = breadcrumbs_view
////                nTask.execute(minioClient)
//            }
//
//            override fun onNavigateNewLocation(newItem: BreadcrumbItem, changedPosition: Int) {
////                println("Minio breadcrumb" + newItem.getSelectedItem())
//            }
//        })

        list.adapter = fastAdapter

        loadBucketObjects("")
    }

    private fun loadBucketObjects(selectedBucketPrefix: String) {
        val task = BackgroundBucketTask(selectedBucket, selectedBucketPrefix, this)
        task.execute(minioClient)
    }

    override fun onDataLoaded(results: List<Result<Item>>?, bucketPrefix: String, bucket: String) {
        toolbar.subtitle = results?.size.toString() + " objects"

        buildBreadcrumbs(bucket, bucketPrefix)

        itemAdapter.clear()
        for ((index, bucketObj) in results!!.withIndex()) {
            itemAdapter.add(BucketItem().build(bucketObj.get().objectName(), bucketObj.get().isDir))
        }
//        adapter.replaceItems(result.orEmpty())

        fastAdapter.notifyAdapterDataSetChanged()
    }

    private fun buildBreadcrumbs(bucketName: String, bucketPrefix: String) {
        if (bucketPrefix.isEmpty()) {
            mBreadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(bucketName))
        } else {
            val path = bucketPrefix.split('/')
            mBreadcrumbsView.addItem(BreadcrumbItem.createSimpleItem(path[path.size - 2]))
        }
    }

    /**
     * Our ActionBarCallBack to showcase the CAB
     */
    internal inner class ActionBarCallBack : ActionMode.Callback {

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
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