package com.dublikunt.nclient

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.dublikunt.nclient.adapters.TagsAdapter
import com.dublikunt.nclient.components.activities.GeneralActivity
import com.dublikunt.nclient.components.widgets.CustomGridLayoutManager
import com.dublikunt.nclient.components.widgets.TagTypePage
import com.dublikunt.nclient.settings.DefaultDialogs
import com.dublikunt.nclient.settings.DefaultDialogs.CustomDialogResults
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.settings.TagV2
import com.dublikunt.nclient.utility.LogUtility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy

class TagFilterActivity : GeneralActivity() {
    private lateinit var searchView: SearchView
    private lateinit var mViewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_filter)

        //init toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        val mTagTypePageAdapter = TagTypePageAdapter(this)
        mViewPager = findViewById(R.id.container)
        mViewPager.adapter = mTagTypePageAdapter
        mViewPager.offscreenPageLimit = 1
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        LogUtility.download("ISNULL?" + (tabLayout == null))
        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val page = getFragment(position)
                if (page != null) {
                    (page.recyclerView?.adapter as TagsAdapter?)!!.addItem()
                }
            }
        })
        TabLayoutMediator(
            tabLayout!!,
            mViewPager,
            TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                var id = 0
                when (position) {
                    0 -> id = R.string.applied_filters
                    1 -> id = R.string.tags
                    2 -> id = R.string.artists
                    3 -> id = R.string.characters
                    4 -> id = R.string.parodies
                    5 -> id = R.string.groups
                    6 -> id = R.string.online_tags
                }
                tab.setText(id)
            }).attach()
        mViewPager.currentItem = page
    }

    private val actualFragment: TagTypePage?
        get() = getFragment(mViewPager.currentItem)

    private fun getFragment(position: Int): TagTypePage? {
        return supportFragmentManager.findFragmentByTag("f$position") as TagTypePage?
    }

    private val page: Int
        get() {
            val data = intent.data
            if (data != null) {
                val params = data.pathSegments
                for (x in params) LogUtility.info(x)
                if (params.size > 0) {
                    when (params[0]) {
                        "tags" -> return 1
                        "artists" -> return 2
                        "characters" -> return 3
                        "parodies" -> return 4
                        "groups" -> return 5
                    }
                }
            }
            return 0
        }

    private fun updateSortItem(item: MenuItem) {
        item.setIcon(if (TagV2.isSortedByName()) R.drawable.ic_sort_by_alpha else R.drawable.ic_sort)
        item.setTitle(if (TagV2.isSortedByName()) R.string.sort_by_title else R.string.sort_by_popular)
        Global.setTint(item.icon)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tag_filter, menu)
        updateSortItem(menu.findItem(R.id.sort_by_name))
        searchView = (menu.findItem(R.id.search).actionView as SearchView?)!!
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                actualFragment?.refilter(newText)
                return true
            }
        })
        return true
    }

    private fun createDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.are_you_sure).setMessage(getString(R.string.clear_this_list))
            .setIcon(R.drawable.ic_help)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            val page = actualFragment
            page?.reset()
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val page = actualFragment
        when (id) {
            R.id.reset_tags -> createDialog()
            R.id.set_min_count -> minCountBuild()
            R.id.sort_by_name -> {
                TagV2.updateSortByName(this)
                updateSortItem(item)
                page?.refilter(searchView.query.toString())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun minCountBuild() {
        val min = TagV2.getMinCount()
        val builder = DefaultDialogs.Builder(this)
        builder.setActual(min).setMax(100).setMin(2)
        builder.setYesbtn(R.string.ok).setNobtn(R.string.cancel)
        builder.setTitle(R.string.set_minimum_count).setDialogs(object : CustomDialogResults() {
            override fun positive(actual: Int) {
                LogUtility.download("ACTUAL: $actual")
                TagV2.updateMinCount(this@TagFilterActivity, actual)
                actualFragment?.changeSize()
            }
        })
        DefaultDialogs.pageChangerDialog(builder)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            changeLayout(false)
        }
    }

    private fun changeLayout(landscape: Boolean) {
        val count = if (landscape) 4 else 2
        val page = actualFragment
        if (page != null) {
            val recycler = page.recyclerView
            if (recycler != null) {
                val adapter = recycler.adapter
                val gridLayoutManager = CustomGridLayoutManager(this, count)
                recycler.layoutManager = gridLayoutManager
                recycler.adapter = adapter
            }
        }
    }

    internal class TagTypePageAdapter(activity: TagFilterActivity) :
        FragmentStateAdapter(activity.supportFragmentManager, activity.lifecycle) {
        override fun createFragment(position: Int): Fragment {
            return TagTypePage.newInstance(position)
        }

        override fun getItemCount(): Int {
            return if (Login.isLogged()) 7 else 6
        }
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
