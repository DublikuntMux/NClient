package com.dublikunt.nclient.api.comments

import android.util.JsonReader
import android.util.JsonToken
import com.dublikunt.nclient.CommentActivity
import com.dublikunt.nclient.adapters.CommentAdapter
import com.dublikunt.nclient.settings.Global.client
import com.dublikunt.nclient.utility.Utility.baseUrl
import okhttp3.Request
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

class CommentsFetcher(private val commentActivity: CommentActivity, private val id: Int) :
    Thread() {
    private val comments: MutableList<Comment> = ArrayList()
    override fun run() {
        populateComments()
        postResult()
    }

    private fun postResult() {
        val commentAdapter = CommentAdapter(commentActivity, comments, id)
        commentActivity.setAdapter(commentAdapter)
        commentActivity.runOnUiThread {
            commentActivity.recycler.adapter = commentAdapter
            commentActivity.refresher.isRefreshing = false
        }
    }

    private fun populateComments() {
        val url = String.format(Locale.US, COMMENT_API_URL, id)
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body
            val reader = JsonReader(InputStreamReader(body.byteStream()))
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                reader.beginArray()
                while (reader.hasNext()) comments.add(Comment(reader))
            }
            reader.close()
            response.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val COMMENT_API_URL = baseUrl + "api/gallery/%d/comments"
    }
}
