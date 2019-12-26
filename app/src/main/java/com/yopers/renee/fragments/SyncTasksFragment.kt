package com.yopers.renee.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.yopers.renee.ObjectBox
import com.yopers.renee.R
import com.yopers.renee.TaskItem
import com.yopers.renee.models.Task
import com.yopers.renee.models.Task_
import com.yopers.renee.models.User
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import kotlinx.android.synthetic.main.fragment_sync_tasks.*
import moe.feng.common.view.breadcrumbs.BreadcrumbsView
import timber.log.Timber
import java.util.*

class SyncTasksFragment: Fragment() {
    var itemAdapter = ItemAdapter<TaskItem>()
    private lateinit var fastAdapter: FastAdapter<TaskItem>
    private lateinit var user: User
    private lateinit var toolbar: Toolbar
    private lateinit var mBreadcrumbsView: BreadcrumbsView
    private val taskBox: Box<Task> = ObjectBox.boxStore.boxFor()

    companion object {
        @JvmStatic
        fun newInstance(mConfig: User) =
            SyncTasksFragment().apply {
                arguments = Bundle().apply {
                    // putInt(ARG_COLUMN_COUNT, columnCount)
                    user = mConfig
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sync_tasks, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.tag("Sync Tasks Fragment")

        toolbar = activity!!.findViewById(R.id.toolbar)
        mBreadcrumbsView = activity!!.findViewById(R.id.breadcrumbs_view)

        mBreadcrumbsView.visibility = View.GONE

        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)

        fastAdapter.onClickListener = { view, adapter, item, position ->
            Timber.i("Clicked on sync task ${item.taskID}")
            showTaskEditDialog(item, position)
            true
        }

        taskList.adapter = fastAdapter

        loadTasks()

//        WorkManager.getInstance(context!!).cancelAllWork()
    }

    private fun loadTasks() {
        val tasks: List<Task> = taskBox.query().equal(Task_.userId, user.id).build().find()
        Timber.i("Found ${tasks.size} assigned background tasks")

        toolbar.subtitle = "${tasks.size} Synchronized Tasks"

        tasks.forEach {
            itemAdapter.add(TaskItem().build(it.id, it.taskId!!, it.source!!, it.destination!!))
        }
    }


    private fun showTaskEditDialog(item: TaskItem, position: Int) {
        MaterialDialog(context!!).show {
            title(text = "Modify Sync Task")
            negativeButton (text = "Remove") {
                Timber.i("Remove task id ${item.taskID}")

                // Remove task from work manager queue
                WorkManager.getInstance(context).cancelWorkById(UUID.fromString(item.taskID!!))
                Timber.i("Removed task from WorkManaget queue")

                // Remove task from database
                taskBox.remove(item.id!!)
                Timber.i("Removed task from database")

                // Update list adapter
                fastAdapter.notifyAdapterItemRemoved(position)
                fastAdapter.notifyAdapterDataSetChanged()
            }
            positiveButton (text = "Cancel") {

            }
        }
    }
}