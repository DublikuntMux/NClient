package com.dublikunt.nclient.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dublikunt.nclient.StatusViewerActivity
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.status.StatusManager
import java.util.Locale

class SectionsPagerAdapter(context: StatusViewerActivity) :
    FragmentStateAdapter(context.supportFragmentManager, context.lifecycle) {
    private var statuses: List<String> = StatusManager.getNames()

    fun getPageTitle(position: Int): CharSequence? {
        val status = statuses[position]
        val count = Queries.StatusMangaTable.getCountPerStatus(status)
        return String.format(Locale.US, "%s - %d", status, count)
    }

    override fun createFragment(position: Int): Fragment {
        return PlaceholderFragment.newInstance(statuses[position])
    }

    override fun getItemCount(): Int {
        return statuses.size
    }
}
