package com.dublikunt.nclient

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Priority
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.components.activities.GeneralActivity
import com.dublikunt.nclient.components.views.ZoomFragment
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.settings.DefaultDialogs
import com.dublikunt.nclient.settings.DefaultDialogs.CustomDialogResults
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.LogUtility
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import java.io.File


class ZoomActivity : GeneralActivity() {
    private lateinit var gallery: GenericGallery
    private var actualPage = 0
    private var isHidden = false
    private lateinit var mViewPager: ViewPager2
    private lateinit var pageManagerLabel: MaterialTextView
    private lateinit var cornerPageViewer: MaterialTextView
    private lateinit var pageSwitcher: View
    private lateinit var seekBar: SeekBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var view: View
    private var directory: GalleryFolder? = null

    @ViewPager2.Orientation
    private var tmpScrollType = 0
    private var up = false
    private var down = false
    private var side = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("Settings", 0)
        side = preferences.getBoolean(VOLUME_SIDE_KEY, true)
        setContentView(R.layout.activity_zoom)

        //read arguments
        gallery = intent.getParcelableExtra("$packageName.GALLERY")!!
        val page = intent.extras!!.getInt("$packageName.PAGE", 1) - 1
        directory = gallery.galleryFolder
        //toolbar setup
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        title = gallery.title
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Global.isLockScreen) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        //find views
        val mSectionsPagerAdapter: SectionsPagerAdapter = SectionsPagerAdapter(this)
        mViewPager = findViewById(R.id.container)
        mViewPager.adapter = mSectionsPagerAdapter
        mViewPager.orientation = preferences.getInt(
            SCROLL_TYPE_KEY,
            ScrollType.HORIZONTAL.ordinal
        )
        mViewPager.offscreenPageLimit = Global.offscreenLimit
        pageSwitcher = findViewById(R.id.page_switcher)
        pageManagerLabel = findViewById(R.id.pages)
        cornerPageViewer = findViewById(R.id.page_text)
        seekBar = findViewById(R.id.seekBar)
        view = findViewById(R.id.view)

        //initial setup for views
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        mViewPager.keepScreenOn = Global.isLockScreen
        findViewById<View>(R.id.prev).setOnClickListener { changeClosePage(false) }
        findViewById<View>(R.id.next).setOnClickListener { changeClosePage(true) }
        seekBar.max = gallery.pageCount - 1
        if (Global.useRtl()) {
            seekBar.rotationY = 180f
            mViewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL
        }

        //Adding listeners
        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(newPage: Int) {
                val oldPage = actualPage
                actualPage = newPage
                LogUtility.download("Page selected: $newPage from page $oldPage")
                setPageText(newPage + 1)
                seekBar.progress = newPage
                clearFarRequests(oldPage, newPage)
                makeNearRequests(newPage)
            }
        })
        pageManagerLabel.setOnClickListener {
            DefaultDialogs.pageChangerDialog(
                DefaultDialogs.Builder(this)
                    .setActual(actualPage + 1)
                    .setMin(1)
                    .setMax(gallery.pageCount)
                    .setTitle(R.string.change_page)
                    .setDrawable(R.drawable.ic_find_in_page)
                    .setDialogs(object : CustomDialogResults() {
                        override fun positive(actual: Int) {
                            changePage(actual - 1)
                        }
                    })
            )
        }
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setPageText(progress + 1)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                changePage(seekBar.progress)
            }
        })
        changePage(page)
        setPageText(page + 1)
        seekBar.progress = page
    }

    private fun setUserInput(enabled: Boolean) {
        mViewPager.isUserInputEnabled = enabled
    }

    private fun setPageText(page: Int) {
        pageManagerLabel.text = getString(R.string.page_format, page, gallery.pageCount)
        cornerPageViewer.text = getString(R.string.page_format, page, gallery.pageCount)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (Global.volumeOverride()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> up = false
                KeyEvent.KEYCODE_VOLUME_DOWN -> down = false
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (Global.volumeOverride()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    up = true
                    changeClosePage(side)
                    if (up && down) changeSide()
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    down = true
                    changeClosePage(!side)
                    if (up && down) changeSide()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun changeSide() {
        getSharedPreferences("Settings", 0).edit()
            .putBoolean(VOLUME_SIDE_KEY, !side.also { side = it }).apply()
        Toast.makeText(
            this,
            if (side) R.string.next_page_volume_up else R.string.next_page_volume_down,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun changeClosePage(next: Boolean) {
        var next = next
        if (Global.useRtl()) next = !next
        if (next && mViewPager.currentItem < mViewPager.adapter!!.itemCount - 1) changePage(
            mViewPager.currentItem + 1
        )
        if (!next && mViewPager.currentItem > 0) changePage(mViewPager.currentItem - 1)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changeLayout(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun hardwareKeys(): Boolean {
        return ViewConfiguration.get(this).hasPermanentMenuKey()
    }

    private fun applyMargin(landscape: Boolean, view: View?) {
        val lp = view!!.layoutParams as ConstraintLayout.LayoutParams
        lp.setMargins(
            0,
            0,
            if (landscape && !hardwareKeys()) Global.getNavigationBarHeight(this) else 0,
            0
        )
        view.layoutParams = lp
    }

    fun geViewPager(): ViewPager2? {
        return mViewPager
    }

    private fun changeLayout(landscape: Boolean) {
        val statusBarHeight = Global.getStatusBarHeight(this)
        applyMargin(landscape, findViewById(R.id.master_layout))
        applyMargin(landscape, toolbar)
        pageSwitcher.setPadding(0, 0, 0, if (landscape) 0 else statusBarHeight)
    }

    private fun changePage(newPage: Int) {
        mViewPager.currentItem = newPage
    }

    private fun changeScrollTypeDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val scrollType = mViewPager.orientation
        tmpScrollType = mViewPager.orientation
        builder.setTitle(getString(R.string.change_scroll_type) + ":")
        builder.setSingleChoiceItems(
            R.array.scroll_type,
            scrollType
        ) { _: DialogInterface?, which: Int -> tmpScrollType = which }
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            if (tmpScrollType != scrollType) {
                mViewPager.orientation = tmpScrollType
                getSharedPreferences("Settings", 0).edit().putInt(SCROLL_TYPE_KEY, tmpScrollType)
                    .apply()
                val page = actualPage
                changePage(page + 1)
                changePage(page)
            }
        }.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_zoom, menu)
        Utility.tintMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.rotate) {
            actualFragment!!.rotate()
        } else if (id == R.id.save_page) {
            if (Global.hasStoragePermission(this)) {
                downloadPage()
            } else requestStorage()
        } else if (id == R.id.share) {
            if (gallery.id <= 0) sendImage(false) else openSendImageDialog()
        } else if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.bookmark) {
            Queries.ResumeTable.insert(gallery.id, actualPage + 1)
        } else if (id == R.id.scrollType) {
            changeScrollTypeDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openSendImageDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            sendImage(
                true
            )
        }
            .setNegativeButton(R.string.no) { _: DialogInterface?, _: Int ->
                sendImage(
                    false
                )
            }
            .setCancelable(true).setTitle(R.string.send_with_title)
            .setMessage(R.string.caption_send_with_title)
            .show()
    }

    private fun requestStorage() {
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Global.initStorage(this)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) downloadPage()
    }

    private val actualFragment: ZoomFragment?
        get() = getActualFragment(mViewPager.currentItem)

    private fun makeNearRequests(newPage: Int) {
        var fragment: ZoomFragment?
        val offScreenLimit = Global.offscreenLimit
        for (i in newPage - offScreenLimit..newPage + offScreenLimit) {
            fragment = getActualFragment(i)
            if (fragment == null) continue
            if (i == newPage) fragment.loadImage(Priority.IMMEDIATE) else fragment.loadImage()
        }
    }

    private fun clearFarRequests(oldPage: Int, newPage: Int) {
        var fragment: ZoomFragment?
        val offScreenLimit = Global.offscreenLimit
        for (i in oldPage - offScreenLimit..oldPage + offScreenLimit) {
            if (i >= newPage - offScreenLimit && i <= newPage + offScreenLimit) continue
            fragment = getActualFragment(i)
            if (fragment == null) continue
            fragment.cancelRequest()
        }
    }

    private fun getActualFragment(position: Int): ZoomFragment? {
        return supportFragmentManager.findFragmentByTag("f$position") as ZoomFragment?
    }

    private fun sendImage(withText: Boolean) {
        val pageNum = mViewPager.currentItem
        Utility.sendImage(
            this,
            actualFragment!!.drawable,
            if (withText) gallery.sharePageUrl(pageNum) else null
        )
    }

    private fun downloadPage() {
        val output = File(
            Global.SCREENFOLDER,
            gallery.id.toString() + "-" + (mViewPager.currentItem + 1) + ".jpg"
        )
        Utility.saveImage(actualFragment!!.drawable, output)
    }

    private fun animateLayout() {
        val adapter: AnimatorListenerAdapter = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (isHidden) {
                    pageSwitcher.visibility = View.GONE
                    toolbar.visibility = View.GONE
                    view.visibility = View.GONE
                    cornerPageViewer.visibility = View.VISIBLE
                }
            }
        }
        pageSwitcher.visibility = View.VISIBLE
        toolbar.visibility = View.VISIBLE
        view.visibility = View.VISIBLE
        cornerPageViewer.visibility = View.GONE
        pageSwitcher.animate().alpha(if (isHidden) 0f else 0.75f).setDuration(150)
            .setListener(adapter).start()
        view.animate().alpha(if (isHidden) 0f else 0.75f).setDuration(150).setListener(adapter)
            .start()
        toolbar.animate().alpha(if (isHidden) 0f else 0.75f).setDuration(150).setListener(adapter)
            .start()
    }

    private fun applyVisibilityFlag() {
        if (isHidden) {
            hideSystemUI(window.decorView)
        } else {
            showSystemUI(window.decorView)
        }
    }

    private enum class ScrollType {
        HORIZONTAL, VERTICAL
    }

    inner class SectionsPagerAdapter(activity: ZoomActivity) :
        FragmentStateAdapter(activity.supportFragmentManager, activity.lifecycle) {
        private var allowScroll = true
        override fun createFragment(position: Int): Fragment {
            val f = ZoomFragment.newInstance(gallery, position, directory)
            f.setZoomChangeListener(object : ZoomFragment.OnZoomChangeListener {
                override fun onZoomChange(v: View?, zoomLevel: Float) {
                    try {
                        val Scroll = zoomLevel < 1.1f
                        if (Scroll != allowScroll) {
                            setUserInput(!allowScroll)
                            allowScroll = Scroll
                        }
                    } catch (ignored: Exception) {
                    }
                }
            })

            f.setClickListener {
                isHidden = !isHidden
                LogUtility.download("Clicked $isHidden")
                applyVisibilityFlag()
                animateLayout()
            }
            return f
        }

        override fun getItemCount(): Int {
            return gallery.pageCount
        }
    }

    private fun hideSystemUI(view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI(view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        private const val VOLUME_SIDE_KEY = "volumeSide"
        private const val SCROLL_TYPE_KEY = "zoomScrollType"
    }
}
