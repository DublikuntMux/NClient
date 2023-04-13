package com.dublikunt.nclient.async.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.components.status.StatusManager
import com.dublikunt.nclient.components.status.StatusManager.add
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.settings.Database.database
import com.dublikunt.nclient.utility.LogUtility.download
import java.io.IOException
import java.util.*

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
        database = db
        insertLanguageTags()
        insertCategoryTags()
        insertDefaultStatus()
        //Queries.DebugDatabase.dumpDatabase(db);
    }

    private fun createAllTables(db: SQLiteDatabase) {
        db.execSQL(Queries.GalleryTable.CREATE_TABLE)
        db.execSQL(Queries.TagTable.CREATE_TABLE)
        db.execSQL(Queries.GalleryBridgeTable.CREATE_TABLE)
        db.execSQL(Queries.BookmarkTable.CREATE_TABLE)
        db.execSQL(Queries.DownloadTable.CREATE_TABLE)
        db.execSQL(Queries.HistoryTable.CREATE_TABLE)
        db.execSQL(Queries.FavoriteTable.CREATE_TABLE)
        db.execSQL(Queries.ResumeTable.CREATE_TABLE)
        db.execSQL(Queries.StatusTable.CREATE_TABLE)
        db.execSQL(Queries.StatusMangaTable.CREATE_TABLE)
    }

    private fun insertCategoryTags() {
        val types = arrayOf(
            Tag("doujinshi", 0, 33172, TagType.CATEGORY, TagStatus.DEFAULT),
            Tag("manga", 0, 33173, TagType.CATEGORY, TagStatus.DEFAULT),
            Tag("misc", 0, 97152, TagType.CATEGORY, TagStatus.DEFAULT),
            Tag("western", 0, 34125, TagType.CATEGORY, TagStatus.DEFAULT),
            Tag("non-h", 0, 34065, TagType.CATEGORY, TagStatus.DEFAULT),
            Tag("artistcg", 0, 36320, TagType.CATEGORY, TagStatus.DEFAULT)
        )
        for (t in types) Queries.TagTable.insert(t)
    }

    private fun insertLanguageTags() {
        val languages = arrayOf(
            Tag(
                "english",
                0,
                SpecialTagIds.LANGUAGE_ENGLISH.toInt(),
                TagType.LANGUAGE,
                TagStatus.DEFAULT
            ),
            Tag(
                "japanese",
                0,
                SpecialTagIds.LANGUAGE_JAPANESE.toInt(),
                TagType.LANGUAGE,
                TagStatus.DEFAULT
            ),
            Tag(
                "chinese",
                0,
                SpecialTagIds.LANGUAGE_CHINESE.toInt(),
                TagType.LANGUAGE,
                TagStatus.DEFAULT
            )
        )
        for (t in languages) Queries.TagTable.insert(t)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database = db
        if (oldVersion == 2) insertLanguageTags()
        if (oldVersion <= 3) insertCategoryTags()
        if (oldVersion <= 4) db.execSQL(Queries.BookmarkTable.CREATE_TABLE)
        if (oldVersion <= 5) updateGalleryWithSizes(db)
        if (oldVersion <= 6) db.execSQL(Queries.DownloadTable.CREATE_TABLE)
        if (oldVersion <= 7) db.execSQL(Queries.HistoryTable.CREATE_TABLE)
        if (oldVersion <= 8) insertFavorite(db)
        if (oldVersion <= 9) addRangeColumn(db)
        if (oldVersion <= 10) db.execSQL(Queries.ResumeTable.CREATE_TABLE)
        if (oldVersion <= 11) updateFavoriteTable(db)
        if (oldVersion <= 12) addStatusTables(db)
    }

    private fun addStatusTables(db: SQLiteDatabase) {
        db.execSQL(Queries.StatusTable.CREATE_TABLE)
        db.execSQL(Queries.StatusMangaTable.CREATE_TABLE)
        insertDefaultStatus()
    }

    private fun insertDefaultStatus() {
        add(context.getString(R.string.default_status_1), Color.BLUE)
        add(context.getString(R.string.default_status_2), Color.GREEN)
        add(context.getString(R.string.default_status_3), Color.YELLOW)
        add(context.getString(R.string.default_status_4), Color.RED)
        add(context.getString(R.string.default_status_5), Color.GRAY)
        add(StatusManager.DEFAULT_STATUS, Color.BLACK)
    }

    private fun updateFavoriteTable(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE Favorite ADD COLUMN `time` INT NOT NULL DEFAULT " + Date().time)
    }

    private fun addRangeColumn(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE Downloads ADD COLUMN `range_start` INT NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE Downloads ADD COLUMN `range_end`   INT NOT NULL DEFAULT -1")
    }

    /**
     * Add all item which are favorite into the favorite table
     */
    @get:SuppressLint("Range")
    private val allFavoriteIndex: IntArray
        get() {
            val c = Queries.GalleryTable.getAllFavoriteCursorDeprecated("%", false)
            val favorites = IntArray(c.count)
            var i = 0
            if (c.moveToFirst()) {
                do {
                    favorites[i++] = c.getInt(c.getColumnIndex(Queries.GalleryTable.IDGALLERY))
                } while (c.moveToNext())
            }
            c.close()
            return favorites
        }

    /**
     * Create favorite table
     * Get all id of favorite gallery
     * save all galleries
     * delete and recreate table without favorite column
     * insert all galleries again
     * populate favorite
     */
    private fun insertFavorite(db: SQLiteDatabase) {
        database = db
        db.execSQL(Queries.FavoriteTable.CREATE_TABLE)
        try {
            val favorites = allFavoriteIndex
            val allGalleries = Queries.GalleryTable.allGalleries
            db.execSQL(Queries.GalleryTable.DROP_TABLE)
            db.execSQL(Queries.GalleryTable.CREATE_TABLE)
            for (g in allGalleries) Queries.GalleryTable.insert(g)
            for (i in favorites) Queries.FavoriteTable.insert(i)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Add the columns which contains the sizes of the images
     */
    private fun updateGalleryWithSizes(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxW` INT NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `maxH` INT NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minW` INT NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Gallery ADD COLUMN `minH` INT NOT NULL DEFAULT 0")
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        download("Downgrading database from $oldVersion to $newVersion")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        database = db
        Queries.GalleryTable.clearGalleries()
    }

    companion object {
        const val DATABASE_NAME = "Entries.db"
        private const val DATABASE_VERSION = 13
    }
}
