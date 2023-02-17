package com.dublikunt.nclient

import android.content.res.Configuration
import android.os.Bundle
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.dublikunt.nclient.adapters.CommentAdapter
import com.dublikunt.nclient.api.comments.Comment
import com.dublikunt.nclient.api.comments.CommentsFetcher
import com.dublikunt.nclient.components.activities.BaseActivity
import com.dublikunt.nclient.settings.AuthRequest
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.StringWriter
import java.util.*

class CommentActivity : BaseActivity() {
    private lateinit var adapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.comments)
        findViewById<View>(R.id.page_switcher).visibility = View.GONE
        val id = intent.getIntExtra("$packageName.GALLERYID", -1)
        if (id == -1) {
            finish()
            return
        }
        recycler = findViewById(R.id.recycler)
        refresher = findViewById(R.id.refresher)
        refresher.setOnRefreshListener { CommentsFetcher(this@CommentActivity, id).start() }
        val commentText = findViewById<TextInputEditText>(R.id.commentText)
        findViewById<View>(R.id.card).visibility = if (Login.isLogged()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.sendButton).setOnClickListener {
            if (commentText.text.toString().length < MINIUM_MESSAGE_LENGHT) {
                Toast.makeText(
                    this,
                    getString(R.string.minimum_comment_length, MINIUM_MESSAGE_LENGHT),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val refererUrl = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", id)
            val submitUrl = String.format(
                Locale.US,
                Utility.getBaseUrl() + "api/gallery/%d/comments/submit",
                id
            )
            val requestString = createRequestString(commentText.text.toString())
            commentText.setText("")
            val body: RequestBody = requestString.toRequestBody("application/json".toMediaType())
            AuthRequest(refererUrl, submitUrl, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val reader = JsonReader(response.body.charStream())
                    var comment: Comment? = null
                    reader.beginObject()
                    while (reader.peek() != JsonToken.END_OBJECT) {
                        if ("comment" == reader.nextName()) {
                            comment = Comment(reader)
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.close()
                    if (comment != null) adapter.addComment(comment)
                }
            }).setMethod("POST", body).start()
        }
        changeLayout(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        refresher.isRefreshing = true
        CommentsFetcher(this@CommentActivity, id).start()
    }

    fun setAdapter(adapter: CommentAdapter) {
        this.adapter = adapter
    }

    private fun createRequestString(text: String): String {
        try {
            val writer = StringWriter()
            val json = JsonWriter(writer)
            json.beginObject()
            json.name("body").value(text)
            json.endObject()
            val finalText = writer.toString()
            json.close()
            return finalText
        } catch (ignore: IOException) {
        }
        return ""
    }

    override val portraitColumnCount: Int
        get() {
            return 1
        }

    override val landscapeColumnCount: Int
        get() {
            return 2
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val MINIUM_MESSAGE_LENGHT = 10
    }
}
