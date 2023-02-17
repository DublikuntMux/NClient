package com.dublikunt.nclient.async.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.dublikunt.nclient.api.Inspector
import com.dublikunt.nclient.api.SimpleGallery
import com.dublikunt.nclient.api.components.*
import com.dublikunt.nclient.enums.ApiRequestType
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.enums.TitleType
import com.dublikunt.nclient.async.downloader.GalleryDownloaderManager
import com.dublikunt.nclient.async.downloader.GalleryDownloader
import com.dublikunt.nclient.classes.Bookmark
import com.dublikunt.nclient.components.status.Status
import com.dublikunt.nclient.components.status.StatusManager
import com.dublikunt.nclient.components.status.StatusManager.add
import com.dublikunt.nclient.components.status.StatusManager.getByName
import com.dublikunt.nclient.settings.Global.maxHistory
import com.dublikunt.nclient.settings.Global.titleType
import com.dublikunt.nclient.settings.Tags.minCount
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import com.dublikunt.nclient.utility.LogUtility.info
import java.io.IOException
import java.util.*

@SuppressLint("Range")
object Queries {
    lateinit var db: SQLiteDatabase

    @JvmStatic
    fun getColumnFromName(cursor: Cursor, name: String?): Int {
        return cursor.getColumnIndex(name)
    }

