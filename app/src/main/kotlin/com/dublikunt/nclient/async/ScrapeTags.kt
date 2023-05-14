package com.dublikunt.nclient.async

import android.content.Context
import android.content.Intent
import android.util.JsonReader
import androidx.core.app.JobIntentService
import com.dublikunt.nclient.api.comments.Tag
import com.dublikunt.nclient.async.database.Queries.TagTable.allFiltered
import com.dublikunt.nclient.async.database.Queries.TagTable.insertScrape
import com.dublikunt.nclient.async.database.Queries.TagTable.updateStatus
import com.dublikunt.nclient.enums.TagStatus
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.LogUtility.error
import okhttp3.Request
import java.io.IOException
import java.util.*

class ScrapeTags : JobIntentService() {
    @get:Throws(IOException::class)
    private val newVersionCode: Int
        get() {
            val x = client.newCall(Request.Builder().url(VERSION).build()).execute()
            val body = x.body
            try {
                val k = body.string().trim { it <= ' ' }.toInt()
                download("Found version: $k")
                x.close()
                return k
            } catch (e: NumberFormatException) {
                error("Unable to convert")
            }
            return -1
        }

    override fun onHandleWork(intent: Intent) {
        val preferences = applicationContext.getSharedPreferences("Settings", 0)
        val nowTime = Date()
        val lastTime = Date(preferences.getLong("lastSync", nowTime.time))
        val lastVersion = preferences.getInt("lastTagsVersion", -1)
        var newVersion = -1
        if (!enoughDayPassed(nowTime, lastTime)) return
        download("Scraping tags")
        try {
            newVersion = newVersionCode
            if (lastVersion > -1 && lastVersion >= newVersion) return
            val tags = allFiltered
            fetchTags()
            for (t in tags) updateStatus(t.id, t.status)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        download("End scraping")
        preferences.edit()
            .putLong("lastSync", nowTime.time)
            .putInt("lastTagsVersion", newVersion)
            .apply()
    }

    @Throws(IOException::class)
    private fun fetchTags() {
        val x = client.newCall(Request.Builder().url(TAGS).build()).execute()
        val body = x.body
        val reader = JsonReader(body.charStream())
        reader.beginArray()
        while (reader.hasNext()) {
            val tag = readTag(reader)
            insertScrape(tag, true)
        }
        reader.close()
        x.close()
    }

    @Throws(IOException::class)
    private fun readTag(reader: JsonReader): Tag {
        reader.beginArray()
        val id = reader.nextInt()
        val name = reader.nextString()
        val count = reader.nextInt()
        val type = TagType.values[reader.nextInt()]
        reader.endArray()
        return Tag(name, count, id, type, TagStatus.DEFAULT)
    }

    private fun enoughDayPassed(nowTime: Date, lastTime: Date): Boolean {
        if (nowTime.time == lastTime.time) return true
        var daysBetween = 0
        val now = Calendar.getInstance()
        val last = Calendar.getInstance()
        now.time = nowTime
        last.time = lastTime
        while (last.before(now)) {
            last.add(Calendar.DAY_OF_MONTH, 1)
            daysBetween++
            if (daysBetween > DAYS_UNTIL_SCRAPE) return true
        }
        download("Passed $daysBetween days since last scrape")
        return false
    }

    companion object {
        private const val DAYS_UNTIL_SCRAPE = 7
        private const val DATA_FOLDER =
            "https://raw.githubusercontent.com/Dar9586/NClientV2/master/data/"
        private const val TAGS = DATA_FOLDER + "tags.json"
        private const val VERSION = DATA_FOLDER + "tagsVersion"
        fun startWork(context: Context?) {
            enqueueWork(context!!, ScrapeTags::class.java, 2000, Intent())
        }
    }
}
