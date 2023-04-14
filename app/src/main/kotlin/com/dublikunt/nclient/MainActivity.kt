package com.dublikunt.nclient

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.adapters.ListAdapter
import com.dublikunt.nclient.api.Inspector
import com.dublikunt.nclient.api.Inspector.*
import com.dublikunt.nclient.api.Inspector.Companion.basicInspector
import com.dublikunt.nclient.api.Inspector.Companion.favoriteInspector
import com.dublikunt.nclient.api.Inspector.Companion.galleryInspector
import com.dublikunt.nclient.api.Inspector.Companion.randomInspector
import com.dublikunt.nclient.api.Inspector.Companion.searchInspector
import com.dublikunt.nclient.api.Inspector.Companion.tagInspector
import com.dublikunt.nclient.api.comments.Ranges
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.api.gallerys.Gallery
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.async.downloader.DownloadGallery
import com.dublikunt.nclient.components.GlideX
import com.dublikunt.nclient.components.activities.BaseActivity
import com.dublikunt.nclient.components.views.PageSwitcher
import com.dublikunt.nclient.components.views.PageSwitcher.DefaultPageChanger
import com.dublikunt.nclient.components.widgets.CustomGridLayoutManager
import com.dublikunt.nclient.enums.*
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.ThemeScheme
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.settings.Tags
import com.dublikunt.nclient.utility.ImageDownloadUtility
import com.dublikunt.nclient.utility.LogUtility
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import java.util.*


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val startGallery: InspectorResponse = object : MainInspectorResponse() {
        override fun onSuccess(galleries: List<GenericGallery>) {
            val g = if (galleries.size == 1) galleries[0] as Gallery else Gallery.emptyGallery()
            val intent = Intent(this@MainActivity, GalleryActivity::class.java)
            LogUtility.download(g.toString())
            intent.putExtra("$packageName.GALLERY", g)
            runOnUiThread {
                startActivity(intent)
                finish()
            }
            LogUtility.download("STARTED")
        }
    }
    private val changeLanguageTimeHandler = Handler(Looper.myLooper()!!)
    lateinit var adapter: ListAdapter
    private val addDataset: InspectorResponse = object : MainInspectorResponse() {
        override fun onSuccess(galleries: List<GenericGallery>) {
            adapter.addGalleries(galleries)
        }
    }

    lateinit var loginItem: MenuItem
    private lateinit var onlineFavoriteManager: MenuItem
    private var failCount = 0
    private lateinit var inspector: Inspector
    private lateinit var navigationView: NavigationView
    private var modeType = ModeType.UNKNOWN
    private var idOpenedGallery = -1
    private var inspecting = false
    private var filteringTag = false
    private lateinit var temporaryType: SortType
    private var snackbar: Snackbar? = null
    private var showedCaptcha = false
    private var noNeedForCaptcha = false
    private lateinit var pageSwitcher: PageSwitcher
    private val resetDataset: InspectorResponse = object : MainInspectorResponse() {
        override fun onSuccess(galleries: List<GenericGallery>) {
            super.onSuccess(galleries)
            adapter.restartDataset(galleries)
            showPageSwitcher(inspector.page, inspector.pageCount)
            runOnUiThread { recycler.smoothScrollToPosition(0) }
        }
    }
    private val changeLanguageRunnable = Runnable {
        useNormalMode()
        inspector.start()
    }
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var setting: Setting? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectStartMode(intent, packageName)
        LogUtility.download("Main started with mode $modeType")

        findUsefulViews()
        initializeToolbar()
        initializeNavigationView()
        initializeRecyclerView()
        initializePageSwitcherActions()
        loadStringLogin()
        refresher.setOnRefreshListener {
            inspector = inspector.cloneInspector(this@MainActivity, resetDataset)
            inspector.page = 1
            inspector.start()
        }
        manageDrawer()
        setActivityTitle()
        inspector.start()
    }

    private fun manageDrawer() {
        if (modeType != ModeType.NORMAL) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            val toggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
        }
    }

    private fun setActivityTitle() {
        when (modeType) {
            ModeType.FAVORITE -> supportActionBar!!.setTitle(R.string.favorite_online_manga)
            ModeType.SEARCH -> supportActionBar!!.title =
                inspector.searchTitle
            ModeType.TAG -> supportActionBar!!.title =
                inspector.tag.name
            ModeType.NORMAL -> supportActionBar!!.setTitle(com.franmontiel.persistentcookiejar.R.string.app_name)
            else -> supportActionBar!!.title = "WTF"
        }
    }

    private fun initializePageSwitcherActions() {
        pageSwitcher.setChanger(object : DefaultPageChanger() {
            override fun pageChanged(switcher: PageSwitcher, page: Int) {
                inspector = inspector.cloneInspector(this@MainActivity, resetDataset)
                inspector.page = pageSwitcher.getActualPage()
                inspector.start()
            }
        })
    }

    private fun initializeToolbar() {
        setSupportActionBar(toolbar)
        val bar = supportActionBar!!
        bar.setDisplayShowTitleEnabled(true)
        bar.setTitle(com.franmontiel.persistentcookiejar.R.string.app_name)
    }

    @Suppress("InvalidSetHasFixedSize")
    private fun initializeRecyclerView() {
        adapter = ListAdapter(this)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (inspecting) return
                if (!Global.isInfiniteScrollMain()) return
                if (refresher.isRefreshing) return

                val manager = (recycler.layoutManager as CustomGridLayoutManager?)!!
                if (!pageSwitcher.lastPageReached() && lastGalleryReached(manager)) {
                    inspecting = true
                    inspector = inspector.cloneInspector(this@MainActivity, addDataset)
                    inspector.page = inspector.page + 1
                    inspector.start()
                }
            }
        })
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    fun hidePageSwitcher() {
        runOnUiThread { pageSwitcher.visibility = View.GONE }
    }

    fun showPageSwitcher(actualPage: Int, totalPage: Int) {
        pageSwitcher.setPages(totalPage, actualPage)
        if (Global.isInfiniteScrollMain()) {
            hidePageSwitcher()
        }
    }

    private fun lastGalleryReached(manager: CustomGridLayoutManager?): Boolean {
        return manager!!.findLastVisibleItemPosition() >= recycler.adapter!!.itemCount - 1 - manager.spanCount
    }

    private fun initializeNavigationView() {
        changeNavigationImage(navigationView)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        navigationView.setNavigationItemSelectedListener(this)
        onlineFavoriteManager.isVisible = Login.isLogged()
    }

    fun setIdOpenedGallery(idOpenedGallery: Int) {
        this.idOpenedGallery = idOpenedGallery
    }

    private fun findUsefulViews() {
        masterLayout = findViewById(R.id.master_layout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.nav_view)
        recycler = findViewById(R.id.recycler)
        refresher = findViewById(R.id.refresher)
        pageSwitcher = findViewById(R.id.page_switcher)
        drawerLayout = findViewById(R.id.drawer_layout)
        loginItem = navigationView.menu.findItem(R.id.action_login)
        onlineFavoriteManager = navigationView.menu.findItem(R.id.online_favorite_manager)
    }

    private fun loadStringLogin() {
        if (Login.user != null) loginItem.title =
            getString(R.string.login_formatted, Login.user!!.username) else loginItem.setTitle(
            if (Login.isLogged()) R.string.logout else R.string.login
        )
    }

    private fun hideError() {
        if (snackbar != null && snackbar!!.isShown) snackbar!!.dismiss()
        snackbar = null
    }

    private fun showError(text: String?, listener: View.OnClickListener?) {
        if (text == null) {
            hideError()
            return
        }
        if (listener == null) {
            snackbar = masterLayout?.let { Snackbar.make(it, text, Snackbar.LENGTH_SHORT) }
        } else {
            snackbar = masterLayout?.let { Snackbar.make(it, text, Snackbar.LENGTH_INDEFINITE) }
            snackbar!!.setAction(R.string.retry, listener)
        }
        snackbar!!.show()
    }

    private fun showError(@StringRes text: Int, listener: View.OnClickListener?) {
        showError(getString(text), listener)
    }

    private fun selectStartMode(intent: Intent, packageName: String) {
        val data = intent.data
        if (intent.getBooleanExtra("$packageName.ISBYTAG", false)) useTagMode(
            intent,
            packageName
        ) else if (intent.getBooleanExtra(
                "$packageName.SEARCHMODE", false
            )
        ) useSearchMode(intent, packageName) else if (intent.getBooleanExtra(
                "$packageName.FAVORITE", false
            )
        ) useFavoriteMode(1) else if (intent.getBooleanExtra(
                "$packageName.BYBOOKMARK",
                false
            )
        ) useBookmarkMode(
            intent,
            packageName
        ) else if (data != null) manageDataStart(data) else useNormalMode()
    }

    private fun useNormalMode() {
        inspector = basicInspector(this, 1, resetDataset)
        modeType = ModeType.NORMAL
    }

    private fun useBookmarkMode(intent: Intent, packageName: String) {
        inspector = intent.getParcelableExtra("$packageName.INSPECTOR")!!
        inspector.initialize(this, resetDataset)
        modeType = ModeType.BOOKMARK
        val type = inspector.requestType
        if (type === ApiRequestType.BYTAG) modeType =
            ModeType.TAG else if (type === ApiRequestType.BYALL) modeType =
            ModeType.NORMAL else if (type === ApiRequestType.BYSEARCH) modeType =
            ModeType.SEARCH else if (type === ApiRequestType.FAVORITE) modeType = ModeType.FAVORITE
    }

    private fun useFavoriteMode(page: Int) {
        inspector = favoriteInspector(this, null, page, resetDataset)
        modeType = ModeType.FAVORITE
    }

    private fun useSearchMode(intent: Intent, packageName: String) {
        val query = intent.getStringExtra("$packageName.QUERY")
        val ok = tryOpenId(query)
        if (!ok) createSearchInspector(intent, packageName, query)
    }

    private fun createSearchInspector(intent: Intent, packageName: String, query: String?) {
        var query = query
        val advanced = intent.getBooleanExtra("$packageName.ADVANCED", false)
        val tagArrayList = intent.getParcelableArrayListExtra<Tag>(
            "$packageName.TAGS"
        )
        val ranges = intent.getParcelableExtra<Ranges>(getPackageName() + ".RANGES")
        var tags: HashSet<Tag>? = null
        query = query!!.trim { it <= ' ' }
        if (advanced) {
            assert(
                tagArrayList != null
            )
            tags = tagArrayList?.let { HashSet(it) }
        }
        inspector =
            searchInspector(this, query, tags, 1, Global.sortType, ranges, resetDataset)
        modeType = ModeType.SEARCH
    }

    private fun tryOpenId(query: String?): Boolean {
        try {
            val id = query!!.toInt()
            inspector = galleryInspector(this, id, startGallery)
            modeType = ModeType.ID
            return true
        } catch (ignore: NumberFormatException) {
        }
        return false
    }

    private fun useTagMode(intent: Intent, packageName: String) {
        val t = intent.getParcelableExtra<Tag>(
            "$packageName.TAG"
        )
        inspector = tagInspector(this, t!!, 1, Global.sortType, resetDataset)
        modeType = ModeType.TAG
    }

    private fun manageDataStart(data: Uri) {
        val datas = data.pathSegments
        LogUtility.download("Datas: $datas")
        if (datas.size == 0) {
            useNormalMode()
            return
        }
        val dataType: TagType = TagType.typeByName(datas[0])
        if (dataType !== TagType.UNKNOWN) useDataTagMode(datas, dataType) else useDataSearchMode(
            data,
            datas
        )
    }

    private fun useDataSearchMode(data: Uri, datas: List<String>) {
        val query = data.getQueryParameter("q")
        val pageParam = data.getQueryParameter("page")
        val favorite = "favorites" == datas[0]
        val type = SortType.findFromAddition(data.getQueryParameter("sort"))
        var page = 1
        if (pageParam != null) page = pageParam.toInt()
        if (favorite) {
            if (Login.isLogged()) useFavoriteMode(page) else {
                val intent = Intent(this, FavoriteActivity::class.java)
                startActivity(intent)
                finish()
            }
            return
        }
        inspector = searchInspector(this, query, null, page, type, null, resetDataset)
        modeType = ModeType.SEARCH
    }

    private fun useDataTagMode(datas: List<String>, type: TagType) {
        val query = datas[1]
        var tag = Queries.TagTable.getTagFromTagName(query)
        if (tag == null) tag =
            Tag(query, -1, SpecialTagIds.INVALID_ID.toInt(), type, TagStatus.DEFAULT)
        var sortType: SortType? = SortType.RECENT_ALL_TIME
        if (datas.size == 3) {
            sortType = SortType.findFromAddition(datas[2])
        }
        inspector = tagInspector(this, tag, 1, sortType, resetDataset)
        modeType = ModeType.TAG
    }

    private fun changeNavigationImage(navigationView: NavigationView?) {
        val light = Global.theme == ThemeScheme.LIGHT
        val view = navigationView!!.getHeaderView(0)
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        ImageDownloadUtility.loadImage(
            if (light) R.drawable.ic_logo_dark else R.drawable.ic_logo,
            imageView
        )
    }

    private fun changeUsedLanguage(item: MenuItem) {
        when (Global.getOnlyLanguage()) {
            Language.ENGLISH -> Global.updateOnlyLanguage(this, Language.JAPANESE)
            Language.JAPANESE -> Global.updateOnlyLanguage(this, Language.CHINESE)
            Language.CHINESE -> Global.updateOnlyLanguage(this, Language.ALL)
            Language.ALL -> Global.updateOnlyLanguage(this, Language.ENGLISH)
            else -> Global.updateOnlyLanguage(this, Language.ALL)
        }
        //wait 250ms to reduce the requests
        changeLanguageTimeHandler.removeCallbacks(changeLanguageRunnable)
        changeLanguageTimeHandler.postDelayed(
            changeLanguageRunnable,
            CHANGE_LANGUAGE_DELAY.toLong()
        )
    }

    private fun showLogoutForm() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setIcon(R.drawable.ic_exit_to_app).setTitle(R.string.logout)
            .setMessage(R.string.are_you_sure)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            Login.logout(this)
            onlineFavoriteManager.isVisible = false
            loginItem.setTitle(R.string.login)
        }.setNegativeButton(R.string.no, null).show()
    }

    override fun onResume() {
        super.onResume()
        Login.initLogin(this)
        if (idOpenedGallery != -1) {
            adapter.updateColor(idOpenedGallery)
            idOpenedGallery = -1
        }
        loadStringLogin()
        onlineFavoriteManager.isVisible = Login.isLogged()
        if (!noNeedForCaptcha) {
            if (Login.hasCookie("csrftoken")) {
                inspector = inspector.cloneInspector(this, resetDataset)
                inspector.start() //restart inspector
                noNeedForCaptcha = true
            }
        }
        if (setting != null) {
            Global.initFromShared(this) //restart all settings
            inspector = inspector.cloneInspector(this, resetDataset)
            inspector.start() //restart inspector
            if (setting!!.theme != Global.theme || setting!!.locale != Global.initLanguage(this)) {
                val manager = GlideX.with(applicationContext)
                manager?.pauseAllRequestsRecursive()
                recreate()
            }
            adapter.notifyDataSetChanged() //restart adapter
            adapter.resetStatuses()
            showPageSwitcher(inspector.page, inspector.pageCount)
            changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            setting = null
        } else if (filteringTag) {
            inspector = basicInspector(this, 1, resetDataset)
            inspector.start()
            filteringTag = false
        }
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        popularItemDispay(menu.findItem(R.id.by_popular))
        showLanguageIcon(menu.findItem(R.id.only_language))
        menu.findItem(R.id.only_language).isVisible = modeType == ModeType.NORMAL
        menu.findItem(R.id.random_favorite).isVisible = modeType == ModeType.FAVORITE
        initializeSearchItem(menu.findItem(R.id.search))
        if (modeType == ModeType.TAG) {
            val item = menu.findItem(R.id.tag_manager)
            item.isVisible = inspector.tag.id > 0
            val ts = inspector.tag.status
            updateTagStatus(item, ts)
        }
        Utility.tintMenu(menu)
        return true
    }

    private fun initializeSearchItem(item: MenuItem) {
        if (modeType != ModeType.FAVORITE) item.actionView =
            null else {
            (item.actionView as SearchView?)!!.setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    inspector = favoriteInspector(this@MainActivity, query, 1, resetDataset)
                    inspector.start()
                    supportActionBar!!.title = query
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }
    }

    private fun popularItemDispay(item: MenuItem) {
        item.title =
            getString(R.string.sort_type_title_format, getString(Global.sortType.nameId))
        Global.setTint(item.icon)
    }

    private fun showLanguageIcon(item: MenuItem) {
        when (Global.getOnlyLanguage()) {
            Language.JAPANESE -> {
                item.setTitle(R.string.only_japanese)
                item.setIcon(R.drawable.ic_jpbw)
            }
            Language.CHINESE -> {
                item.setTitle(R.string.only_chinese)
                item.setIcon(R.drawable.ic_cnbw)
            }
            Language.ENGLISH -> {
                item.setTitle(R.string.only_english)
                item.setIcon(R.drawable.ic_gbbw)
            }
            Language.ALL -> {
                item.setTitle(R.string.all_languages)
                item.setIcon(R.drawable.ic_world)
            }
            else -> {
                item.setTitle(R.string.all_languages)
                item.setIcon(R.drawable.ic_world)
            }
        }
        Global.setTint(item.icon)
    }

    override val portraitColumnCount: Int
        get() {
            return Global.colPortMain
        }

    override val landscapeColumnCount: Int
        get() {
            return Global.colLandMain
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        LogUtility.download("Pressed item: " + item.itemId)
        if (item.itemId == R.id.by_popular) {
            updateSortType(item)
        } else if (item.itemId == R.id.only_language) {
            changeUsedLanguage(item)
            showLanguageIcon(item)
        } else if (item.itemId == R.id.search) {
            if (modeType != ModeType.FAVORITE) {
                i = Intent(this, SearchActivity::class.java)
                startActivity(i)
            }
        } else if (item.itemId == R.id.open_browser) {
            i = Intent(Intent.ACTION_VIEW, Uri.parse(inspector.url))
            startActivity(i)
        } else if (item.itemId == R.id.random_favorite) {
            inspector = randomInspector(this, startGallery, true)
            inspector.start()
        } else if (item.itemId == R.id.download_page) {
            showDialogDownloadAll()
        } else if (item.itemId == R.id.add_bookmark) {
            Queries.BookmarkTable.addBookmark(inspector)
        } else if (item.itemId == R.id.tag_manager) {
            val ts = Tags.updateStatus(inspector.tag)
            updateTagStatus(item, ts)
        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSortType(item: MenuItem) {
        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        val builder = MaterialAlertDialogBuilder(this)
        for (type in SortType.values()) adapter.add(getString(type.nameId))
        temporaryType = Global.sortType
        builder.setIcon(R.drawable.ic_sort).setTitle(R.string.sort_select_type)
        builder.setSingleChoiceItems(
            adapter, temporaryType.ordinal
        ) { _: DialogInterface?, which: Int ->
            temporaryType = SortType.values()[which]
        }
        builder.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                temporaryType = SortType.values()[position]
                parent.setSelection(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
            Global.updateSortType(this@MainActivity, temporaryType)
            popularItemDispay(item)
            inspector = inspector.cloneInspector(this@MainActivity, resetDataset)
            inspector.setSortType(temporaryType)
            inspector.start()
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun showDialogDownloadAll() {
        val builder = MaterialAlertDialogBuilder(this)
        builder
            .setTitle(R.string.download_all_galleries_in_this_page)
            .setIcon(R.drawable.ic_file_download)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.ok
            ) { _: DialogInterface?, _: Int ->
                for (g in inspector.galleries) DownloadGallery.downloadGallery(
                    this@MainActivity,
                    g
                )
            }
        builder.show()
    }

    private fun updateTagStatus(item: MenuItem, ts: TagStatus) {
        when (ts) {
            TagStatus.DEFAULT -> item.setIcon(R.drawable.ic_help)
            TagStatus.AVOIDED -> item.setIcon(R.drawable.ic_close)
            TagStatus.ACCEPTED -> item.setIcon(R.drawable.ic_check)
        }
        Global.setTint(item.icon)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        if (item.itemId == R.id.downloaded) {
            if (Global.hasStoragePermission(this)) startLocalActivity() else requestStorage()
        } else if (item.itemId == R.id.bookmarks) {
            intent = Intent(this, BookmarkActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.history) {
            intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.favorite_manager) {
            intent = Intent(this, FavoriteActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.action_settings) {
            setting = Setting()
            intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.online_favorite_manager) {
            intent = Intent(this, MainActivity::class.java)
            intent.putExtra("$packageName.FAVORITE", true)
            startActivity(intent)
        } else if (item.itemId == R.id.action_login) {
            if (Login.isLogged()) showLogoutForm() else {
                intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        } else if (item.itemId == R.id.random) {
            intent = Intent(this, RandomActivity::class.java)
            startActivity(intent)
        } else if (item.itemId == R.id.tag_manager) {
            intent = Intent(this, TagFilterActivity::class.java)
            filteringTag = true
            startActivity(intent)
        } else if (item.itemId == R.id.status_manager) {
            intent = Intent(this, StatusViewerActivity::class.java)
            startActivity(intent)
        }
        //drawerLayout.closeDrawer(GravityCompat.START);
        return true
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
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startLocalActivity()
    }

    private fun startLocalActivity() {
        val i = Intent(this, LocalActivity::class.java)
        startActivity(i)
    }

    /**
     * UNKNOWN in case of error
     * NORMAL when in main page
     * TAG when searching for a specific tag
     * FAVORITE when using online favorite button
     * SEARCH when used SearchActivity
     * BOOKMARK when loaded a bookmark
     * ID when searched for an ID
     */
    private enum class ModeType {
        UNKNOWN, NORMAL, TAG, FAVORITE, SEARCH, BOOKMARK, ID
    }

    internal abstract inner class MainInspectorResponse :
        DefaultInspectorResponse() {
        override fun onSuccess(galleries: List<GenericGallery>) {
            super.onSuccess(galleries)
            adapter.resetStatuses()
            noNeedForCaptcha = true
            if (galleries.isEmpty()) showError(R.string.no_entry_found, null)
        }

        override fun onStart() {
            runOnUiThread { refresher.isRefreshing = true }
            hideError()
        }

        override fun onEnd() {
            runOnUiThread { refresher.isRefreshing = false }
            inspecting = false
        }

        override fun onFailure(e: Exception) {
            super.onFailure(e)
            if (e is InvalidResponseException) {
                failCount += 1
                if (failCount == MAX_FAIL_BEFORE_CLEAR_COOKIE || !noNeedForCaptcha && !showedCaptcha) {
                    Login.removeCloudflareCookies()
                    failCount = 0
                    showedCaptcha = true
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.putExtra("$packageName.IS_CAPTCHA", true)
                    startActivity(intent)
                }
                showError(R.string.invalid_response) {
                    inspector =
                        inspector.cloneInspector(this@MainActivity, inspector.response)
                    inspector.start()
                }
            } else {
                showError(R.string.unable_to_connect_to_the_site) {
                    inspector =
                        inspector.cloneInspector(this@MainActivity, inspector.response)
                    inspector.start()
                }
            }
        }
    }

    private inner class Setting {
        val theme: ThemeScheme = Global.theme
        val locale: Locale = Global.initLanguage(this@MainActivity)

    }

    companion object {
        private const val MAX_FAIL_BEFORE_CLEAR_COOKIE = 3
        private const val CHANGE_LANGUAGE_DELAY = 1000
    }
}
