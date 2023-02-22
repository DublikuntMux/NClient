package com.dublikunt.nclient.adapters

import android.content.DialogInterface
import android.content.Intent
import android.text.format.DateFormat
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.CopyToClipboardActivity.Companion.copyTextToClipboard
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.MainActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.ZoomActivity
import com.dublikunt.nclient.api.LocalGallery
import com.dublikunt.nclient.api.components.Gallery
import com.dublikunt.nclient.api.components.GalleryData
import com.dublikunt.nclient.api.components.GenericGallery
import com.dublikunt.nclient.async.database.Queries.ResumeTable.insert
import com.dublikunt.nclient.classes.Size
import com.dublikunt.nclient.components.photoview.PhotoView
import com.dublikunt.nclient.components.widgets.CustomGridLayoutManager
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.enums.TagType
import com.dublikunt.nclient.files.GalleryFolder
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Global.SCREENFOLDER
import com.dublikunt.nclient.settings.Global.applyFastScroller
import com.dublikunt.nclient.settings.Global.downloadPolicy
import com.dublikunt.nclient.settings.Global.findGalleryFolder
import com.dublikunt.nclient.settings.Global.hasStoragePermission
import com.dublikunt.nclient.settings.Global.isZoomOneColumn
import com.dublikunt.nclient.settings.Global.useRtl
import com.dublikunt.nclient.utility.ImageDownloadUtility.downloadPage
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImageOp
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.util.*

