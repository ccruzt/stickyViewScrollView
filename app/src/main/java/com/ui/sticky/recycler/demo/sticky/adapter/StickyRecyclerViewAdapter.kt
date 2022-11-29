package com.ui.sticky.recycler.demo.sticky.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ui.sticky.recycler.demo.R

class StickyRecyclerViewAdapter(
    private val list: List<BaseSticky>
) : RecyclerView.Adapter<StickyRecyclerViewAdapter.BaseStickyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseStickyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_non_sticky -> NormalViewHolder(view)
            R.layout.item_sticky -> {
                view.tag = "sticky"
                HeaderViewHolder(view)
            }
            else -> NormalViewHolder(view)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: BaseStickyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is NonStickyItem -> R.layout.item_non_sticky
            is StickyItem -> R.layout.item_sticky
            else -> R.layout.item_non_sticky
        }
    }

    abstract inner class BaseStickyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: BaseSticky)
    }

    inner class HeaderViewHolder(itemView: View) : BaseStickyViewHolder(itemView) {
        override fun bind(item: BaseSticky) {
            val textView = itemView.findViewById<TextView>(R.id.tv_item_sticky)
//            textView.tag = "sticky"
            textView.text = item.text
        }
    }

    inner class NormalViewHolder(itemView: View) : BaseStickyViewHolder(itemView) {
        override fun bind(item: BaseSticky) {
            val textView = itemView.findViewById<TextView>(R.id.tv_item_non_sticky)
            textView.text = item.text
        }
    }
    inner class AnotherItemInStickyViewHolder(itemView: View) : BaseStickyViewHolder(itemView) {
        override fun bind(item: BaseSticky) {
        }
    }
}
