package com.yopers.renee

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class TaskItem : AbstractItem<TaskItem.ViewHolder>() {
    var id: Long? = null
    var taskID: String? = null
    var taskSource: String? = null
    var taskDestination: String? = null

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int
        get() = R.id.fastadapter_sample_item_id

    /** defines the layout which will be used for this item in the list  */
    override val layoutRes: Int
        get() = R.layout.item_sync_task

    fun build(id: Long, name: String, source: String, destination: String): TaskItem {
        this.id = id
        this.taskID = name
        this.taskSource = source
        this.taskDestination = destination

        return this
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(private var view: View) : FastAdapter.ViewHolder<TaskItem>(view) {
        var taskID: TextView = view.findViewById(R.id.taskName)
        var taskSource: TextView = view.findViewById(R.id.taskSource)
        var taskDestination: TextView = view.findViewById(R.id.taskDestination)

        override fun bindView(item: TaskItem, payloads: MutableList<Any>) {
            taskID.text = item.taskID
            taskSource.text = item.taskSource
            taskDestination.text = item.taskDestination
        }

        override fun unbindView(item: TaskItem) {
            taskID.text = null
            taskSource.text = null
            taskDestination.text = null
        }
    }
}