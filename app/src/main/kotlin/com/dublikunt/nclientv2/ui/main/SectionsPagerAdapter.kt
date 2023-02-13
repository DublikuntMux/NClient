package com.dublikunt.nclientv2.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dublikunt.nclientv2.StatusViewerActivity
import com.dublikunt.nclientv2.async.database.Queries
import com.dublikunt.nclientv2.components.status.StatusManager

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(context: StatusViewerActivity) :
    FragmentStateAdapter(context.supportFragmentManager, context.lifecycle) {
    private var statuses: List<String> = StatusManager.names
    private var counts: HashMap<String, Int> = Queries.StatusMangaTable.getCountsPerStatus()

    fun getPageTitle(position: Int): CharSequence? {
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
