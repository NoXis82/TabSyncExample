package com.noxis.tabsyncsample.mediator

import androidx.recyclerview.widget.RecyclerView

/**
 * будет наблюдать за изменениями в recyclerView и сразу перезаполнять TabLayout
 */
class PagerAdapterObserver(private val populateTabsFromPagerAdapter: () -> Unit) :
    RecyclerView.AdapterDataObserver() {

    override fun onChanged() {
        populateTabsFromPagerAdapter()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        populateTabsFromPagerAdapter()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        populateTabsFromPagerAdapter()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        populateTabsFromPagerAdapter()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        populateTabsFromPagerAdapter()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        populateTabsFromPagerAdapter()
    }
}