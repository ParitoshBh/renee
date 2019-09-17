package com.yopers.renee

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class BucketItem : AbstractItem<BucketItem.ViewHolder>() {
    var name: String? = null
    var isDir: Boolean? = null

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int
        get() = R.id.fastadapter_sample_item_id

    /** defines the layout which will be used for this item in the list  */
    override val layoutRes: Int
        get() = R.layout.fragment_bucket_list

    fun build(name: String, isDir: Boolean): BucketItem {
        this.name = name
        this.isDir = isDir
        return this
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<BucketItem>(view) {
        var name: TextView = view.findViewById(R.id.item_number)
        var isDir: TextView = view.findViewById(R.id.isDirectory)

        override fun bindView(item: BucketItem, payloads: MutableList<Any>) {
            name.text = item.name
            isDir.text = item.isDir.toString()
        }

        override fun unbindView(item: BucketItem) {
            name.text = null
            isDir.text = null
        }
    }
}