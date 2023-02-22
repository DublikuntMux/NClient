package com.dublikunt.nclient.adapters

import android.content.DialogInterface
import android.content.Intent
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.dublikunt.nclient.GalleryActivity
import com.dublikunt.nclient.LocalActivity
import com.dublikunt.nclient.R
import com.dublikunt.nclient.api.LocalGallery
import com.dublikunt.nclient.api.LocalSortType
import com.dublikunt.nclient.async.converters.CreatePDF
import com.dublikunt.nclient.async.converters.CreateZIP
import com.dublikunt.nclient.async.database.Queries.StatusMangaTable.getStatus
import com.dublikunt.nclient.async.downloader.DownloadGallery
import com.dublikunt.nclient.async.downloader.DownloadGallery.Companion.startWork
import com.dublikunt.nclient.async.downloader.DownloadObserver
import com.dublikunt.nclient.async.downloader.DownloadQueue.addObserver
import com.dublikunt.nclient.async.downloader.DownloadQueue.downloaders
import com.dublikunt.nclient.async.downloader.DownloadQueue.givePriority
import com.dublikunt.nclient.async.downloader.DownloadQueue.remove
import com.dublikunt.nclient.async.downloader.DownloadQueue.removeObserver
import com.dublikunt.nclient.async.downloader.GalleryDownloader
import com.dublikunt.nclient.classes.MultichoiceAdapter
import com.dublikunt.nclient.settings.Global.localSortType
import com.dublikunt.nclient.settings.Global.recursiveDelete
import com.dublikunt.nclient.settings.Global.recursiveSize
import com.dublikunt.nclient.settings.Global.setTint
import com.dublikunt.nclient.utility.ImageDownloadUtility.loadImage
import com.dublikunt.nclient.utility.LogUtility.download
import com.dublikunt.nclient.utility.Utility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class LocalAdapter(private val context: LocalActivity, myDataset: ArrayList<LocalGallery>) :
    MultichoiceAdapter<Any, LocalAdapter.ViewHolder>(), Filterable {
    private val statuses = SparseIntArray()
    private lateinit var dataset: MutableList<LocalGallery>
    private lateinit var galleryDownloaders: MutableList<GalleryDownloader?>
    private val comparatorByName = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val b1 = o1 is LocalGallery
        val b2 = o2 is LocalGallery
        val s1 = if (b1) (o1 as LocalGallery).title else (o1 as GalleryDownloader).truePathTitle
        val s2 = if (b2) (o2 as LocalGallery).title else (o2 as GalleryDownloader).truePathTitle
        s1.compareTo(s2)
    }
    private val comparatorBySize = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val page1 = if (o1 is LocalGallery) recursiveSize(o1.directory) else 0
        val page2 = if (o2 is LocalGallery) recursiveSize(o2.directory) else 0
        page1.compareTo(page2)
    }
    private val comparatorByPageCount = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val page1 = if (o1 is LocalGallery) o1.pageCount else 0
        val page2 = if (o2 is LocalGallery) o2.pageCount else 0
        page1 - page2
    }
    private val comparatorByDate = Comparator { o1: Any, o2: Any ->
        if (o1 === o2) return@Comparator 0
        val b1 = o1 is LocalGallery
        val b2 = o2 is LocalGallery
        //downloading manga are newer
        if (b1 && !b2) return@Comparator -1
        if (!b1 && b2) return@Comparator 1
        if (b1 /*&&b2*/) {
            val res =
                (o1 as LocalGallery).directory.lastModified() - (o2 as LocalGallery).directory.lastModified()
            if (res != 0L) return@Comparator if (res < 0) -1 else 1
        }
        val s1 = if (b1) (o1 as LocalGallery).title else (o1 as GalleryDownloader).truePathTitle
        val s2 = if (b2) (o2 as LocalGallery).title else (o2 as GalleryDownloader).truePathTitle
        s1.compareTo(s2)
    }
    private lateinit var filter: ArrayList<Any>
    private var lastQuery: String
    private val observer: DownloadObserver = object : DownloadObserver {
        private fun updatePosition(downloader: GalleryDownloader?) {
            val id = filter.indexOf(downloader!!)
            if (id >= 0) context.runOnUiThread { notifyItemChanged(id) }
        }

        override fun triggerStartDownload(downloader: GalleryDownloader) {
            updatePosition(downloader)
        }

        override fun triggerUpdateProgress(downloader: GalleryDownloader?, reach: Int, total: Int) {
            updatePosition(downloader)
        }

        override fun triggerEndDownload(downloader: GalleryDownloader?) {
            val l = downloader!!.localGallery()
            galleryDownloaders.remove(downloader)
            if (l != null) {
                dataset.remove(l)
                dataset.add(l)
                download(l)
                sortElements()
            }
            context.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
        }

        override fun triggerCancelDownload(downloader: GalleryDownloader) {
            removeDownloader(downloader)
        }

        override fun triggerPauseDownload(downloader: GalleryDownloader?) {
            context.runOnUiThread { notifyItemChanged(filter.indexOf(downloader!!)) }
        }
    }
    private var colCount: Int

    init {
        dataset = CopyOnWriteArrayList(myDataset)
        colCount = context.colCount
        galleryDownloaders = downloaders
        lastQuery = context.query
        filter = ArrayList(myDataset)
        filter.addAll(listOf(galleryDownloaders))
        addObserver(observer)
        sortElements()
    }

    protected override fun getMaster(holder: ViewHolder): ViewGroup {
        return holder.layout
    }

    override fun getItemAt(position: Int): Any {
        return filter[position]
    }

    private fun createHash(
        galleryDownloaders: List<GalleryDownloader?>,
        dataset: List<LocalGallery>
    ): ArrayList<Any> {
        val hashMap = HashMap<String?, Any>(dataset.size + galleryDownloaders.size)
        for (gall in dataset) {
            if (gall.title.lowercase()
                    .contains(lastQuery)
            ) hashMap[gall.trueTitle] = gall
        }
        for (gall in galleryDownloaders) {
            if (gall != null && gall.truePathTitle.lowercase()
                    .contains(lastQuery)
            ) hashMap[gall.truePathTitle] = gall
        }
        val arr = ArrayList(hashMap.values)
        sortItems(arr)
        return ArrayList(arr)
    }

    private fun sortItems(arr: ArrayList<Any>) {
        val type = localSortType
        if (type.type === LocalSortType.Type.RANDOM) {
            arr.shuffle(Utility.RANDOM)
        } else {
            arr.sortWith(getComparator(type.type))
            if (type.descending) arr.reverse()
        }
    }

    private fun getComparator(type: LocalSortType.Type): Comparator<Any> {
        when (type) {
            LocalSortType.Type.DATE -> return comparatorByDate
            LocalSortType.Type.TITLE -> return comparatorByName
            LocalSortType.Type.PAGE_COUNT -> return comparatorByPageCount
            else -> {}
        }
        return comparatorByName
    }

    fun setColCount(colCount: Int) {
        this.colCount = colCount
    }

    private fun sortElements() {
        filter = createHash(galleryDownloaders, dataset)
    }

    override fun onCreateMultichoiceViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var id = 0
        when (viewType) {
            0 -> id = if (colCount == 1) R.layout.entry_layout_single else R.layout.entry_layout
            1 -> id =
                if (colCount == 1) R.layout.entry_download_layout else R.layout.entry_download_layout_compact
        }
        return ViewHolder(LayoutInflater.from(parent.context).inflate(id, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return if (filter[position] is LocalGallery) 0 else 1
    }

    private fun bindGallery(holder: ViewHolder, position: Int, ent: LocalGallery) {
        if (holder.flag != null) holder.flag.visibility = View.GONE
        loadImage(context, ent.getPage(ent.min), holder.imgView)
        holder.title.text = ent.title
        if (colCount == 1) holder.pages.text =
            context.getString(R.string.page_count_format, ent.pageCount) else holder.pages.text =
            String.format(
                Locale.US, "%d", ent.pageCount
            )
        holder.title.setOnClickListener {
            val layout = holder.title.layout
            if (layout.getEllipsisCount(layout.lineCount - 1) > 0) holder.title.maxLines =
                7 else if (holder.title.maxLines == 7) holder.title.maxLines =
                3 else holder.layout.performClick()
        }
        var statusColor = statuses[ent.id, 0]
        if (statusColor == 0) {
            statusColor = getStatus(ent.id).color
            statuses.put(ent.id, statusColor)
        }
        holder.title.setBackgroundColor(statusColor)
    }

    fun updateColor(id: Int) {
        if (id < 0) return
        statuses.put(id, getStatus(id).color)
        for (i in filter.indices) {
            val o = filter[i] as? LocalGallery ?: continue
            if (o.id == id) notifyItemChanged(i)
        }
    }

    override fun defaultMasterAction(position: Int) {
        if (position < 0 || filter.size <= position) return
        if (filter[position] !is LocalGallery) return
        val lg = filter[position] as LocalGallery
        startGallery(context, lg.directory)
        context.setIdGalleryPosition(lg.id)
    }

    private fun bindDownload(holder: ViewHolder, position: Int, downloader: GalleryDownloader) {
        val percentage = downloader.percentage
        loadImage(context, downloader.thumbnail, holder.imgView)
        holder.title.text = downloader.truePathTitle
        holder.cancelButton.setOnClickListener { removeDownloader(downloader) }
        when (downloader.status) {
            GalleryDownloader.Status.PAUSED -> {
                holder.playButton.setImageResource(R.drawable.ic_play_arrow)
                holder.playButton.setOnClickListener {
                    downloader.status = GalleryDownloader.Status.NOT_STARTED
                    startWork(context)
                    notifyItemChanged(position)
                }
            }
            GalleryDownloader.Status.DOWNLOADING -> {
                holder.playButton.setImageResource(R.drawable.ic_pause)
                holder.playButton.setOnClickListener {
                    downloader.status = GalleryDownloader.Status.PAUSED
                    notifyItemChanged(position)
                }
            }
            GalleryDownloader.Status.NOT_STARTED -> {
                holder.playButton.setImageResource(R.drawable.ic_play_arrow)
                holder.playButton.setOnClickListener { givePriority(downloader) }
            }
            else -> {}
        }
        holder.progress.text = context.getString(R.string.percentage_format, percentage)
        holder.progress.visibility =
            if (downloader.status === GalleryDownloader.Status.NOT_STARTED) View.GONE else View.VISIBLE
        holder.progressBar.progress = percentage
        holder.progressBar.isIndeterminate =
            downloader.status === GalleryDownloader.Status.NOT_STARTED
        setTint(holder.playButton.drawable)
        setTint(holder.cancelButton.drawable)
    }

    private fun removeDownloader(downloader: GalleryDownloader) {
        val position = filter.indexOf(downloader)
        if (position < 0) return
        filter.removeAt(position)
        remove(downloader, true)
        galleryDownloaders.remove(downloader)
        context.runOnUiThread { notifyItemRemoved(position) }
    }

    override fun getItemId(position: Int): Long {
        return if (position == -1) -1 else filter[position].hashCode().toLong()
    }

    override fun onBindMultichoiceViewHolder(holder: ViewHolder, position: Int) {
        if (filter[position] is LocalGallery) bindGallery(
            holder,
            position,
            filter[position] as LocalGallery
        ) else bindDownload(holder, position, filter[position] as GalleryDownloader)
    }

    private fun showDialogDelete() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.delete_galleries).setMessage(allGalleries)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            val coll = ArrayList(selected)
            for (o in coll) {
                filter.remove(o)
                if (o is LocalGallery) {
                    dataset.remove(o)
                    recursiveDelete(o.directory)
                } else if (o is DownloadGallery) {
                    remove(o as GalleryDownloader, true)
                }
            }
            context.runOnUiThread { notifyDataSetChanged() }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    private val allGalleries: String
        get() {
            val builder = StringBuilder()
            for (o in selected) {
                if (o is LocalGallery) builder.append(o.title) else builder.append((o as GalleryDownloader).truePathTitle)
                builder.append('\n')
            }
            return builder.toString()
        }

    private fun showDialogPDF() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.create_pdf).setMessage(allGalleries)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            for (o in selected) {
                if (o !is LocalGallery) continue
                CreatePDF.startWork(context, (o as LocalGallery?)!!)
            }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    private fun showDialogZip() {
        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(R.string.create_zip).setMessage(allGalleries)
        builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            for (o in selected) {
                if (o !is LocalGallery) continue
                CreateZIP.startWork(context, (o as LocalGallery?)!!)
            }
        }.setNegativeButton(R.string.no, null).setCancelable(true)
        builder.show()
    }

    fun hasSelectedClass(c: Class<*>): Boolean {
        for (x in selected) if (x.javaClass == c) return true
        return false
    }

    override fun getItemCount(): Int {
        return filter.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults? {
                val query = constraint.toString().lowercase()
                if (lastQuery == query) return null
                val results = FilterResults()
                lastQuery = query
                results.values = createHash(galleryDownloaders, dataset)
                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                filter = results.values as ArrayList<Any>
                context.runOnUiThread { notifyDataSetChanged() }
            }
        }
    }

    fun removeObserver() {
        removeObserver(observer)
    }

    fun viewRandom() {
        if (dataset.size == 0) return
        val x = Utility.RANDOM.nextInt(dataset.size)
        startGallery(context, dataset[x].directory)
    }

    fun sortChanged() {
        sortElements()
        context.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
    }

    fun startSelected() {
        for (o in selected) {
            if (o !is GalleryDownloader) continue
            if (o.status === GalleryDownloader.Status.PAUSED) o.status =
                GalleryDownloader.Status.NOT_STARTED
        }
        context.runOnUiThread { notifyDataSetChanged() }
    }

    fun pauseSelected() {
        for (o in selected) {
            if (o !is GalleryDownloader) continue
            o.status = GalleryDownloader.Status.PAUSED
        }
        context.runOnUiThread { notifyDataSetChanged() }
    }

    fun deleteSelected() {
        showDialogDelete()
    }

    fun zipSelected() {
        showDialogZip()
    }

    fun pdfSelected() {
        showDialogPDF()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgView: ImageView
        val overlay: View
        val title: MaterialTextView
        val pages: MaterialTextView
        val flag: MaterialTextView?
        val progress: MaterialTextView
        val layout: ViewGroup
        val playButton: ImageButton
        val cancelButton: ImageButton
        val progressBar: ProgressBar

        init {
            //Both
            imgView = v.findViewById(R.id.image)
            title = v.findViewById(R.id.title)
            //Local
            pages = v.findViewById(R.id.pages)
            layout = v.findViewById(R.id.master_layout)
            flag = v.findViewById(R.id.flag)
            overlay = v.findViewById(R.id.overlay)
            //Downloader
            progress = itemView.findViewById(R.id.progress)
            progressBar = itemView.findViewById(R.id.progressBar)
            playButton = itemView.findViewById(R.id.playButton)
            cancelButton = itemView.findViewById(R.id.cancelButton)
        }
    }

    companion object {
        fun startGallery(context: AppCompatActivity, directory: File) {
            if (!directory.isDirectory) return
            val ent = LocalGallery(directory)
            ent.calculateSizes()
            Thread {
                val intent = Intent(context, GalleryActivity::class.java)
                intent.putExtra(context.packageName + ".GALLERY", ent)
                intent.putExtra(context.packageName + ".ISLOCAL", true)
                context.runOnUiThread { context.startActivity(intent) }
            }.start()
        }
    }
}
