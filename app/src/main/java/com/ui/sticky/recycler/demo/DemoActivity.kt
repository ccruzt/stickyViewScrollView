package com.ui.sticky.recycler.demo

import android.os.Bundle
import com.ui.sticky.recycler.demo.databinding.BottomScrollingBinding
import com.ui.sticky.recycler.demo.sticky.adapter.NonStickyItem
import com.ui.sticky.recycler.demo.sticky.adapter.StickyItem
import com.ui.sticky.recycler.demo.sticky.adapter.StickyRecyclerViewAdapter

class DemoActivity : BaseActivity<BottomScrollingBinding>() {

    private lateinit var stickyAdapter: StickyRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        initRecyclerView()
    }

    private fun initRecyclerView() {
        val stickyList = listOf(
            StickyItem(text = "Header 1"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Header 2"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
            NonStickyItem(text = "Item"),
        )

        /*stickyAdapter = StickyRecyclerViewAdapter(stickyList)
        binding.rvSticky.apply {
            layoutManager = LinearLayoutManager(context)
            binding.rvSticky.isNestedScrollingEnabled = false
            adapter = stickyAdapter
            addItemDecoration(
                HeaderItemDecoration(this, false) {
                    this.adapter?.getItemViewType(it) == R.layout.item_sticky
                }
            )
        }*/
    }

    override fun getBindingClass(): BottomScrollingBinding {
        return BottomScrollingBinding.inflate(layoutInflater)
    }

}