class GalleryAdapter(
    private val context: GalleryActivity,
    private val gallery: GenericGallery,
    colCount: Int
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    private val maxSize: Size = gallery.maxSize
    private val minSize: Size = gallery.minSize
    private val angles = SparseIntArray()
    var directory: GalleryFolder? = null
    private var maxImageSize: Size? = null
    private var policy: Policy? = null
    private var colCount = 0

    init {
        setColCount(colCount)
        try {
            if (gallery is LocalGallery) {
                directory = gallery.galleryFolder
            } else if (hasStoragePermission(context)) {
                if (gallery.id != -1) {
                    val f = findGalleryFolder(context, gallery.id)
                    if (f != null) directory = GalleryFolder(f)
                } else {
                    directory = GalleryFolder(gallery.title)
                }
            }
        } catch (ignore: IllegalArgumentException) {
            directory = null
        }
        download("Max maxSize: " + maxSize + ", min maxSize: " + gallery.minSize)
    }

    fun positionToType(pos: Int): Type {
        if (pos == 0) return Type.TAG
        return if (pos > gallery.pageCount) Type.RELATED else Type.PAGE
    }

    fun setColCount(colCount: Int) {
        this.colCount = colCount
        applyProportionPolicy()
    }

    private fun applyProportionPolicy() {
        policy =
            if (colCount == 1) Policy.FULL else if (maxSize.height - minSize.height < TOLERANCE) Policy.MAX else Policy.PROPORTION
        download("NEW POLICY: $policy")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var id = 0
        when (Type.values()[viewType]) {
            Type.TAG -> id = R.layout.tags_layout
            Type.PAGE -> when (policy) {
                Policy.MAX -> id = R.layout.image_void
                Policy.FULL -> id = R.layout.image_void_full
                Policy.PROPORTION -> id = R.layout.image_void_static
                else -> {}
            }
            Type.RELATED -> id = R.layout.related_recycler
        }
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(id, parent, false),
            Type.values()[viewType]
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (positionToType(holder.bindingAdapterPosition)) {
            Type.TAG -> loadTagLayout(holder)
            Type.PAGE -> loadPageLayout(holder)
            Type.RELATED -> loadRelatedLayout(holder)
        }
    }

    private fun loadRelatedLayout(holder: ViewHolder) {
        download("Called RElated")
        val recyclerView = holder.master.findViewById<RecyclerView>(R.id.recycler)
        if (gallery.isLocal) {
            holder.master.visibility = View.GONE
            return
        }
        val gallery = gallery as Gallery
        if (!gallery.isRelatedLoaded || gallery.related.size == 0) {
            holder.master.visibility = View.GONE
            return
        } else holder.master.visibility = View.VISIBLE
        recyclerView.layoutManager =
            CustomGridLayoutManager(context, 1, RecyclerView.HORIZONTAL, false)
        if (gallery.isRelatedLoaded) {
            val adapter = ListAdapter(context)
            adapter.addGalleries(ArrayList(gallery.related))
            recyclerView.adapter = adapter
        }
    }

    private fun loadTagLayout(holder: ViewHolder) {
        val vg = holder.master.findViewById<ViewGroup>(R.id.tag_master)
        val idContainer = holder.master.findViewById<TextView>(R.id.id_num)
        initializeIdContainer(idContainer)
        if (!hasTags()) {
            val layoutParams = vg.layoutParams
            layoutParams.height = 0
            vg.layoutParams = layoutParams
            return
        }
        val inflater = context.layoutInflater
        var tagCount: Int
        var idStringTagName: Int
        var lay: ViewGroup
        var cg: ChipGroup
        val tagList = gallery.galleryData.tags
        for (type in TagType.values) {
            idStringTagName = TAG_NAMES[type.id.toInt()]
            tagCount = tagList.getCount(type)
            lay = vg.getChildAt(type.id.toInt()) as ViewGroup
            cg = lay.findViewById(R.id.chip_group)
            if (cg.childCount != 0) continue
            lay.visibility = if (tagCount == 0) View.GONE else View.VISIBLE
            (lay.findViewById<View>(R.id.title) as TextView).setText(idStringTagName)
            for (a in 0 until tagCount) {
                val tag = tagList.getTag(type, a)
                val c = inflater.inflate(R.layout.chip_layout, cg, false) as Chip
                c.text = tag.name
                c.setOnClickListener {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra(context.packageName + ".TAG", tag)
                    intent.putExtra(context.packageName + ".ISBYTAG", true)
                    context.startActivity(intent)
                }
                c.setOnLongClickListener {
                    copyTextToClipboard(context, tag.name)
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    true
                }
                cg.addView(c)
            }
            addInfoLayout(holder, gallery.galleryData)
        }
    }

    private fun initializeIdContainer(idContainer: TextView) {
        if (gallery.id <= 0) {
            idContainer.visibility = View.GONE
            return
        }
        val id = gallery.id.toString()
        idContainer.text = id
        idContainer.visibility =
            if (gallery.id != SpecialTagIds.INVALID_ID.toInt()) View.VISIBLE else View.GONE
        idContainer.setOnClickListener {
            copyTextToClipboard(context, id)
            context.runOnUiThread {
                Toast.makeText(
                    context,
                    R.string.id_copied_to_clipboard,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addInfoLayout(holder: ViewHolder, gallery: GalleryData) {
        var text = holder.master.findViewById<TextView>(R.id.page_count)
        text.text = context.getString(R.string.page_count_format, gallery.pageCount)
        text = holder.master.findViewById(R.id.upload_date)
        text.text = context.getString(
            R.string.upload_date_format,
            DateFormat.getDateFormat(context).format(gallery.uploadDate),
            DateFormat.getTimeFormat(context).format(gallery.uploadDate)
        )
        text = holder.master.findViewById(R.id.favorite_count)
        text.text = context.getString(R.string.favorite_count_format, gallery.favoriteCount)
    }

    fun setMaxImageSize(maxImageSize: Size?) {
        this.maxImageSize = maxImageSize
        context.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
    }

    private fun loadPageLayout(holder: ViewHolder) {
        val pos = holder.bindingAdapterPosition
        val imgView = holder.master.findViewById<ImageView>(R.id.image)
        imgView.setOnClickListener { startGallery(holder.bindingAdapterPosition) }
        imgView.setOnLongClickListener(null)
        holder.master.setOnClickListener { startGallery(holder.bindingAdapterPosition) }
        holder.master.setOnLongClickListener(null)
        holder.pageNumber!!.text = String.format(Locale.US, "%d", pos)
        if (policy == Policy.MAX) holder.itemView.post {
            //find the max size and apply proportion
            if (maxImageSize != null) return@post
            val cellWidth = holder.itemView.width // this will give you cell width dynamically
            download(String.format(Locale.US, "Setting: %d,%s", cellWidth, maxSize.toString()))
            if (maxSize.width > 10 && maxSize.height > 10) {
                val hei = maxSize.height * cellWidth / maxSize.width
                if (hei >= 100) setMaxImageSize(Size(cellWidth, hei))
            }
        }
        if (policy == Policy.MAX && maxImageSize != null) {
            val params = imgView.layoutParams
            params.height = maxImageSize!!.height
            params.width = maxImageSize!!.width
            imgView.layoutParams = params
        }
        if (policy == Policy.FULL) {
            val photoView = imgView as PhotoView
            photoView.isZoomable = isZoomOneColumn
            photoView.setOnMatrixChangeListener {
                photoView.setAllowParentInterceptOnEdge(
                    photoView.scale <= 1f
                )
            }
            photoView.setOnClickListener {
                if (photoView.scale <= 1f) startGallery(
                    holder.bindingAdapterPosition
                )
            }
            val listener = OnLongClickListener {
                optionDialog(imgView, pos)
                true
            }
            imgView.setOnLongClickListener(listener)
            holder.master.setOnLongClickListener(listener)
        }
        loadImageOnPolicy(imgView, pos)
    }

    private fun optionDialog(imgView: ImageView, pos: Int) {
        val adapter = ArrayAdapter<String>(context, android.R.layout.select_dialog_item)
        adapter.add(context.getString(R.string.share))
        adapter.add(context.getString(R.string.rotate_image))
        adapter.add(context.getString(R.string.bookmark_here))
        if (hasStoragePermission(context)) adapter.add(context.getString(R.string.save_page))
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.settings).setIcon(R.drawable.ic_share)
        builder.setAdapter(adapter) { _: DialogInterface?, which: Int ->
            when (which) {
                0 -> openSendImageDialog(imgView, pos)
                1 -> rotate(pos)
                2 -> insert(gallery.id, pos)
                3 -> {
                    val name = String.format(Locale.US, "%d-%d.jpg", gallery.id, pos)
                    Utility.saveImage(imgView.drawable, File(SCREENFOLDER, name))
                }
            }
        }.show()
    }

    private fun openSendImageDialog(img: ImageView, pos: Int) {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            sendImage(
                img,
                pos,
                true
            )
        }
            .setNegativeButton(R.string.no) { _: DialogInterface?, _: Int ->
                sendImage(
                    img,
                    pos,
                    false
                )
            }
            .setCancelable(true).setTitle(R.string.send_with_title)
            .setMessage(R.string.caption_send_with_title)
            .show()
    }

    private fun sendImage(img: ImageView, pos: Int, text: Boolean) {
        Utility.sendImage(context, img.drawable, if (text) gallery.sharePageUrl(pos - 1) else null)
    }

    private fun rotate(pos: Int) {
        angles.append(pos, (angles[pos] + 270) % 360)
        context.runOnUiThread { notifyItemChanged(pos) }
    }

    private fun startGallery(page: Int) {
        if (!gallery.isLocal && downloadPolicy === Global.DataUsageType.NONE) {
            context.runOnUiThread {
                Toast.makeText(
                    context,
                    R.string.enable_network_to_continue,
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        val intent = Intent(context, ZoomActivity::class.java)
        intent.putExtra(context.packageName + ".GALLERY", gallery)
        intent.putExtra(context.packageName + ".DIRECTORY", directory)
        intent.putExtra(context.packageName + ".PAGE", page)
        context.startActivity(intent)
    }

    private fun loadImageOnPolicy(imgView: ImageView, pos: Int) {
        val file: File? = directory?.getPage(pos)
        val angle = angles[pos]
        if (policy == Policy.FULL) {
            if (file != null && file.exists()) loadImageOp(
                context,
                imgView,
                file,
                angle
            ) else if (!gallery.isLocal) {
                val ent = gallery as Gallery
                loadImageOp(context, imgView, ent, pos - 1, angle)
            } else loadImage(R.mipmap.ic_launcher, imgView)
        } else {
            if (file != null && file.exists()) loadImage(
                context,
                file,
                imgView
            ) else if (!gallery.isLocal) {
                val ent = gallery as Gallery
                downloadPage(context, imgView, ent, pos - 1, false)
            } else loadImage(R.mipmap.ic_launcher, imgView)
        }
    }

    private fun hasTags(): Boolean {
        return gallery.hasGalleryData()
    }

    override fun getItemViewType(position: Int): Int {
        return positionToType(position).ordinal
    }

    override fun getItemCount(): Int {
        return gallery.pageCount + 2
    }

    enum class Type {
        TAG, PAGE, RELATED
    }

    enum class Policy {
        PROPORTION, MAX, FULL
    }

    class ViewHolder(v: View, type: Type) : RecyclerView.ViewHolder(v) {
        val master: View
        var pageNumber: MaterialTextView? = null

        init {
            master = v.findViewById(R.id.master)
            pageNumber = v.findViewById(R.id.page_number)
            if (useRtl()) v.rotationY = 180f
            if (type == Type.RELATED) applyFastScroller(master.findViewById(R.id.recycler))
        }
    }

    companion object {
        private val TAG_NAMES = intArrayOf(
            R.string.unknown,
            R.string.tag_parody_gallery,
            R.string.tag_character_gallery,
            R.string.tag_tag_gallery,
            R.string.tag_artist_gallery,
            R.string.tag_group_gallery,
            R.string.tag_language_gallery,
            R.string.tag_category_gallery
        )
        private const val TOLERANCE = 1000
    }
}