    /**
     * Table with information about the galleries
     */
    object GalleryTable {
        const val TABLE_NAME = "Gallery"
        const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
        const val IDGALLERY = "idGallery"
        const val TITLE_ENG = "title_eng"
        const val TITLE_JP = "title_jp"
        const val TITLE_PRETTY = "title_pretty"
        const val FAVORITE_COUNT = "favorite_count"
        const val MEDIAID = "mediaId"
        const val FAVORITE = "favorite"
        const val PAGES = "pages"
        const val UPLOAD = "upload"
        const val MAX_WIDTH = "maxW"
        const val MAX_HEIGHT = "maxH"
        const val MIN_WIDTH = "minW"
        const val MIN_HEIGHT = "minH"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Gallery` ( " +
            "`idGallery`      INT               NOT NULL PRIMARY KEY , " +
            "`title_eng`      TINYTEXT          NOT NULL, " +
            "`title_jp`       TINYTEXT          NOT NULL, " +
            "`title_pretty`   TINYTEXT          NOT NULL, " +
            "`favorite_count` INT               NOT NULL, " +
            "`mediaId`        INT               NOT NULL, " +
            "`pages`          TEXT              NOT NULL," +
            "`upload`         UNSIGNED BIG INT  NOT NULL," +  //Date
            "`maxW`           INT               NOT NULL," +
            "`maxH`           INT               NOT NULL," +
            "`minW`           INT               NOT NULL," +
            "`minH`           INT               NOT NULL" +
            ");"

        fun clearGalleries() {
            db.delete(
                TABLE_NAME, String.format(
                    Locale.US,
                    "%s NOT IN (SELECT %s FROM %s) AND " +
                        "%s NOT IN (SELECT %s FROM %s) AND " +
                        "%s NOT IN (SELECT %s FROM %s)",
                    IDGALLERY, DownloadTable.ID_GALLERY, DownloadTable.TABLE_NAME,
                    IDGALLERY, FavoriteTable.ID_GALLERY, FavoriteTable.TABLE_NAME,
                    IDGALLERY, StatusMangaTable.GALLERY, StatusMangaTable.TABLE_NAME
                ), null
            )
            db.delete(
                GalleryBridgeTable.TABLE_NAME, String.format(
                    Locale.US,
                    "%s NOT IN (SELECT %s FROM %s)",
                    GalleryBridgeTable.ID_GALLERY, IDGALLERY, TABLE_NAME
                ), null
            )
            db.delete(
                FavoriteTable.TABLE_NAME, String.format(
                    Locale.US,
                    "%s NOT IN (SELECT %s FROM %s)",
                    FavoriteTable.ID_GALLERY, IDGALLERY, TABLE_NAME
                ), null
            )
            db.delete(
                DownloadTable.TABLE_NAME, String.format(
                    Locale.US,
                    "%s NOT IN (SELECT %s FROM %s)",
                    DownloadTable.ID_GALLERY, IDGALLERY, TABLE_NAME
                ), null
            )
        }

        /**
         * Retrieve gallery using the id
         *
         * @param id id of the gallery to retrieve
         */
        @Throws(IOException::class)
        fun galleryFromId(id: Int): Gallery? {
            val cursor = db.query(
                true,
                TABLE_NAME,
                null,
                "$IDGALLERY=?",
                arrayOf("" + id),
                null,
                null,
                null,
                null
            )
            var g: Gallery? = null
            if (cursor.moveToFirst()) {
                g = cursorToGallery(cursor)
            }
            cursor.close()
            return g
        }

        fun getAllFavoriteCursorDeprecated(query: CharSequence?, online: Boolean): Cursor {
            info("FILTER IN: $query;;$online")
            val cursor: Cursor
            var sql = "SELECT * FROM " + TABLE_NAME + " WHERE (" +
                FAVORITE + " =? OR " + FAVORITE + "=3)"
            if (query != null && query.isNotEmpty()) {
                sql += " AND (" + TITLE_ENG + " LIKE ? OR " +
                    TITLE_JP + " LIKE ? OR " +
                    TITLE_PRETTY + " LIKE ? )"
                val q = "%$query%"
                cursor = db.rawQuery(sql, arrayOf("" + if (online) 2 else 1, q, q, q))
            } else cursor = db.rawQuery(sql, arrayOf("" + if (online) 2 else 1))
            download(sql)
            download("AFTER FILTERING: " + cursor.count)
            info("END FILTER IN: $query;;$online")
            return cursor
        }

        /**
         * Retrieve all galleries inside the DB
         */
        @get:Throws(IOException::class)
        val allGalleries: List<Gallery>
            get() {
                val query = "SELECT * FROM $TABLE_NAME"
                val cursor = db.rawQuery(query, null)
                val galleries: MutableList<Gallery> = ArrayList(cursor.count)
                if (cursor.moveToFirst()) {
                    do {
                        galleries.add(cursorToGallery(cursor))
                    } while (cursor.moveToNext())
                }
                cursor.close()
                return galleries
            }

        fun insert(gallery: GenericGallery) {
            val values = ContentValues(12)
            val data = gallery.galleryData
            values.put(IDGALLERY, gallery.id)
            values.put(TITLE_ENG, data.getTitle(TitleType.ENGLISH))
            values.put(TITLE_JP, data.getTitle(TitleType.JAPANESE))
            values.put(TITLE_PRETTY, data.getTitle(TitleType.PRETTY))
            values.put(FAVORITE_COUNT, data.favoriteCount)
            values.put(MEDIAID, data.mediaId)
            values.put(PAGES, data.createPagePath())
            values.put(UPLOAD, data.uploadDate.time)
            values.put(MAX_WIDTH, gallery.maxSize.width)
            values.put(MAX_HEIGHT, gallery.maxSize.height)
            values.put(MIN_WIDTH, gallery.minSize.width)
            values.put(MIN_HEIGHT, gallery.minSize.height)
            //Insert gallery
            db.insertWithOnConflict(
                TABLE_NAME,
                null,
                values,
                if (gallery is Gallery) SQLiteDatabase.CONFLICT_REPLACE else SQLiteDatabase.CONFLICT_IGNORE
            )
            TagTable.insertTagsForGallery(data)
        }

        /**
         * Convert a cursor pointing to galleries to a list of galleries, cursor not closed
         *
         * @param cursor Cursor to scroll
         * @return ArrayList of galleries
         */
        @Throws(IOException::class)
        fun cursorToList(cursor: Cursor): List<Gallery> {
            val galleries: MutableList<Gallery> = ArrayList(cursor.count)
            if (cursor.moveToFirst()) {
                do {
                    galleries.add(cursorToGallery(cursor))
                } while (cursor.moveToNext())
            }
            return galleries
        }

        fun delete(id: Int) {
            db.delete(TABLE_NAME, "$IDGALLERY=?", arrayOf("" + id))
            GalleryBridgeTable.deleteGallery(id)
        }

        /**
         * Convert a row of a cursor to a [Gallery]
         */
        @Throws(IOException::class)
        fun cursorToGallery(cursor: Cursor): Gallery {
            return Gallery(
                cursor, GalleryBridgeTable.getTagsForGallery(
                    cursor.getInt(
                        getColumnFromName(cursor, IDGALLERY)
                    )
                )
            )
        }

        /**
         * Insert max and min size of a certain [Gallery]
         */
        fun updateSizes(gallery: Gallery?) {
            if (gallery == null) return
            val values = ContentValues(4)
            values.put(MAX_WIDTH, gallery.maxSize.width)
            values.put(MAX_HEIGHT, gallery.maxSize.height)
            values.put(MIN_WIDTH, gallery.minSize.width)
            values.put(MIN_HEIGHT, gallery.minSize.height)
            db.updateWithOnConflict(
                "Gallery",
                values,
                "$IDGALLERY=?",
                arrayOf("" + gallery.id),
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    object TagTable {
        const val TABLE_NAME = "Tags"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Tags` (" +
            " `idTag` INT  NOT NULL PRIMARY KEY," +
            " `name` TEXT NOT NULL , " +
            "`type` TINYINT(1) NOT NULL , " +
            "`count` INT NOT NULL," +
            "`status` TINYINT(1) NOT NULL," +
            "`online` TINYINT(1) NOT NULL DEFAULT 0);"
        const val IDTAG = "idTag"
        const val NAME = "name"
        const val TYPE = "type"
        const val COUNT = "count"
        const val STATUS = "status"
        const val ONLINE = "online"

        /**
         * Convert a [Cursor] row to a [Tag]
         */
        @JvmStatic
        fun cursorToTag(cursor: Cursor): Tag {
            return Tag(
                cursor.getString(cursor.getColumnIndex(NAME)),
                cursor.getInt(cursor.getColumnIndex(COUNT)),
                cursor.getInt(cursor.getColumnIndex(IDTAG)),
                TagType.values[cursor.getInt(cursor.getColumnIndex(TYPE))],
                TagStatus.values()[cursor.getInt(cursor.getColumnIndex(STATUS))]
            )
        }

        /**
         * Fetch all rows inside a [Cursor] and convert them into a [Tag]
         * The [Cursor] passed as parameter is closed
         */
        fun getTagsFromCursor(cursor: Cursor): List<Tag> {
            val tags: MutableList<Tag> = ArrayList(cursor.count)
            val i = 0
            if (cursor.moveToFirst()) {
                do {
                    tags.add(cursorToTag(cursor))
                } while (cursor.moveToNext())
            }
            cursor.close()
            return tags
        }

        /**
         * Return a cursor which points to a list of [Tag] which have certain properties
         *
         * @param query      Retrieve only tags which contains a certain string
         * @param type       If not null only tags which are of a specific [TagType]
         * @param online     Retrieve only tags which have been blacklisted from the main site
         * @param sortByName sort by name or by count
         */
        @JvmStatic
        fun getFilterCursor(
            query: String,
            type: TagType?,
            online: Boolean,
            sortByName: Boolean
        ): Cursor {
            //create query
            val sql = StringBuilder("SELECT * FROM ").append(TABLE_NAME)
            sql.append(" WHERE ")
            sql.append(COUNT).append(">=? ") //min tag count
            if (query.isNotEmpty()) sql.append("AND ").append(NAME)
                .append(" LIKE ?") //query if is used
            if (type != null) sql.append("AND ").append(TYPE).append("=? ") //type if is used
            if (online) sql.append("AND ").append(ONLINE).append("=1 ") //retrieve only online tags
            if (!online && type == null) sql.append("AND ").append(STATUS)
                .append("!=0 ") //retrieve only used tags
            sql.append("ORDER BY ") //sort first by name if provided, the for count
            if (!sortByName) sql.append(COUNT).append(" DESC,")
            sql.append(NAME).append(" ASC")

            //create parameter list
            val list = ArrayList<String>()
            list.add("" + minCount) //minium tags (always provided)
            if (query.isNotEmpty()) list.add("%$query%") //query
            if (type != null) list.add("" + type.id) //type of the tag
            download("FILTER URL: $sql, ARGS: $list")
            return db.rawQuery(sql.toString(), list.toTypedArray())
        }

        /**
         * Returns a List of all tags of a specific type and which have a min count
         *
         * @param type The type to fetch
         */
        fun getAllTagOfType(type: TagType): List<Tag> {
            val query =
                "SELECT * FROM $TABLE_NAME WHERE $TYPE = ? AND $COUNT >= ?"
            return getTagsFromCursor(db.rawQuery(query, arrayOf("" + type.id, "" + minCount)))
        }

        /**
         * Returns a List of all tags of a specific type
         *
         * @param type The type to fetch
         */
        fun getTrueAllType(type: TagType): List<Tag> {
            val query = "SELECT * FROM $TABLE_NAME WHERE $TYPE = ?"
            return getTagsFromCursor(db.rawQuery(query, arrayOf("" + type.id)))
        }

        /**
         * Returns a List of all tags of a specific status
         *
         * @param status The status to fetch
         */
        @JvmStatic
        fun getAllStatus(status: TagStatus): List<Tag> {
            val query = "SELECT * FROM $TABLE_NAME WHERE $STATUS = ?"
            return getTagsFromCursor(db.rawQuery(query, arrayOf("" + status.ordinal)))
        }

        /**
         * Returns a List of all tags which are AVOIDED or ACCEPTED
         */
        @JvmStatic
        val allFiltered: List<Tag>
            get() {
                val query = "SELECT * FROM $TABLE_NAME WHERE $STATUS != ?"
                return getTagsFromCursor(
                    db.rawQuery(
                        query,
                        arrayOf("" + TagStatus.DEFAULT.ordinal)
                    )
                )
            }

        /**
         * Returns a List of all tags which are AVOIDED or ACCEPTED of a specific type
         */
        fun getAllFilteredByType(type: TagType?): List<Tag> {
            val query = "SELECT * FROM $TABLE_NAME WHERE $STATUS != ?"
            return getTagsFromCursor(db.rawQuery(query, arrayOf("" + TagStatus.DEFAULT.ordinal)))
        }

        /**
         * Returns a List of all tags which have been blacklisted from the site
         */
        @JvmStatic
        val allOnlineBlacklisted: List<Tag>
            get() {
                val query = "SELECT * FROM $TABLE_NAME WHERE $ONLINE = 1"
                val t = getTagsFromCursor(
                    db.rawQuery(query, null)
                )
                for (t1 in t) t1.status = TagStatus.AVOIDED
                return t
            }

        /**
         * Returns true if the tag has been blacklisted form the main site
         */
        fun isBlackListed(tag: Tag): Boolean {
            val query =
                "SELECT $IDTAG FROM $TABLE_NAME WHERE $IDTAG=? AND $ONLINE=1"
            val c = db.rawQuery(query, arrayOf("" + tag.id))
            val x = c.moveToFirst()
            c.close()
            return x
        }

        /**
         * Returns the tag which has a specific if, null if it does not exists
         */
        @JvmStatic
        fun getTagById(id: Int): Tag? {
            val query = "SELECT * FROM $TABLE_NAME WHERE $IDTAG = ?"
            val c = db.rawQuery(query, arrayOf("" + id))
            var t: Tag? = null
            if (c.moveToFirst()) t = cursorToTag(c)
            c.close()
            return t
        }

        @JvmStatic
        fun updateStatus(id: Int, status: TagStatus): Int {
            val values = ContentValues(1)
            values.put(STATUS, status.ordinal)
            return db.updateWithOnConflict(
                TABLE_NAME,
                values,
                "$IDTAG=?",
                arrayOf("" + id),
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        /**
         * Update status and count of a specific tag
         */
        fun updateTag(tag: Tag): Int {
            insert(tag)
            val values = ContentValues(2)
            values.put(STATUS, tag.status.ordinal)
            values.put(COUNT, tag.count)
            return db.updateWithOnConflict(
                TABLE_NAME,
                values,
                "$IDTAG=?",
                arrayOf("" + tag.id),
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        @JvmOverloads
        @JvmStatic
        fun insert(tag: Tag, replace: Boolean = false) {
            val values = ContentValues(5)
            values.put(IDTAG, tag.id)
            values.put(NAME, tag.name)
            values.put(TYPE, tag.type.id)
            values.put(COUNT, tag.count)
            values.put(STATUS, tag.status.ordinal)
            db.insertWithOnConflict(
                TABLE_NAME,
                null,
                values,
                if (replace) SQLiteDatabase.CONFLICT_REPLACE else SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        fun updateBlacklistedTag(tag: Tag, online: Boolean) {
            val values = ContentValues(1)
            values.put(ONLINE, if (online) 1 else 0)
            db.updateWithOnConflict(
                TABLE_NAME,
                values,
                "$IDTAG=?",
                arrayOf("" + tag.id),
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        fun removeAllBlacklisted() {
            val values = ContentValues(1)
            values.put(ONLINE, 0)
            db.updateWithOnConflict(
                TABLE_NAME,
                values,
                null,
                null,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        fun resetAllStatus() {
            val values = ContentValues(1)
            values.put(STATUS, TagStatus.DEFAULT.ordinal)
            db.updateWithOnConflict(
                TABLE_NAME,
                values,
                null,
                null,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        /**
         * Get the first `count` tags of `type`, ordered by tag count
         */
        fun getTopTags(type: TagType, count: Int): List<Tag> {
            val query =
                "SELECT * FROM $TABLE_NAME WHERE $TYPE=? ORDER BY $COUNT DESC LIMIT ?;"
            val cursor = db.rawQuery(query, arrayOf("" + type.id, "" + count))
            return getTagsFromCursor(cursor)
        }

        /**
         * Retrieve the status of a tag from the DB and set it
         *
         * @return the status if the tag exists, null otherwise
         */
        fun getStatus(tag: Tag): TagStatus? {
            val query = "SELECT " + STATUS + " FROM " + TABLE_NAME +
                " WHERE " + IDTAG + " =?"
            val c = db.rawQuery(query, arrayOf("" + tag.id))
            var status: TagStatus? = null
            if (c.moveToFirst()) {
                status = cursorToTag(c).status
                tag.status = status
            }
            c.close()
            return status
        }

        fun getTagFromTagName(name: String): Tag? {
            var tag: Tag? = null
            val cursor = db.query(TABLE_NAME, null, "$NAME=?", arrayOf(name), null, null, null)
            if (cursor.moveToFirst()) tag = cursorToTag(cursor)
            cursor.close()
            return tag
        }

        /**
         * @param tagString a comma-separated list of integers (maybe vulnerable)
         * @return the tags with id contained inside the list
         */
        @JvmStatic
        fun getTagsFromListOfInt(tagString: String): TagList {
            val tags = TagList()
            val query =
                "SELECT * FROM $TABLE_NAME WHERE $IDTAG IN ($tagString)"
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    tags.addTag(cursorToTag(cursor))
                } while (cursor.moveToNext())
            }
            return tags
        }

        /**
         * Return a list of tags which contain name and are of a certain type
         */
        fun search(name: String, type: TagType): List<Tag> {
            val query =
                "SELECT * FROM $TABLE_NAME WHERE $NAME LIKE ? AND $TYPE=?"
            download(query)
            val c = db.rawQuery(query, arrayOf("%$name%", "" + type.id))
            return getTagsFromCursor(c)
        }

        /**
         * Search a tag by name and type
         *
         * @return The Tag if found, null otehrwise
         */
        fun searchTag(name: String, type: TagType): Tag? {
            var tag: Tag? = null
            val query = "SELECT * FROM $TABLE_NAME WHERE $NAME = ? AND $TYPE=?"
            download(query)
            val c = db.rawQuery(query, arrayOf(name, "" + type.id))
            if (c.moveToFirst()) tag = cursorToTag(c)
            c.close()
            return tag
        }

        /**
         * Insert all tags owned by a gallery and link it using [GalleryBridgeTable]
         */
        fun insertTagsForGallery(gallery: GalleryData) {
            val tags = gallery.tags
            var len: Int
            var tag: Tag
            for (t in TagType.values) {
                len = tags.getCount(t)
                for (i in 0 until len) {
                    tag = tags.getTag(t, i)
                    insert(tag) //Insert tag
                    GalleryBridgeTable.insert(gallery.id, tag.id) //Insert link
                }
            }
        }

        /*To avoid conflict between the import process and the ScrapeTags*/
        @JvmStatic
        fun insertScrape(tag: Tag, b: Boolean) {
            if (db.isOpen) insert(tag, b)
        }
    }

    object DownloadTable {
        const val ID_GALLERY = "id_gallery"
        const val RANGE_START = "range_start"
        const val RANGE_END = "range_end"
        const val TABLE_NAME = "Downloads"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Downloads` (" +
            "`id_gallery`  INT NOT NULL PRIMARY KEY , " +
            "`range_start` INT NOT NULL," +
            "`range_end`   INT NOT NULL," +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE" +
            "); "

        @JvmStatic
        fun addGallery(downloader: GalleryDownloader) {
            val gallery = downloader.gallery
            GalleryTable.insert(gallery)
            val values = ContentValues(3)
            values.put(ID_GALLERY, gallery.id)
            values.put(RANGE_START, downloader.start)
            values.put(RANGE_END, downloader.end)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        @JvmStatic
        fun removeGallery(id: Int) {
            val favorite = FavoriteTable.isFavorite(id)
            if (!favorite) GalleryTable.delete(id)
            db.delete(TABLE_NAME, "$ID_GALLERY=?", arrayOf("" + id))
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getAllDownloads(context: Context): List<GalleryDownloaderManager> {
            val q = "SELECT * FROM %s INNER JOIN %s ON %s=%s"
            val query = String.format(
                Locale.US,
                q,
                GalleryTable.TABLE_NAME,
                TABLE_NAME,
                GalleryTable.IDGALLERY,
                ID_GALLERY
            )
            val c = db.rawQuery(query, null)
            val managers: MutableList<GalleryDownloaderManager> = ArrayList()
            var x: Gallery
            var m: GalleryDownloaderManager
            if (c.moveToFirst()) {
                do {
                    x = GalleryTable.cursorToGallery(c)
                    m = GalleryDownloaderManager(
                        context, x, c.getInt(c.getColumnIndex(RANGE_START)), c.getInt(
                            c.getColumnIndex(
                                RANGE_END
                            )
                        )
                    )
                    managers.add(m)
                } while (c.moveToNext())
            }
            c.close()
            return managers
        }
    }

    object HistoryTable {
        const val ID = "id"
        const val MEDIAID = "mediaId"
        const val TITLE = "title"
        const val THUMB = "thumbType"
        const val TIME = "time"
        const val TABLE_NAME = "History"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `History`(" +
            "`id` INT NOT NULL PRIMARY KEY," +
            "`mediaId` INT NOT NULL," +
            "`title` TEXT NOT NULL," +
            "`thumbType` TINYINT(1) NOT NULL," +
            "`time` INT NOT NULL" +
            ");"

        fun addGallery(gallery: SimpleGallery) {
            if (gallery.id <= 0) return
            val values = ContentValues(5)
            values.put(ID, gallery.id)
            values.put(MEDIAID, gallery.mediaId)
            values.put(TITLE, gallery.title)
            values.put(THUMB, gallery.thumb.ordinal)
            values.put(TIME, Date().time)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            cleanHistory()
        }

        val history: List<SimpleGallery>
            get() {
                val galleries = ArrayList<SimpleGallery>()
                val c = db.query(
                    TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "$TIME DESC",
                    "" + maxHistory
                )
                if (c.moveToFirst()) {
                    do {
                        galleries.add(SimpleGallery(c))
                    } while (c.moveToNext())
                }
                galleries.trimToSize()
                return galleries
            }

        fun emptyHistory() {
            db.delete(TABLE_NAME, null, null)
        }

        private fun cleanHistory() {
            while (db.delete(
                    TABLE_NAME,
                    "(SELECT COUNT(*) FROM $TABLE_NAME)>? AND $TIME=(SELECT MIN($TIME) FROM $TABLE_NAME)",
                    arrayOf("" + maxHistory)
                ) == 1
            );
        }
    }

    object BookmarkTable {
        const val TABLE_NAME = "Bookmark"
        const val URL = "url"
        const val PAGE = "page"
        const val TYPE = "type"
        const val TAG_ID = "tagId"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Bookmark`(" +
            "`url` TEXT NOT NULL UNIQUE," +
            "`page` INT NOT NULL," +
            "`type` INT NOT NULL," +
            "`tagId` INT NOT NULL" +
            ");"

        fun deleteBookmark(url: String) {
            download("Deleted: " + db.delete(TABLE_NAME, "$URL=?", arrayOf(url)))
        }

        fun addBookmark(inspector: Inspector) {
            val tag = inspector.tag
            val values = ContentValues(4)
            values.put(URL, inspector.url)
            values.put(PAGE, inspector.page)
            values.put(TYPE, inspector.requestType.ordinal())
            values.put(TAG_ID, tag?.id ?: 0)
            download("ADDED: " + inspector.url)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        val bookmarks: List<Bookmark>
            get() {
                val query = "SELECT * FROM $TABLE_NAME"
                val cursor = db.rawQuery(query, null)
                val bookmarks: MutableList<Bookmark> = ArrayList(cursor.count)
                var b: Bookmark
                download("This url has " + cursor.count)
                if (cursor.moveToFirst()) {
                    do {
                        b = Bookmark(
                            cursor.getString(cursor.getColumnIndex(URL)),
                            cursor.getInt(cursor.getColumnIndex(PAGE)),
                            ApiRequestType.values[cursor.getInt(cursor.getColumnIndex(TYPE))],
                            cursor.getInt(cursor.getColumnIndex(TAG_ID))
                        )
                        bookmarks.add(b)
                    } while (cursor.moveToNext())
                }
                cursor.close()
                return bookmarks
            }
    }

    object GalleryBridgeTable {
        const val TABLE_NAME = "GalleryTags"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `GalleryTags` (" +
            "`id_gallery` INT NOT NULL , " +
            "`id_tag` INT NOT NULL ," +
            "PRIMARY KEY (`id_gallery`, `id_tag`), " +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE , " +
            "FOREIGN KEY(`id_tag`) REFERENCES `Tags`(`idTag`) ON UPDATE CASCADE ON DELETE RESTRICT );"
        const val ID_GALLERY = "id_gallery"
        const val ID_TAG = "id_tag"
        fun insert(galleryId: Int, tagId: Int) {
            val values = ContentValues(2)
            values.put(ID_GALLERY, galleryId)
            values.put(ID_TAG, tagId)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        fun deleteGallery(id: Int) {
            db.delete(TABLE_NAME, "$ID_GALLERY=?", arrayOf("" + id))
        }

        fun getTagCursorForGallery(id: Int): Cursor {
            val query = String.format(
                Locale.US, "SELECT * FROM %s WHERE %s IN (SELECT %s FROM %s WHERE %s=%d)",
                TagTable.TABLE_NAME,
                TagTable.IDTAG,
                ID_TAG,
                TABLE_NAME,
                ID_GALLERY,
                id
            )
            return db.rawQuery(query, null)
        }

        fun getTagsForGallery(id: Int): TagList {
            val c = getTagCursorForGallery(id)
            val tagList = TagList()
            val tags = TagTable.getTagsFromCursor(c)
            tagList.addTags(tags)
            return tagList
        }
    }

    object FavoriteTable {
        const val TABLE_NAME = "Favorite"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Favorite` (" +
            "`id_gallery` INT NOT NULL PRIMARY KEY , " +
            "`time` INT NOT NULL," +
            "FOREIGN KEY(`id_gallery`) REFERENCES `Gallery`(`idGallery`) ON UPDATE CASCADE ON DELETE CASCADE);"
        const val ID_GALLERY = "id_gallery"
        const val TIME = "time"
        private val TITLE_CLAUSE = String.format(
            Locale.US, "%s LIKE ? OR %s LIKE ? OR %s LIKE ?",
            GalleryTable.TITLE_ENG,
            GalleryTable.TITLE_JP,
            GalleryTable.TITLE_PRETTY
        )
        private val FAVORITE_JOIN_GALLERY = String.format(
            Locale.US, "%s INNER JOIN %s ON %s=%s",
            TABLE_NAME,
            GalleryTable.TABLE_NAME,
            ID_GALLERY,
            GalleryTable.IDGALLERY
        )

        fun addFavorite(gallery: Gallery) {
            GalleryTable.insert(gallery)
            insert(gallery.id)
        }

        fun titleTypeToColumn(type: TitleType?): String {
            return when (type) {
                TitleType.PRETTY -> GalleryTable.TITLE_PRETTY
                TitleType.ENGLISH -> GalleryTable.TITLE_ENG
                TitleType.JAPANESE -> GalleryTable.TITLE_JP
                else -> GalleryTable.TITLE_PRETTY
            }
        }

        /**
         * Get all favorites galleries which title contains `query`
         *
         * @param orderByTitle true if order by title, false order by latest
         * @return cursor which points to the galleries
         */
        fun getAllFavoriteGalleriesCursor(
            query: CharSequence,
            orderByTitle: Boolean,
            limit: Int,
            offset: Int
        ): Cursor {
            val order = if (orderByTitle) titleTypeToColumn(titleType) else "$TIME DESC"
            val param = "%$query%"
            val limitString = String.format(Locale.US, " %d, %d ", offset, limit)
            return db.query(
                FAVORITE_JOIN_GALLERY,
                null,
                TITLE_CLAUSE,
                arrayOf(param, param, param),
                null,
                null,
                order,
                limitString
            )
        }

        /**
         * Get all favorites galleries
         *
         * @return cursor which points to the galleries
         */
        val allFavoriteGalleriesCursor: Cursor
            get() {
                val query = String.format(
                    Locale.US, "SELECT * FROM %s WHERE %s IN (SELECT %s FROM %s)",
                    GalleryTable.TABLE_NAME,
                    GalleryTable.IDGALLERY,
                    ID_GALLERY,
                    TABLE_NAME
                )
                return db.rawQuery(query, null)
            }

        /**
         * Retrieve all favorite galleries
         */
        @get:Throws(IOException::class)
        val allFavoriteGalleries: List<Gallery>
            get() {
                val c = allFavoriteGalleriesCursor
                val galleries = GalleryTable.cursorToList(c)
                c.close()
                return galleries
            }

        fun insert(galleryId: Int) {
            val values = ContentValues(2)
            values.put(ID_GALLERY, galleryId)
            values.put(TIME, Date().time)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        fun removeFavorite(id: Int) {
            db.delete(TABLE_NAME, "$ID_GALLERY=?", arrayOf("" + id))
        }

        fun countFavorite(text: String?): Int {
            if (text == null || text.trim { it <= ' ' }.isEmpty()) return countFavorite()
            var totalFavorite = 0
            val param = "%$text%"
            val c = db.query(
                FAVORITE_JOIN_GALLERY,
                arrayOf("COUNT(*)"),
                TITLE_CLAUSE,
                arrayOf(param, param, param),
                null,
                null,
                null
            )
            if (c.moveToFirst()) {
                totalFavorite = c.getInt(0)
            }
            c.close()
            return totalFavorite
        }

        fun countFavorite(): Int {
            var totalFavorite = 0
            val query = "SELECT COUNT(*) FROM $TABLE_NAME"
            val c = db.rawQuery(query, null)
            if (c.moveToFirst()) {
                totalFavorite = c.getInt(0)
            }
            c.close()
            return totalFavorite
        }

        fun isFavorite(id: Int): Boolean {
            val query = "SELECT * FROM $TABLE_NAME WHERE $ID_GALLERY=?"
            val c = db.rawQuery(query, arrayOf("" + id))
            val b = c.moveToFirst()
            c.close()
            return b
        }

        fun removeAllFavorite() {
            db.delete(TABLE_NAME, null, null)
        }
    }

    object ResumeTable {
        const val TABLE_NAME = "Resume"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Resume` (" +
            "`id_gallery` INT NOT NULL PRIMARY KEY , " +
            "`page` INT NOT NULL" +
            ");"
        const val ID_GALLERY = "id_gallery"
        const val PAGE = "page"
        fun insert(id: Int, page: Int) {
            if (id < 0) return
            val values = ContentValues(2)
            values.put(ID_GALLERY, id)
            values.put(PAGE, page)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            download("Added bookmark to page $page of id $id")
        }

        fun pageFromId(id: Int): Int {
            if (id < 0) return -1
            var `val` = -1
            val c = db.query(
                TABLE_NAME,
                arrayOf(PAGE),
                "$ID_GALLERY= ?",
                arrayOf("" + id),
                null,
                null,
                null
            )
            if (c.moveToFirst()) `val` = c.getInt(c.getColumnIndex(PAGE))
            c.close()
            return `val`
        }

        fun remove(id: Int) {
            db.delete(TABLE_NAME, "$ID_GALLERY= ?", arrayOf("" + id))
        }
    }

    object StatusMangaTable {
        const val TABLE_NAME = "StatusManga"
        const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `StatusManga` (" +
            "`gallery` INT NOT NULL PRIMARY KEY, " +
            "`name` TINYTEXT NOT NULL, " +
            "`time` INT NOT NULL," +
            "FOREIGN KEY(`gallery`) REFERENCES `" + GalleryTable.TABLE_NAME + "`(`" + GalleryTable.IDGALLERY + "`) ON UPDATE CASCADE ON DELETE CASCADE," +
            "FOREIGN KEY(`name`) REFERENCES `" + StatusTable.TABLE_NAME + "`(`" + StatusTable.NAME + "`) ON UPDATE CASCADE ON DELETE CASCADE" +
            ");"
        const val NAME = "name"
        const val GALLERY = "gallery"
        const val TIME = "time"
        fun insert(gallery: GenericGallery, status: Status?) {
            val values = ContentValues(3)
            GalleryTable.insert(gallery)
            StatusTable.insert(status)
            values.put(NAME, status!!.name)
            values.put(GALLERY, gallery.id)
            values.put(TIME, Date().time)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        fun remove(id: Int) {
            db.delete(TABLE_NAME, "$GALLERY=?", arrayOf("" + id))
        }

        @JvmStatic
        fun getStatus(id: Int): Status {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(NAME),
                "$GALLERY=?",
                arrayOf("" + id),
                null,
                null,
                null
            )
            val status: Status = if (cursor.moveToFirst()) getByName(
                cursor.getString(
                    cursor.getColumnIndex(NAME)
                )
            )!! else getByName(StatusManager.DEFAULT_STATUS)!!
            cursor.close()
            return status
        }

        fun insert(gallery: GenericGallery, s: String?) {
            insert(gallery, getByName(s!!))
        }

        fun update(oldStatus: Status, newStatus: Status) {
            val values = ContentValues(1)
            values.put(NAME, newStatus.name)
            values.put(TIME, Date().time)
            db.update(TABLE_NAME, values, "$NAME=?", arrayOf(oldStatus.name))
        }

        fun getGalleryOfStatus(name: String, filter: String, sortByTitle: Boolean): Cursor {
            val query = String.format(
                "SELECT * FROM %s INNER JOIN %s ON %s=%s WHERE %s=? AND (%s LIKE ? OR %s LIKE ? OR %s LIKE ?) ORDER BY %s",
                GalleryTable.TABLE_NAME, TABLE_NAME,
                GalleryTable.IDGALLERY, GALLERY,
                NAME,
                GalleryTable.TITLE_ENG, GalleryTable.TITLE_JP, GalleryTable.TITLE_PRETTY,
                if (sortByTitle) FavoriteTable.titleTypeToColumn(titleType) else "$TIME DESC"
            )
            val likeFilter = "%$filter%"
            download(query)
            return db.rawQuery(query, arrayOf(name, likeFilter, likeFilter, likeFilter))
        }

        val countsPerStatus: HashMap<String, Int>
            get() {
                val query = String.format(
                    "select %s, count(%s) as count from %s group by %s;",
                    NAME, GALLERY, TABLE_NAME, NAME
                )
                download(query)
                val cursor = db.rawQuery(query, null)
                val counts = HashMap<String, Int>()
                while (cursor.moveToNext()) {
                    try {
                        val status = cursor.getString(0)
                        val count = cursor.getInt(1)
                        counts[status] = count
                    } catch (e: Exception) {
                        error(e)
                    }
                }
                cursor.close()
                return counts
            }

        fun removeStatus(name: String) {
            db.delete(TABLE_NAME, "$NAME=?", arrayOf(name))
        }
    }

    object StatusTable {
        const val TABLE_NAME = "Status"
        const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `Status` (" +
            "`name` TINYTEXT NOT NULL PRIMARY KEY, " +
            "`color` INT NOT NULL " +
            ");"
        const val NAME = "name"
        const val COLOR = "color"
        fun insert(status: Status?) {
            val values = ContentValues(2)
            values.put(NAME, status!!.name)
            values.put(COLOR, status.color)
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        fun remove(name: String) {
            db.delete(TABLE_NAME, "$NAME= ?", arrayOf(name))
            StatusMangaTable.removeStatus(name)
        }

        fun initStatuses() {
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
            if (cursor.moveToFirst()) {
                do {
                    add(
                        cursor.getString(cursor.getColumnIndex(NAME)),
                        cursor.getInt(cursor.getColumnIndex(COLOR))
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        fun update(oldStatus: Status, newStatus: Status) {
            val values = ContentValues(2)
            values.put(NAME, newStatus.name)
            values.put(COLOR, newStatus.color)
            db.update(TABLE_NAME, values, "$NAME=?", arrayOf(oldStatus.name))
            StatusMangaTable.update(oldStatus, newStatus)
        }
    }
}
