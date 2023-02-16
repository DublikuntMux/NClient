package com.dublikunt.nclient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.comments.Comment
import com.dublikunt.nclient.settings.AuthRequest
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.DateFormat
import java.util.*

class CommentAdapter(
    private val context: AppCompatActivity,
    comments: List<Comment>,
    galleryId: Int
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {
    private val comments: MutableList<Comment>
    private val format: DateFormat = android.text.format.DateFormat.getDateFormat(context)
    private var userId = 0
    private val galleryId: Int

    init {
        this.galleryId = galleryId
        this.comments = comments as MutableList<Comment>
        userId = if (Login.isLogged() && Login.user != null) {
            Login.user!!.id
        } else -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.comment_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val position = holder.bindingAdapterPosition
        val c = comments[position]
        holder.layout.setOnClickListener { v1: View? ->
            context.runOnUiThread {
                holder.body.maxLines = if (holder.body.maxLines == 7) 999 else 7
            }
        }
        holder.close.visibility = if (c.posterId != userId) View.GONE else View.VISIBLE
        holder.user.text = c.username
        holder.body.text = c.comment
        holder.date.text = format.format(c.postDate)
        holder.close.setOnClickListener { v: View? ->
            val refererUrl = String.format(Locale.US, Utility.getBaseUrl() + "g/%d/", galleryId)
            val submitUrl =
                String.format(Locale.US, Utility.getBaseUrl() + "api/comments/%d/delete", c.id)
            AuthRequest(refererUrl, submitUrl, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.body.string().contains("true")) {
                        comments.removeAt(position)
                        context.runOnUiThread { notifyItemRemoved(position) }
                    }
                }
            }).setMethod("POST", AuthRequest.EMPTY_BODY).start()
        }
        if (c.avatarUrl == null || Global.downloadPolicy != Global.DataUsageType.FULL) loadImage(
            R.drawable.ic_person,
            holder.userImage
        ) else loadImage(
            context, c.avatarUrl, holder.userImage
        )
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    fun addComment(c: Comment) {
        comments.add(0, c)
        comments
        context.runOnUiThread { notifyItemInserted(0) }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val userImage: ImageButton
        val close: MaterialButton
        val user: MaterialTextView
        val body: MaterialTextView
        val date: MaterialTextView
        val layout: ConstraintLayout

        init {
            layout = v.findViewById(R.id.master_layout)
            userImage = v.findViewById(R.id.propic)
            close = v.findViewById(R.id.close)
            user = v.findViewById(R.id.username)
            body = v.findViewById(R.id.body)
            date = v.findViewById(R.id.date)
        }
    }
}
