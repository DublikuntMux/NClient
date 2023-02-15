package com.dublikunt.nclient.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dublikunt.nclient.StatusViewerActivity
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.status.StatusManager

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(context: StatusViewerActivity) :
    FragmentStateAdapter(context.supportFragmentManager, context.lifecycle) {
    private var statuses: List<String> = StatusManager.names
    private var counts: HashMap<String, Int> = Queries.StatusMangaTable.countsPerStatus

    fun getPageTitle(position: Int): CharSequence {
        val status = statuses[position]
        var count = 0
        if (counts.containsKey(status)) {
            count = counts[status]!!
        }
        return String.format("%s - %d", status, count)
    }

    override fun createFragment(position: Int): Fragment {
        return PlaceholderFragment.newInstance(statuses[position])
    }

    override fun getItemCount(): Int {
        return statuses.size
    }
}
