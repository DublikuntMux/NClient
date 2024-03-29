package com.dublikunt.nclient.settings;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.webkit.CookieSyncManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dublikunt.nclient.CopyToClipboardActivity;
import com.dublikunt.nclient.R;
import com.dublikunt.nclient.api.components.GenericGallery;
import com.dublikunt.nclient.api.enums.Language;
import com.dublikunt.nclient.api.enums.SortType;
import com.dublikunt.nclient.api.enums.TitleType;
import com.dublikunt.nclient.api.local.LocalSortType;
import com.dublikunt.nclient.components.CustomCookieJar;
import com.dublikunt.nclient.utility.LogUtility;
import com.dublikunt.nclient.utility.Utility;
import com.dublikunt.nclient.utility.network.NetworkUtil;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;

public class Global {
    public static final String channelID1 = "download_gallery", channelID2 = "create_pdf", channelID3 = "create_zip";
    private static final String mainFolderName = "NClientV2";
    private static final String downloadFolderName = "Download";
    private static final String screenFolderName = "Screen";
    private static final String pdfFolderName = "PDF";
    private static final String updateFolderName = "Update";
    private static final String zipFolderName = "ZIP";
    private static final String backupFolderName = "Backup";
    private static final String torrentFolderName = "Torrents";
    private static final DisplayMetrics lastDisplay = new DisplayMetrics();
    public static OkHttpClient client = null;
    public static File mainFolder;
    public static File downloadFolder;
    public static File screenFolder;
    public static File pdfFolder;
    public static File updateFolder;
    public static File zipFolder;
    public static File backupFolder;
    public static File torrentFolder;
    private static Language onlyLanguage;
    private static TitleType titleType;
    private static SortType sortType;
    private static LocalSortType localSortType;
    private static boolean invertFix, buttonChangePage, hideMultitask, volumeOverride, zoomOneColumn, keepHistory, lockScreen, onlyTag, showTitles, removeAvoidedGalleries, useRtl;
    private static ThemeScheme theme;
    private static DataUsageType usageMobile, usageWifi;
    private static String lastVersion, mirror;
    private static int maxHistory, columnCount, maxId, galleryWidth = -1, galleryHeight = -1;
    private static int colPortStat, colLandStat, colPortHist, colLandHist, colPortMain, colLandMain, colPortDownload, colLandDownload, colLandFavorite, colPortFavorite;
    private static boolean infiniteScrollMain, infiniteScrollFavorite, exactTagMatch;
    private static int defaultZoom, offscreenLimit;
    private static Point screenSize;
    private static final String userAgent = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N)";

    public static long recursiveSize(@NonNull File path) {
        if (path.isFile()) return path.length();
        long size = 0;
        File[] files = path.listFiles();
        if (files == null) return size;
        for (File f : files)
            size += f.isFile() ? f.length() : recursiveSize(f);

        return size;
    }

    public static boolean isExactTagMatch() {
        return exactTagMatch;
    }

    public static int getFavoriteLimit(@NonNull Context context) {
        return context.getSharedPreferences("Settings", 0).getInt(context.getString(R.string.key_favorite_limit), 10);
    }

    public static String getLastVersion(Context context) {
        if (context != null)
            lastVersion = context.getSharedPreferences("Settings", 0).getString("last_version", "0.0.0");
        return lastVersion;
    }

    public static int getColLandHistory() {
        return colLandHist;
    }

    public static int getColPortHistory() {
        return colPortHist;
    }

    public static int getColLandStatus() {
        return colLandStat;
    }

    public static int getColPortStatus() {
        return colPortStat;
    }

    public static boolean isDestroyed(@NonNull Activity activity) {
        return activity.isDestroyed();
    }

    @NonNull
    public static String getUserAgent() {
        String agent = userAgent;
        return agent.replace("\n", " ").trim();
    }

    @NonNull
    public static String getDefaultFileParent(Context context) {
        File f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            f = context.getExternalFilesDir(null);
        } else {
            f = Environment.getExternalStorageDirectory();
        }
        return f.getAbsolutePath();
    }

    private static void initFilesTree(Context context) {
        List<File> files = getUsableFolders(context);
        String path = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString(context.getString(R.string.key_save_path), getDefaultFileParent(context));
        File ROOTFOLDER = new File(path);

        if (!files.contains(ROOTFOLDER) && !isExternalStorageManager())
            ROOTFOLDER = new File(getDefaultFileParent(context));
        mainFolder = new File(ROOTFOLDER, mainFolderName);
        LogUtility.d(mainFolder);
        downloadFolder = new File(mainFolder, downloadFolderName);
        screenFolder = new File(mainFolder, screenFolderName);
        pdfFolder = new File(mainFolder, pdfFolderName);
        updateFolder = new File(mainFolder, updateFolderName);
        zipFolder = new File(mainFolder, zipFolderName);
        torrentFolder = new File(mainFolder, torrentFolderName);
        backupFolder = new File(mainFolder, backupFolderName);
    }

    @Nullable
    public static OkHttpClient getClient() {
        return client;
    }

    @NonNull
    public static OkHttpClient getClient(Context context) {
        if (client == null) initHttpClient(context);
        return client;
    }

    public static int getGalleryWidth() {
        return galleryWidth;
    }

    public static void initScreenSize(AppCompatActivity activity) {
        if (screenSize == null) {
            screenSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(screenSize);
        }
    }

    private static void initGallerySize() {
        galleryHeight = screenSize.y / 2;
        galleryWidth = (galleryHeight * 3) / 4;
    }

    public static int getScreenHeight() {
        return screenSize.y;
    }

    public static int getScreenWidth() {
        return screenSize.x;
    }

    public static int getGalleryHeight() {
        return galleryHeight;
    }

    public static int getMaxHistory() {
        return maxHistory;
    }

    public static boolean isInfiniteScrollMain() {
        return infiniteScrollMain;
    }

    public static boolean isInfiniteScrollFavorite() {
        return infiniteScrollFavorite;
    }


    private static void initTitleType(@NonNull Context context) {
        String s = context.getSharedPreferences("Settings", 0).getString(context.getString(R.string.key_title_type), "pretty");
        switch (s) {
            case "pretty":
                titleType = TitleType.PRETTY;
                break;
            case "english":
                titleType = TitleType.ENGLISH;
                break;
            case "japanese":
                titleType = TitleType.JAPANESE;
                break;
        }
    }

    public static int getDeviceWidth(@Nullable Activity activity) {
        getDeviceMetrics(activity);
        return lastDisplay.widthPixels;
    }

    public static int getDeviceHeight(@Nullable Activity activity) {
        getDeviceMetrics(activity);
        return lastDisplay.heightPixels;
    }

    private static void getDeviceMetrics(Activity activity) {
        if (activity != null)
            activity.getWindowManager().getDefaultDisplay().getMetrics(lastDisplay);
    }

    public static void initFromShared(@NonNull Context context) {
        Login.initLogin(context);
        SharedPreferences shared = context.getSharedPreferences("Settings", 0);
        CookieSyncManager.createInstance(context);
        initHttpClient(context);
        initTitleType(context);
        initTheme(context);
        loadNotificationChannel(context);
        NotificationSettings.initializeNotificationManager(context);
        Global.initStorage(context);
        shared.edit().remove("local_sort").apply();
        localSortType = new LocalSortType(shared.getInt(context.getString(R.string.key_local_sort), 0));
        useRtl = shared.getBoolean(context.getString(R.string.key_use_rtl), false);
        mirror = shared.getString(context.getString(R.string.key_site_mirror), Utility.ORIGINAL_URL);
        keepHistory = shared.getBoolean(context.getString(R.string.key_keep_history), true);
        removeAvoidedGalleries = shared.getBoolean(context.getString(R.string.key_remove_ignored), true);
        invertFix = shared.getBoolean(context.getString(R.string.key_inverted_fix), true);
        onlyTag = shared.getBoolean(context.getString(R.string.key_ignore_tags), true);
        volumeOverride = shared.getBoolean(context.getString(R.string.key_override_volume), true);
        columnCount = shared.getInt(context.getString(R.string.key_column_count), 2);
        showTitles = shared.getBoolean(context.getString(R.string.key_show_titles), true);
        buttonChangePage = shared.getBoolean(context.getString(R.string.key_change_page_buttons), true);
        lockScreen = shared.getBoolean(context.getString(R.string.key_disable_lock), false);
        hideMultitask = shared.getBoolean(context.getString(R.string.key_hide_multitasking), true);
        infiniteScrollFavorite = shared.getBoolean(context.getString(R.string.key_infinite_scroll_favo), false);
        infiniteScrollMain = shared.getBoolean(context.getString(R.string.key_infinite_scroll_main), false);
        maxId = shared.getInt(context.getString(R.string.key_max_id), 300000);
        offscreenLimit = Math.max(1, shared.getInt(context.getString(R.string.key_offscreen_limit), 5));
        maxHistory = shared.getInt(context.getString(R.string.key_max_history_size), 2);
        defaultZoom = shared.getInt(context.getString(R.string.key_default_zoom), 100);
        colPortMain = shared.getInt(context.getString(R.string.key_column_port_main), 2);
        colLandMain = shared.getInt(context.getString(R.string.key_column_land_main), 4);
        colPortDownload = shared.getInt(context.getString(R.string.key_column_port_down), 2);
        colLandDownload = shared.getInt(context.getString(R.string.key_column_land_down), 4);
        colPortFavorite = shared.getInt(context.getString(R.string.key_column_port_favo), 2);
        colLandFavorite = shared.getInt(context.getString(R.string.key_column_land_favo), 4);
        colPortHist = shared.getInt(context.getString(R.string.key_column_port_hist), 2);
        colLandHist = shared.getInt(context.getString(R.string.key_column_land_hist), 4);
        colPortStat = shared.getInt(context.getString(R.string.key_column_port_stat), 2);
        colLandStat = shared.getInt(context.getString(R.string.key_column_land_stat), 4);
        zoomOneColumn = shared.getBoolean(context.getString(R.string.key_zoom_one_column), false);
        int x = Math.max(0, shared.getInt(context.getString(R.string.key_only_language), Language.ALL.ordinal()));
        sortType = SortType.values()[shared.getInt(context.getString(R.string.key_by_popular), SortType.RECENT_ALL_TIME.ordinal())];
        usageMobile = DataUsageType.values()[shared.getInt(context.getString(R.string.key_mobile_usage), DataUsageType.FULL.ordinal())];
        usageWifi = DataUsageType.values()[shared.getInt(context.getString(R.string.key_wifi_usage), DataUsageType.FULL.ordinal())];
        if (Language.values()[x] == Language.UNKNOWN) {
            updateOnlyLanguage(context, Language.ALL);
            x = Language.ALL.ordinal();
        }
        onlyLanguage = Language.values()[x];

    }

    public static boolean isButtonChangePage() {
        return buttonChangePage;
    }

    public static boolean hideMultitask() {
        return hideMultitask;
    }

    public static LocalSortType getLocalSortType() {
        return localSortType;
    }

    public static void setLocalSortType(@NonNull Context context, @NonNull LocalSortType localSortType) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString(R.string.key_local_sort), localSortType.hashCode()).apply();
        Global.localSortType = localSortType;
        LogUtility.d("Assegning: " + localSortType);
    }

    public static String getMirror() {
        return mirror;
    }

    public static DataUsageType getDownloadPolicy() {
        switch (NetworkUtil.getType()) {
            case WIFI:
                return usageWifi;
            case CELLULAR:
                return usageMobile;
        }
        return usageWifi;
    }

    public static boolean volumeOverride() {
        return volumeOverride;
    }

    public static boolean isZoomOneColumn() {
        return zoomOneColumn;
    }

    public static void reloadHttpClient(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences("Login", 0);
        Login.setLoginShared(preferences);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .cookieJar(
                new CustomCookieJar(
                    new SetCookieCache(),
                    new SharedPrefsCookiePersistor(preferences)
                )
            );
        builder.addInterceptor(new CustomInterceptor(context.getApplicationContext(), true));
        client = builder.build();
        client.dispatcher().setMaxRequests(25);
        client.dispatcher().setMaxRequestsPerHost(25);
        for (Cookie cookie : client.cookieJar().loadForRequest(Login.BASE_HTTP_URL)) {
            LogUtility.d("Cookie: " + cookie);
        }
        Login.isLogged(context);
    }

    private static void initHttpClient(@NonNull Context context) {
        if (client != null) return;
        reloadHttpClient(context);
    }

    @NonNull
    public static Locale initLanguage(@NonNull Context context) {
        Resources resources = context.getResources();
        Locale l = getLanguage(context);
        Configuration c = new Configuration(resources.getConfiguration());
        c.setLocale(l);

        resources.updateConfiguration(c, resources.getDisplayMetrics());
        return l;
    }

    @NonNull
    public static Locale getLanguage(@NonNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Settings", 0);
        String prefLangKey = context.getString(R.string.key_language);
        String defaultValue = context.getString(R.string.key_default_value);
        String langCode = sharedPreferences.getString(prefLangKey, defaultValue);
        if (langCode.equalsIgnoreCase(defaultValue)) {
            Locale defaultLocale = Locale.getDefault();
            return defaultLocale;
        }
        if (langCode.contains("-") || langCode.contains("_")) {
            String[] regexSplit = langCode.split("[-_]");
            Locale targetLocale = new Locale(regexSplit[0], regexSplit[1]);
            return targetLocale;
        } else {
            Locale targetLocale = new Locale(langCode);
            System.out.println(targetLocale.getCountry());
            return targetLocale;
        }
    }


    public static int getOffscreenLimit() {
        return offscreenLimit;
    }

    @NonNull
    private static String getLocaleCode(@NonNull Locale locale) {
        return String.format("%s-%s", locale.getLanguage(), locale.getCountry());
    }

    private static ThemeScheme initTheme(@NonNull Context context) {
        String h = context.getSharedPreferences("Settings", 0).getString(context.getString(R.string.key_theme_select), "dark");
        return theme = h.equals("light") ? ThemeScheme.LIGHT : ThemeScheme.DARK;
    }

    public static boolean shouldCheckForUpdates(@NonNull Context context) {
        return context.getSharedPreferences("Settings", 0).getBoolean(context.getString(R.string.key_check_update), true);
    }

    public static int getLogo() {
        return theme == ThemeScheme.LIGHT ? R.drawable.ic_logo_dark : R.drawable.ic_logo;
    }

    public static Drawable getLogo(Resources resources) {
        return ResourcesCompat.getDrawable(resources, getLogo(), null);
    }

    public static float getDefaultZoom() {
        return ((float) defaultZoom) / 100f;
    }

    public static TitleType getTitleType() {
        return titleType;
    }

    public static ThemeScheme getTheme() {
        return theme;
    }

    public static boolean removeAvoidedGalleries() {
        return removeAvoidedGalleries;
    }

    @NonNull
    public static Language getOnlyLanguage() {
        return onlyLanguage;
    }

    public static boolean isOnlyTag() {
        return onlyTag;
    }

    public static boolean isLockScreen() {
        return lockScreen;
    }

    public static int getColLandDownload() {
        return colLandDownload;
    }

    public static int getColPortMain() {
        return colPortMain;
    }

    public static int getColLandMain() {
        return colLandMain;
    }

    public static int getColPortDownload() {
        return colPortDownload;
    }

    public static int getColLandFavorite() {
        return colLandFavorite;
    }

    public static int getColPortFavorite() {
        return colPortFavorite;
    }

    public static boolean isKeepHistory() {
        return keepHistory;
    }

    public static boolean useRtl() {
        return useRtl;
    }

    public static boolean showTitles() {
        return showTitles;
    }

    public static SortType getSortType() {
        return sortType;
    }


    public static int getColumnCount() {
        return columnCount;
    }

    public static int getMaxId() {
        return maxId;
    }

    public static void initStorage(Context context) {
        Global.initFilesTree(context);
        boolean[] bools = new boolean[]{
            Global.mainFolder.mkdirs(),
            Global.downloadFolder.mkdir(),
            Global.pdfFolder.mkdir(),
            Global.updateFolder.mkdir(),
            Global.screenFolder.mkdir(),
            Global.zipFolder.mkdir(),
            Global.torrentFolder.mkdir(),
            Global.backupFolder.mkdir(),
        };
        LogUtility.d(
            "0:" + context.getFilesDir() + '\n' +
                "1:" + Global.mainFolder + bools[0] + '\n' +
                "2:" + Global.downloadFolder + bools[1] + '\n' +
                "3:" + Global.pdfFolder + bools[2] + '\n' +
                "4:" + Global.updateFolder + bools[3] + '\n' +
                "5:" + Global.screenFolder + bools[4] + '\n' +
                "5:" + Global.zipFolder + bools[5] + '\n' +
                "5:" + Global.torrentFolder + bools[5] + '\n' +
                "6:" + Global.backupFolder + bools[6] + '\n'
        );

        try {
            new File(Global.mainFolder, ".nomedia").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateOnlyLanguage(@NonNull Context context, @Nullable Language type) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_only_language)), type.ordinal()).apply();
        onlyLanguage = type;
    }

    public static void updateSortType(@NonNull Context context, @NonNull SortType sortType) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_by_popular)), sortType.ordinal()).apply();
        Global.sortType = sortType;
    }

    public static void updateColumnCount(@NonNull Context context, int count) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_column_count)), count).apply();
        columnCount = count;
    }

    public static void updateMaxId(@NonNull Context context, int id) {
        context.getSharedPreferences("Settings", 0).edit().putInt(context.getString((R.string.key_max_id)), id).apply();
        maxId = id;
    }

    public static int getStatusBarHeight(@NonNull Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getNavigationBarHeight(@NonNull Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static void shareURL(Context context, String title, String url) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, title + ": " + url);
        sendIntent.setType("text/plain");
        Intent clipboardIntent = new Intent(context, CopyToClipboardActivity.class);
        clipboardIntent.setData(Uri.parse(url));
        Intent chooserIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_with));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{clipboardIntent});
        context.startActivity(chooserIntent);
    }

    public static void shareGallery(Context context, @NonNull GenericGallery gallery) {
        shareURL(context, gallery.getTitle(), Utility.getBaseUrl() + "g/" + gallery.getId());
    }

    public static void setTint(Drawable drawable) {
        if (drawable == null) return;
        DrawableCompat.setTint(drawable, theme == ThemeScheme.LIGHT ? Color.BLACK : Color.WHITE);
    }

    private static void loadNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(channelID1, context.getString(R.string.channel1_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel2 = new NotificationChannel(channelID2, context.getString(R.string.channel2_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel3 = new NotificationChannel(channelID3, context.getString(R.string.channel3_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription(context.getString(R.string.channel1_description));
            channel2.setDescription(context.getString(R.string.channel2_description));
            channel3.setDescription(context.getString(R.string.channel3_description));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel1);
                notificationManager.createNotificationChannel(channel2);
                notificationManager.createNotificationChannel(channel3);
            }
        }
    }

    @NonNull
    public static List<File> getUsableFolders(Context context) {
        List<File> strings = new ArrayList<>(3);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            strings.add(Environment.getExternalStorageDirectory());

        File[] files = context.getExternalFilesDirs(null);
        strings.addAll(Arrays.asList(files));
        return strings;
    }

    public static boolean isJPEGCorrupted(String path) {
        if (!new File(path).exists()) return true;
        try (RandomAccessFile fh = new RandomAccessFile(path, "r")) {
            long length = fh.length();
            if (length < 10L) {
                return true;
            }
            fh.seek(length - 2);
            byte[] eoi = new byte[2];
            fh.read(eoi);
            return eoi[0] != (byte) 0xFF || eoi[1] != (byte) 0xD9;
        } catch (IOException e) {
            LogUtility.e(e.getMessage(), e);
        }
        return true;
    }

    private static File findGalleryFolder(File directory, int id) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return null;
        String fileName = "." + id;
        File[] tmp = directory.listFiles();
        if (tmp == null) return null;
        for (File tmp2 : tmp) {
            if (tmp2.isDirectory() && new File(tmp2, fileName).exists()) {
                return tmp2;
            }
        }
        return null;
    }

    @Nullable
    private static File findGalleryFolder(int id) {
        return findGalleryFolder(Global.downloadFolder, id);
    }

    @Nullable
    public static File findGalleryFolder(Context context, int id) {
        if (id < 1) return null;
        if (context == null) return findGalleryFolder(id);
        for (File dir : getUsableFolders(context)) {
            dir = new File(dir, mainFolderName);
            dir = new File(dir, downloadFolderName);
            File f = findGalleryFolder(dir, id);
            if (f != null) return f;
        }
        return null;
    }

    private static void updateConfigurationNightMode(AppCompatActivity activity, @NonNull Configuration c) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        c.uiMode &= (~Configuration.UI_MODE_NIGHT_MASK);
        c.uiMode |= Configuration.UI_MODE_NIGHT_NO;
    }

    private static void invertFix(AppCompatActivity context) {
        if (!invertFix) return;
        Resources resources = context.getResources();
        Configuration c = new Configuration(resources.getConfiguration());
        updateConfigurationNightMode(context, c);
        resources.updateConfiguration(c, resources.getDisplayMetrics());
    }

    public static void initActivity(AppCompatActivity context) {
        initScreenSize(context);
        initGallerySize();
        initLanguage(context);
        invertFix(context);

        switch (initTheme(context)) {
            case LIGHT:
                context.setTheme(R.style.LightTheme);
                break;
            case DARK:
                context.setTheme(R.style.DarkTheme);
                break;
        }
    }

    public static void recursiveDelete(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File x : files) recursiveDelete(x);
        }
        file.delete();
    }

    @NonNull
    public static String getVersionName(@NonNull Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "0.0.0";
    }

    public static boolean isExternalStorageManager() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    public static void applyFastScroller(RecyclerView recycler) {
        if (recycler == null) return;
        Drawable drawable = ContextCompat.getDrawable(recycler.getContext(), R.drawable.thumb);
        if (drawable == null) return;
        new FastScrollerBuilder(recycler).setThumbDrawable(drawable).build();
    }

    @Contract(pure = true)
    @NonNull
    public static String getLanguageFlag(@NonNull Language language) {
        switch (language) {
            case CHINESE:
                return "\uD83C\uDDE8\uD83C\uDDF3";
            case ENGLISH:
                return "\uD83C\uDDEC\uD83C\uDDE7";
            case JAPANESE:
                return "\uD83C\uDDEF\uD83C\uDDF5";
            case UNKNOWN:
                return "\uD83C\uDFF3";
        }
        return "";
    }

    public enum ThemeScheme {LIGHT, DARK}

    public enum DataUsageType {NONE, THUMBNAIL, FULL}
}
