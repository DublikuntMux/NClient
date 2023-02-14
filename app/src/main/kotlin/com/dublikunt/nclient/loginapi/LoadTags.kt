package com.dublikunt.nclient.loginapi

import android.util.JsonReader
import com.dublikunt.nclient.adapters.TagsAdapter
import com.dublikunt.nclient.api.components.Tag
import com.dublikunt.nclient.api.enums.TagType
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import com.dublikunt.nclient.utility.Utility.unescapeUnicodeString
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.io.StringReader
import java.util.*

class LoadTags(private val adapter: TagsAdapter?) : Thread() {
    @Throws(IOException::class)
    private fun getScripts(url: String): Elements {
        val response = Global.client!!
            .newCall(Request.Builder().url(url).build()).execute()
        val x = Jsoup.parse(response.body.byteStream(), null, Utility.getBaseUrl())
            .getElementsByTag("script")
        response.close()
        return x
    }

    @Throws(StringIndexOutOfBoundsException::class)
    private fun extractArray(e: Element?): String {
        val t = e.toString()
        return t.substring(t.indexOf('['), t.indexOf(';'))
    }

    @Throws(IOException::class)
    private fun readTags(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            val tt = Tag(reader)
            if (tt.type !== TagType.LANGUAGE && tt.type !== TagType.CATEGORY) {
                Login.addOnlineTag(tt)
                adapter?.addItem()
            }
        }
    }

    override fun run() {
        super.run()
        if (Login.getUser() == null) return
        val url = String.format(
            Locale.US, Utility.getBaseUrl() + "users/%s/%s/blacklist",
            Login.getUser().id, Login.getUser().codename
        )
        download(url)
        try {
            val scripts = getScripts(url)
            analyzeScripts(scripts)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: StringIndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class, StringIndexOutOfBoundsException::class)
    private fun analyzeScripts(scripts: Elements) {
        if (scripts.size > 0) {
            Login.clearOnlineTags()
            val array = unescapeUnicodeString(extractArray(scripts.last()))
            val reader = JsonReader(StringReader(array))
            readTags(reader)
            reader.close()
        }
    }
}
