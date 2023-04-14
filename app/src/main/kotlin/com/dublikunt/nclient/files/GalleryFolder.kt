package com.dublikunt.nclient.files

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.collection.SparseArrayCompat
import com.dublikunt.nclient.api.comments.Page
import com.dublikunt.nclient.enums.SpecialTagIds
import com.dublikunt.nclient.settings.Global.DOWNLOADFOLDER
import com.dublikunt.nclient.settings.Global.findGalleryFolder
import java.io.File
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

open class GalleryFolder : Parcelable, Iterable<PageFile> {
    private val pageArray = SparseArrayCompat<PageFile>()
    val folder: File
    var id = SpecialTagIds.INVALID_ID.toInt()
        private set
    var max = -1
        private set
    var min = Int.MAX_VALUE
        private set
    var galleryDataFile: File? = null
        private set

    constructor(child: String) : this(DOWNLOADFOLDER, child) {}
    constructor(parent: File?, child: String) : this(File(parent, child)) {}
    constructor(file: File) {
        folder = file
        require(folder.isDirectory) { "File is not a folder" }
        parseFiles()
    }

    protected constructor(`in`: Parcel) {
        folder = File(Objects.requireNonNull(`in`.readString()))
        id = `in`.readInt()
        min = `in`.readInt()
        max = `in`.readInt()
        val pageCount = `in`.readInt()
        for (i in 0 until pageCount) {
            val k = `in`.readInt()
            val f = `in`.readParcelable<PageFile>(PageFile::class.java.classLoader)
            pageArray.put(k, f)
        }
    }

    private fun parseFiles() {
        val files = folder.listFiles() ?: return
        for (f in files) {
            elaborateFile(f)
        }
    }

    private fun elaborateFile(f: File) {
        val name = f.name
        var matcher = FILE_PATTERN.matcher(name)
        if (matcher.matches()) elaboratePage(f, matcher)
        if (id == SpecialTagIds.INVALID_ID.toInt()) {
            matcher = ID_FILE_PATTERN.matcher(name)
            if (matcher.matches()) id = elaborateId(matcher)
        }
        if (galleryDataFile == null && name == NOMEDIA_FILE) galleryDataFile = f
    }

    private fun elaborateId(matcher: Matcher): Int {
        return Objects.requireNonNull(matcher.group(1)).toInt()
    }

    val pageCount: Int
        get() = pageArray.size()
    val pages: Array<PageFile?>
        get() {
            val files = arrayOfNulls<PageFile>(pageArray.size())
            for (i in 0 until pageArray.size()) {
                files[i] = pageArray.valueAt(i)
            }
            return files
        }

    private fun elaboratePage(f: File, matcher: Matcher) {
        val page = Objects.requireNonNull(matcher.group(1)).toInt()
        val ext = Page.charToExt(
            Objects.requireNonNull(
                matcher.group(2)
            )[0].code
        )
            ?: return
        pageArray.append(page, PageFile(ext, f, page))
        if (page > max) max = page
        if (page < min) min = page
    }

    fun getPage(page: Int): PageFile? {
        return pageArray[page]
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(folder.absolutePath)
        dest.writeInt(id)
        dest.writeInt(min)
        dest.writeInt(max)
        dest.writeInt(pageArray.size())
        for (i in 0 until pageArray.size()) {
            dest.writeInt(pageArray.keyAt(i))
            dest.writeParcelable(pageArray.valueAt(i), flags)
        }
    }

    override fun iterator(): PageFileIterator {
        return PageFileIterator(pageArray)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val pageFiles = o as GalleryFolder
        return folder == pageFiles.folder
    }

    override fun hashCode(): Int {
        return folder.hashCode()
    }

    override fun toString(): String {
        return "GalleryFolder{" +
            "pageArray=" + pageArray +
            ", folder=" + folder +
            ", id=" + id +
            ", max=" + max +
            ", min=" + min +
            ", nomedia=" + galleryDataFile +
            '}'
    }

    class PageFileIterator(private val files: SparseArrayCompat<PageFile>) :
        MutableIterator<PageFile> {
        private var reach = 0
        override fun hasNext(): Boolean {
            return reach < files.size()
        }

        override fun next(): PageFile {
            val f = files.valueAt(reach)
            reach++
            return f
        }

        override fun remove() {
            // HAHA
        }
    }

    companion object {
        @JvmField
        val CREATOR: Creator<GalleryFolder> = object : Creator<GalleryFolder> {
            override fun createFromParcel(`in`: Parcel): GalleryFolder {
                return GalleryFolder(`in`)
            }

            override fun newArray(size: Int): Array<GalleryFolder?> {
                return arrayOfNulls(size)
            }
        }
        private val FILE_PATTERN =
            Pattern.compile("^0*(\\d{1,9})\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE)
        private val ID_FILE_PATTERN = Pattern.compile("^\\.(\\d{1,6})$")
        private const val NOMEDIA_FILE = ".nomedia"

        fun fromId(context: Context?, id: Int): GalleryFolder? {
            val f = findGalleryFolder(context, id) ?: return null
            return GalleryFolder(f)
        }
    }
}
