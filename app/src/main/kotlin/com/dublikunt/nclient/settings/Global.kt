package com.dublikunt.nclient.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.CopyToClipboardActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.gallerys.GenericGallery
import com.dublikunt.nclient.api.gallerys.LocalSortType
import com.dublikunt.nclient.components.CustomCookieJar
import com.dublikunt.nclient.enums.Language
import com.dublikunt.nclient.enums.SortType
import com.dublikunt.nclient.enums.TitleType
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.NetworkUtil.ConnectionType
import com.dublikunt.nclient.utility.NetworkUtil.type
import com.dublikunt.nclient.utility.Utility
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Locale
import kotlin.math.max

object Global {
    const val CHANNEL_ID1 = "download_gallery"
    const val CHANNEL_ID2 = "create_pdf"
    const val CHANNEL_ID3 = "create_zip"
    private const val MAINFOLDER_NAME = "NClient"
    private const val DOWNLOADFOLDER_NAME = "Download"
    private const val SCREENFOLDER_NAME = "Screen"
    private const val PDFFOLDER_NAME = "PDF"
    private const val UPDATEFOLDER_NAME = "Update"
    private const val ZIPFOLDER_NAME = "ZIP"
    private const val BACKUPFOLDER_NAME = "Backup"
    private const val TORRENTFOLDER_NAME = "Torrents"
    private val lastDisplay = DisplayMetrics()

    @JvmStatic
    lateinit var client: OkHttpClient
    lateinit var oldGalleryFolder: File
    lateinit var mainFolder: File
    lateinit var downloadFolder: File
    lateinit var screenFolder: File
    lateinit var pdfFolder: File
    lateinit var updateFolder: File
    lateinit var zipFolder: File
    lateinit var torrentFolder: File
    lateinit var backupFolder: File
    lateinit var onlyLanguage: Language
    lateinit var titleType: TitleType
    lateinit var sortType: SortType
    lateinit var localSortType: LocalSortType
    private var invertFix = false
    var isButtonChangePage = false
    private var hideMultitask = false
    private var isEnableBeta = false
    private var volumeOverride = false
    var isZoomOneColumn = false
    var isKeepHistory = false
    var isLockScreen = false
    var isOnlyTag = false
    var showTitles = false
    private var removeAvoidedGalleries = false
    var useRtl = false
    var theme: ThemeScheme = ThemeScheme.DARK
    private lateinit var usageMobile: DataUsageType
    private lateinit var usageWifi: DataUsageType
    private lateinit var lastVersion: String
    var mirror: String? = null
    var maxHistory = 0
    var columnCount = 0
    var maxId = 0
    var galleryWidth = -1
    var galleryHeight = -1
    var colPortStatus = 0
    var colLandStatus = 0
    var colPortHistory = 0
    var colLandHistory = 0
    var colPortMain = 0
    var colLandMain = 0
    var colPortDownload = 0
    var colLandDownload = 0
    var colLandFavorite = 0
    var colPortFavorite = 0
    private var defaultZoom = 0
    var offscreenLimit = 0
    private var screenSize: Point = Point()
    var infiniteScrollMain: Boolean = false
    private var infiniteScrollFavorite: Boolean = false
    fun recursiveSize(path: File): Long {
        if (path.isFile) return path.length()
        var size: Long = 0
        val files = path.listFiles() ?: return size
        for (f in files) size += if (f.isFile) f.length() else recursiveSize(f)
        return size
    }

    fun getFavoriteLimit(context: Context): Int {
        return context.getSharedPreferences("Settings", 0)
            .getInt(context.getString(R.string.key_favorite_limit), 10)
    }

    fun getLastVersion(context: Context): String {
        lastVersion =
            context.getSharedPreferences("Settings", 0).getString("last_version", "0.0.0")
                .toString()
        return lastVersion
    }

    fun setLastVersion(context: Context) {
        lastVersion = getVersionName(context)
        context.getSharedPreferences("Settings", 0).edit().putString("last_version", lastVersion)
            .apply()
    }

    fun isDestroyed(activity: AppCompatActivity): Boolean {
        return activity.isDestroyed
    }


    val userAgent: String
        get() = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Mobile Safari/537.36"

