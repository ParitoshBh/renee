package com.yopers.renee

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.ui.utils.FastAdapterUIUtils
import com.mikepenz.materialize.util.UIUtils

open class BucketItem : AbstractItem<BucketItem.ViewHolder>() {
    var objectName: String? = null
    var isDir: Boolean? = null
    var objectSize: String? = null
    var objectPath: String? = null

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int
        get() = R.id.fastadapter_sample_item_id

    /** defines the layout which will be used for this item in the list  */
    override val layoutRes: Int
        get() = R.layout.fragment_bucket_list

    fun build(name: String, isDir: Boolean, objectSize: String, objectPath: String): BucketItem {
        this.objectName = name
        this.isDir = isDir
        this.objectSize = objectSize
        this.objectPath = objectPath
        return this
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(private var view: View) : FastAdapter.ViewHolder<BucketItem>(view) {
        var objectName: TextView = view.findViewById(R.id.objectName)
        var objectSize: TextView = view.findViewById(R.id.objectSize)

        override fun bindView(item: BucketItem, payloads: MutableList<Any>) {
            //get the context
            val ctx = itemView.context

            //set the background for the item
            UIUtils.setBackground(
                view,
                FastAdapterUIUtils.getSelectableBackground(
                    ctx,
                    ctx.resources.getColor(R.color.colorPrimaryLight),
                    true
                )
            )

            objectName.text = item.objectName
            objectSize.text = item.objectSize
        }

        override fun unbindView(item: BucketItem) {
            objectName.text = null
            objectSize.text = null
        }
    }
}