    fun getDefaultFileParent(context: Context): String {
        val f: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getExternalFilesDir(null)!!
        } else {
            Environment.getExternalStorageDirectory()
        }
        return f.absolutePath
    }

    private fun initFilesTree(context: Context) {
        val files = getUsableFolders(context)
        val path = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            .getString(context.getString(R.string.key_save_path), getDefaultFileParent(context))
        var rootFolder = File(path!!)

        if (!files.contains(rootFolder) && !isExternalStorageManager) rootFolder = File(
            getDefaultFileParent(context)
        )
        mainFolder = File(rootFolder, MAINFOLDER_NAME)
        download(mainFolder)
        oldGalleryFolder =
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), MAINFOLDER_NAME)
        downloadFolder = File(mainFolder, DOWNLOADFOLDER_NAME)
        screenFolder = File(mainFolder, SCREENFOLDER_NAME)
        pdfFolder = File(mainFolder, PDFFOLDER_NAME)
        updateFolder = File(mainFolder, UPDATEFOLDER_NAME)
        zipFolder = File(mainFolder, ZIPFOLDER_NAME)
        torrentFolder = File(mainFolder, TORRENTFOLDER_NAME)
        backupFolder = File(mainFolder, BACKUPFOLDER_NAME)
    }

    fun initScreenSize(activity: AppCompatActivity) {
        activity.windowManager.defaultDisplay.getSize(screenSize)
    }

    private fun initGallerySize() {
        galleryHeight = screenSize.y / 2
        galleryWidth = galleryHeight * 3 / 4
    }

    val screenHeight: Int
        get() = screenSize.y
    val screenWidth: Int
        get() = screenSize.x

    private fun initTitleType(context: Context) {
        val s = context.getSharedPreferences("Settings", 0)
            .getString(context.getString(R.string.key_title_type), "pretty")
        when (s) {
            "pretty" -> titleType = TitleType.PRETTY
            "english" -> titleType = TitleType.ENGLISH
            "japanese" -> titleType = TitleType.JAPANESE
        }
    }

    fun getDeviceWidth(activity: AppCompatActivity): Int {
        getDeviceMetrics(activity)
        return lastDisplay.widthPixels
    }

    fun getDeviceHeight(activity: AppCompatActivity): Int {
        getDeviceMetrics(activity)
        return lastDisplay.heightPixels
    }

    private fun getDeviceMetrics(activity: AppCompatActivity) {
        activity.windowManager.defaultDisplay.getMetrics(lastDisplay)
    }

    fun initFromShared(context: Context) {
        Login.initLogin(context)
        val shared = context.getSharedPreferences("Settings", 0)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        initHttpClient(context)
        initTitleType(context)
        initTheme(context)
        loadNotificationChannel(context)
        NotificationSettings.initializeNotificationManager(context)
        initStorage(context)
        shared.edit().remove("local_sort").apply()
        localSortType = LocalSortType(shared.getInt(context.getString(R.string.key_local_sort), 0))
        useRtl = shared.getBoolean(context.getString(R.string.key_use_rtl), false)
        mirror = shared.getString(context.getString(R.string.key_site_mirror), Utility.ORIGINAL_URL)
        isKeepHistory = shared.getBoolean(context.getString(R.string.key_keep_history), true)
        removeAvoidedGalleries =
            shared.getBoolean(context.getString(R.string.key_remove_ignored), true)
        invertFix = shared.getBoolean(context.getString(R.string.key_inverted_fix), true)
        isOnlyTag = shared.getBoolean(context.getString(R.string.key_ignore_tags), true)
        volumeOverride = shared.getBoolean(context.getString(R.string.key_override_volume), true)
        isEnableBeta = shared.getBoolean(context.getString(R.string.key_enable_beta), true)
        columnCount = shared.getInt(context.getString(R.string.key_column_count), 2)
        showTitles = shared.getBoolean(context.getString(R.string.key_show_titles), true)
        isButtonChangePage =
            shared.getBoolean(context.getString(R.string.key_change_page_buttons), true)
        isLockScreen = shared.getBoolean(context.getString(R.string.key_disable_lock), false)
        hideMultitask = shared.getBoolean(context.getString(R.string.key_hide_multitasking), true)
        infiniteScrollFavorite =
            shared.getBoolean(context.getString(R.string.key_infinite_scroll_favo), false)
        infiniteScrollMain =
            shared.getBoolean(context.getString(R.string.key_infinite_scroll_main), false)
        maxId = shared.getInt(context.getString(R.string.key_max_id), 300000)
        offscreenLimit =
            max(1, shared.getInt(context.getString(R.string.key_offscreen_limit), 5))
        maxHistory = shared.getInt(context.getString(R.string.key_max_history_size), 2)
        defaultZoom = shared.getInt(context.getString(R.string.key_default_zoom), 100)
        colPortMain = shared.getInt(context.getString(R.string.key_column_port_main), 2)
        colLandMain = shared.getInt(context.getString(R.string.key_column_land_main), 4)
        colPortDownload = shared.getInt(context.getString(R.string.key_column_port_down), 2)
        colLandDownload = shared.getInt(context.getString(R.string.key_column_land_down), 4)
        colPortFavorite = shared.getInt(context.getString(R.string.key_column_port_favo), 2)
        colLandFavorite = shared.getInt(context.getString(R.string.key_column_land_favo), 4)
        colPortHistory = shared.getInt(context.getString(R.string.key_column_port_hist), 2)
        colLandHistory = shared.getInt(context.getString(R.string.key_column_land_hist), 4)
        colPortStatus = shared.getInt(context.getString(R.string.key_column_port_stat), 2)
        colLandStatus = shared.getInt(context.getString(R.string.key_column_land_stat), 4)
        isZoomOneColumn = shared.getBoolean(context.getString(R.string.key_zoom_one_column), false)
        var x = max(
            0,
            shared.getInt(context.getString(R.string.key_only_language), Language.ALL.ordinal)
        )
        sortType = SortType.values()[shared.getInt(
            context.getString(R.string.key_by_popular),
            SortType.RECENT_ALL_TIME.ordinal
        )]
        usageMobile = DataUsageType.values()[shared.getInt(
            context.getString(R.string.key_mobile_usage),
            DataUsageType.FULL.ordinal
        )]
        usageWifi = DataUsageType.values()[shared.getInt(
            context.getString(R.string.key_wifi_usage),
            DataUsageType.FULL.ordinal
        )]
        if (Language.values()[x] == Language.UNKNOWN) {
            updateOnlyLanguage(context, Language.ALL)
            x = Language.ALL.ordinal
        }
        onlyLanguage = Language.values()[x]
    }

    fun hideMultitask(): Boolean {
        return hideMultitask
    }

    fun setLocalSortType(context: Context, localSortType: LocalSortType) {
        context.getSharedPreferences("Settings", 0).edit()
            .putInt(context.getString(R.string.key_local_sort), localSortType.hashCode()).apply()
        Global.localSortType = localSortType
        download("Assegning: $localSortType")
    }

    val downloadPolicy: DataUsageType
        get() {
            return when (type) {
                ConnectionType.WIFI -> usageWifi
                ConnectionType.CELLULAR -> usageMobile
                else -> usageWifi
            }
        }

    fun volumeOverride(): Boolean {
        return volumeOverride
    }

    private fun reloadHttpClient(context: Context) {
        val preferences = context.getSharedPreferences("Login", 0)
        Login.setLoginShared(preferences)
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .cookieJar(
                CustomCookieJar(
                    SetCookieCache(),
                    SharedPrefsCookiePersistor(preferences)
                )
            )
        builder.addInterceptor(CustomInterceptor(true))
        client = builder.build()
        client.dispatcher.maxRequests = 25
        client.dispatcher.maxRequestsPerHost = 25
        for (cookie in client.cookieJar.loadForRequest(Login.BASE_HTTP_URL)) {
            download("Cookie: $cookie")
        }
        Login.isLogged(context)
    }

    private fun initHttpClient(context: Context) {
        reloadHttpClient(context)
    }

    fun initLanguage(context: Context): Locale {
        val resources = context.resources
        val l = getLanguage(context)
        val c = Configuration(resources.configuration)
        c.setLocale(l)
        resources.updateConfiguration(c, resources.displayMetrics)
        return l
    }

    fun getLanguage(context: Context): Locale {
        val sharedPreferences = context.getSharedPreferences("Settings", 0)
        val prefLangKey = context.getString(R.string.key_language)
        val defaultValue = context.getString(R.string.key_default_value)
        val langCode = sharedPreferences.getString(prefLangKey, defaultValue)!!
        if (langCode.equals(defaultValue, ignoreCase = true)) {
            return Locale.getDefault()
        }
        return if (langCode.contains("-") || langCode.contains("_")) {
            val regexSplit =
                langCode.split("[-_]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Locale(regexSplit[0], regexSplit[1])
        } else {
            val targetLocale = Locale(langCode)
            println(targetLocale.country)
            targetLocale
        }
    }

    private fun getLocaleCode(locale: Locale): String {
        return String.format("%s-%s", locale.language, locale.country)
    }

    private fun initTheme(context: Context): ThemeScheme {
        val h = context.getSharedPreferences("Settings", 0)
            .getString(context.getString(R.string.key_theme_select), "dark")
        return if (h == "light") ThemeScheme.LIGHT else ThemeScheme.DARK
    }

    fun shouldCheckForUpdates(context: Context): Boolean {
        return context.getSharedPreferences("Settings", 0)
            .getBoolean(context.getString(R.string.key_check_update), true)
    }

    fun getLogo(resources: Resources): Drawable {
        return ResourcesCompat.getDrawable(
            resources,
            if (theme == ThemeScheme.LIGHT) R.drawable.ic_logo_dark else R.drawable.ic_logo,
            null
        )!!
    }

    fun getDefaultZoom(): Float {
        return defaultZoom.toFloat() / 100f
    }

    fun removeAvoidedGalleries(): Boolean {
        return removeAvoidedGalleries
    }

    fun initStorage(context: Context) {
        if (!hasStoragePermission(context)) return
        initFilesTree(context)
        val bools = booleanArrayOf(
            mainFolder.mkdirs(),
            downloadFolder.mkdir(),
            pdfFolder.mkdir(),
            updateFolder.mkdir(),
            screenFolder.mkdir(),
            zipFolder.mkdir(),
            torrentFolder.mkdir(),
            backupFolder.mkdir()
        )
        download(
            """
                0:${context.filesDir}
                1:$mainFolder${bools[0]}
                2:$downloadFolder${bools[1]}
                3:$pdfFolder${bools[2]}
                4:$updateFolder${bools[3]}
                5:$screenFolder${bools[4]}
                5:$zipFolder${bools[5]}
                5:$torrentFolder${bools[5]}
                6:$backupFolder${bools[6]}

                """.trimIndent()
        )
        try {
            File(mainFolder, ".nomedia").createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun updateOnlyLanguage(context: Context, type: Language?) {
        context.getSharedPreferences("Settings", 0).edit()
            .putInt(context.getString(R.string.key_only_language), type!!.ordinal).apply()
        onlyLanguage = type
    }

    fun updateSortType(context: Context, sortType: SortType) {
        context.getSharedPreferences("Settings", 0).edit()
            .putInt(context.getString(R.string.key_by_popular), sortType.ordinal).apply()
        Global.sortType = sortType
    }

    fun updateColumnCount(context: Context, count: Int) {
        context.getSharedPreferences("Settings", 0).edit()
            .putInt(context.getString(R.string.key_column_count), count).apply()
        columnCount = count
    }

    fun updateMaxId(context: Context, id: Int) {
        context.getSharedPreferences("Settings", 0).edit()
            .putInt(context.getString(R.string.key_max_id), id).apply()
        maxId = id
    }

    fun getStatusBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    fun getNavigationBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun shareURL(context: Context, title: String, url: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "$title: $url")
        sendIntent.type = "text/plain"
        val clipboardIntent = Intent(context, CopyToClipboardActivity::class.java)
        clipboardIntent.data = Uri.parse(url)
        val chooserIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_with))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(clipboardIntent))
        context.startActivity(chooserIntent)
    }

    fun shareGallery(context: Context, gallery: GenericGallery) {
        shareURL(context, gallery.title, Utility.baseUrl + "g/" + gallery.id)
    }

    fun setTint(drawable: Drawable?) {
        if (drawable == null) return
        DrawableCompat.setTint(
            drawable,
            if (theme == ThemeScheme.LIGHT) Color.BLACK else Color.WHITE
        )
    }

    private fun loadNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_ID1,
                context.getString(R.string.channel1_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val channel2 = NotificationChannel(
                CHANNEL_ID2,
                context.getString(R.string.channel2_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val channel3 = NotificationChannel(
                CHANNEL_ID3,
                context.getString(R.string.channel3_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel1.description = context.getString(R.string.channel1_description)
            channel2.description = context.getString(R.string.channel2_description)
            channel3.description = context.getString(R.string.channel3_description)
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel1)
                notificationManager.createNotificationChannel(channel2)
                notificationManager.createNotificationChannel(channel3)
            }
        }
    }

    fun getUsableFolders(context: Context): List<File> {
        val strings: MutableList<File> = ArrayList(3)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) strings.add(Environment.getExternalStorageDirectory())
        val files = context.getExternalFilesDirs(null)
        strings.addAll(listOf(*files))
        return strings
    }


    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun isJPEGCorrupted(path: String): Boolean {
        if (!File(path).exists()) return true
        try {
            RandomAccessFile(path, "r").use { fh ->
                val length = fh.length()
                if (length < 10L) {
                    return true
                }
                fh.seek(length - 2)
                val eoi = ByteArray(2)
                fh.read(eoi)
                return eoi[0] != 0xFF.toByte() || eoi[1] != 0xD9.toByte()
            }
        } catch (e: IOException) {
            error(e.message, e)
        }
        return true
    }

    private fun findGalleryFolder(directory: File?, id: Int): File? {
        if (directory == null || !directory.exists() || !directory.isDirectory) return null
        val fileName = ".$id"
        val tmp = directory.listFiles() ?: return null
        for (tmp2 in tmp) {
            if (tmp2.isDirectory && File(tmp2, fileName).exists()) {
                return tmp2
            }
        }
        return null
    }

    private fun findGalleryFolder(id: Int): File? {
        return findGalleryFolder(downloadFolder, id)
    }

    fun findGalleryFolder(context: Context?, id: Int): File? {
        if (id < 1) return null
        if (context == null) return findGalleryFolder(id)
        for (dir in getUsableFolders(context)) {
            var dir2 = File(dir, MAINFOLDER_NAME)
            dir2 = File(dir2, DOWNLOADFOLDER_NAME)
            val f = findGalleryFolder(dir2, id)
            if (f != null) return f
        }
        return null
    }

    private fun updateConfigurationNightMode(activity: AppCompatActivity, c: Configuration) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        c.uiMode = c.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()
        c.uiMode = c.uiMode or Configuration.UI_MODE_NIGHT_NO
    }

    private fun invertFix(context: AppCompatActivity) {
        if (!invertFix) return
        val resources = context.resources
        val c = Configuration(resources.configuration)
        updateConfigurationNightMode(context, c)
        resources.updateConfiguration(c, resources.displayMetrics)
    }

    fun initActivity(context: AppCompatActivity) {
        initScreenSize(context)
        initGallerySize()
        initLanguage(context)
        invertFix(context)
        when (initTheme(context)) {
            ThemeScheme.LIGHT -> context.setTheme(R.style.LightTheme)
            ThemeScheme.DARK -> context.setTheme(R.style.DarkTheme)
        }
    }

    fun recursiveDelete(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            for (x in files) recursiveDelete(x)
        }
        file.delete()
    }

    fun getVersionName(context: Context): String {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "0.0.0"
    }

    val isExternalStorageManager: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()


    fun applyFastScroller(recycler: RecyclerView?) {
        if (recycler == null) return
        val drawable = ContextCompat.getDrawable(recycler.context, R.drawable.thumb) ?: return
        FastScrollerBuilder(recycler).setThumbDrawable(drawable).build()
    }

    fun getLanguageFlag(language: Language?): String {
        return when (language) {
            Language.CHINESE -> "\uD83C\uDDE8\uD83C\uDDF3"
            Language.ENGLISH -> "\uD83C\uDDEC\uD83C\uDDE7"
            Language.JAPANESE -> "\uD83C\uDDEF\uD83C\uDDF5"
            Language.UNKNOWN -> "\uD83C\uDFF3"
            else -> "\uD83C\uDFF3"
        }
    }

    enum class ThemeScheme {
        LIGHT, DARK
    }

    enum class DataUsageType {
        NONE, THUMBNAIL, FULL
    }
}
