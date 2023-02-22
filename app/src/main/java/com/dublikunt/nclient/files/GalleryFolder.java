package com.dublikunt.nclient.files;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;

import com.dublikunt.nclient.api.components.Page;
import com.dublikunt.nclient.enums.ImageExt;
import com.dublikunt.nclient.enums.SpecialTagIds;
import com.dublikunt.nclient.settings.Global;

import java.io.File;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryFolder implements Parcelable, Iterable<PageFile> {

    public static final Creator<GalleryFolder> CREATOR = new Creator<GalleryFolder>() {
        /**
         * Creates a new instance of GalleryFolder from the specified Parcel. This is used to create an instance of the gallery folder in the system when it is requested to do so.
         *
         * @param in - The Parcel to be used to create the gallery folder.
         *
         * @return The newly created gallery folder or null if the folder could not be created due to insufficient permissions to create
         */
        @Override
        public GalleryFolder createFromParcel(Parcel in) {
            return new GalleryFolder(in);
        }

        /**
         * Creates a new array of GalleryFolder. The size of the array must be greater than or equal to the number of gallery folders in this gallery manager.
         *
         * @param size - the size of the new array to be returned
         */
        @Override
        public GalleryFolder[] newArray(int size) {
            return new GalleryFolder[size];
        }
    };
    private static final Pattern FILE_PATTERN = Pattern.compile("^0*(\\d{1,9})\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDFILE_PATTERN = Pattern.compile("^\\.(\\d{1,6})$");
    private static final String NOMEDIA_FILE = ".nomedia";
    private final SparseArrayCompat<PageFile> pageArray = new SparseArrayCompat<>();
    private final File folder;
    private int id = SpecialTagIds.INVALID_ID;
    private int max = -1;
    private int min = Integer.MAX_VALUE;
    private File nomedia;

    public GalleryFolder(@NonNull String child) {
        this(Global.getDOWNLOADFOLDER(), child);
    }

    public GalleryFolder(@Nullable File parent, @NonNull String child) {
        this(new File(parent, child));

    }

    public GalleryFolder(File file) {
        folder = file;
        // throw an IllegalArgumentException if the file is not a folder
        if (!folder.isDirectory())
            throw new IllegalArgumentException("File is not a folder");
        parseFiles();
    }


    protected GalleryFolder(Parcel in) {
        folder = new File(Objects.requireNonNull(in.readString()));
        id = in.readInt();
        min = in.readInt();
        max = in.readInt();
        int pageCount = in.readInt();
        // Reads a page of pages from the input stream.
        for (int i = 0; i < pageCount; i++) {
            int k = in.readInt();
            PageFile f = in.readParcelable(PageFile.class.getClassLoader());
            pageArray.put(k, f);
        }
    }

    /**
     * Returns a GalleryFolder with the given id. This method is useful when you want to know what kind of folder is in the system and don't want to create a new instance of the gallery folder for you.
     *
     * @param context - The context to use. Can be null if not needed.
     * @param id      - The id of the folder to look up.
     * @return The gallery folder or null if not found or invalid id passed in ( in which case null is returned
     */
    public static @Nullable
    GalleryFolder fromId(@Nullable Context context, int id) {
        File f = Global.findGalleryFolder(context, id);
        // Returns the FITS file or null if the file is null.
        if (f == null) return null;
        return new GalleryFolder(f);
    }

    /**
     * Parses all files in the folder and elaborates them. This is called by parse () when the folder is changed
     */
    private void parseFiles() {
        File[] files = folder.listFiles();
        // Returns true if the files are not null.
        if (files == null) return;
        for (File f : files) {
            elaborateFile(f);
        }
    }

    /**
     * Elaborates a file. This is called by #load ( java. io. File ) when it encounters a file that should be elaborated.
     *
     * @param f - the file to elaborate ( never null
     */
    private void elaborateFile(File f) {
        String name = f.getName();

        Matcher matcher = FILE_PATTERN.matcher(name);
        // If the matcher matches the page and elaborate the page.
        if (matcher.matches()) elaboratePage(f, matcher);

        // If the id is invalid this method will set the id to the special tag id if it is not set.
        if (id == SpecialTagIds.INVALID_ID) {
            matcher = IDFILE_PATTERN.matcher(name);
            // If the matcher matches the matcher and set the id to the id of the element.
            if (matcher.matches()) id = elaborateId(matcher);
        }

        // Set nomedia file if not null.
        if (nomedia == null && name.equals(NOMEDIA_FILE)) nomedia = f;
    }

    /**
     * Elaborate id from regex. This is used to handle identifiers that are too long to fit in 32 bits
     *
     * @param matcher - Matcher to extract id
     */
    private int elaborateId(Matcher matcher) {
        return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
    }

    /**
     * Returns the number of pages in the page array. This is a constant value and should be avoided if performance is critical
     */
    public int getPageCount() {
        return pageArray.size();
    }

    /**
     * Returns the file that will be used to store gallery data. Note that this is a copy of nomedia and does not have to be persisted in the gallery.
     *
     * @return File that will be used to store gallery data or null if none exists for the gallery type ( such as image
     */
    public File getGalleryDataFile() {
        return nomedia;
    }

    /**
     * Returns an array of PageFile objects that this PageManager knows about. This is useful for debugging and to avoid memory leaks
     */
    public PageFile[] getPages() {
        PageFile[] files = new PageFile[pageArray.size()];
        // Fills files in the files array.
        for (int i = 0; i < pageArray.size(); i++) {
            files[i] = pageArray.valueAt(i);
        }
        return files;
    }

    /**
     * Returns the folder that this file is in. This is used to determine where the files are stored in the file system.
     *
     * @return File object representing the folder that this file is in or null if there is no folder in the file system
     */
    public File getFolder() {
        return folder;
    }

    /**
     * Returns the maximum value of the constraint. This is a constant value and may change between implementations depending on the type of constraint
     */
    public int getMax() {
        return max;
    }

    /**
     * Returns the minimum value of the interval. This is a getter so it can be used as a getter
     */
    public int getMin() {
        return min;
    }

    /**
     * Creates a PageFile for the page and appends it to #pageArray. Does nothing if the page is out of range
     *
     * @param f       - the file to load the page from
     * @param matcher - the matcher used to parse the page number and
     */
    private void elaboratePage(File f, Matcher matcher) {
        int page = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        ImageExt ext = Page.charToExt(Objects.requireNonNull(matcher.group(2)).charAt(0));
        // Returns the extension of the file.
        if (ext == null) return;
        pageArray.append(page, new PageFile(ext, f, page));
        // Set the maximum page to the current page.
        if (page > max) max = page;
        // Set the minimum page to be used for the search.
        if (page < min) min = page;
    }

    /**
     * Returns the page file associated with the given page number. This may be different from the page number returned by getPageCount ( int ) in that it does not take into account the order of the pages in the file list.
     *
     * @param page - the page number to look up. Must be greater than zero.
     * @return the page file or null if there is no page file for the given page number ( or if the page number is out of range
     */
    public PageFile getPage(int page) {
        return pageArray.get(page);
    }

    /**
     * Returns the id of this object. This is used to distinguish objects created by the class from objects created by the object factory
     */
    public int getId() {
        return id;
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this PageSet to a Parcel. The data will be written as follows : Folder name Integer Integer #min Integer #max Integer #pageArray List
     *
     * @param dest  - The Parcel to which the object is to be written.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(folder.getAbsolutePath());
        dest.writeInt(id);
        dest.writeInt(min);
        dest.writeInt(max);
        dest.writeInt(pageArray.size());
        // Write the page array to the output stream.
        for (int i = 0; i < pageArray.size(); i++) {
            dest.writeInt(pageArray.keyAt(i));
            dest.writeParcelable(pageArray.valueAt(i), flags);
        }
    }

    /**
     * Returns an iterator over the PageFile objects in this PageFileArray. The iterator is backed by the #pageArray
     */
    @NonNull
    @Override
    public Iterator<PageFile> iterator() {
        return new PageFileIterator(pageArray);
    }

    /**
     * Compares this object with the specified object for equality. Returns true if they are equal and false otherwise.
     *
     * @param o - The object to compare with this object for equality
     */
    @Override
    public boolean equals(Object o) {
        // Returns true if this object is the same as the receiver.
        if (this == o) return true;
        // Returns true if this object is a subclass of the same class.
        if (o == null || getClass() != o.getClass()) return false;

        GalleryFolder pageFiles = (GalleryFolder) o;

        return folder.equals(pageFiles.folder);
    }

    /**
     * Overridden to return the folder's hash code. This is used for sorting and comparison to avoid collisions
     */
    @Override
    public int hashCode() {
        return folder.hashCode();
    }

    /**
     * Returns a string representation of this object. This is used for debugging purposes and to verify that the object is valid.
     *
     * @return a string representation of this object ( never null ). Note that this will be a copy of the object
     */
    @NonNull
    @Override
    public String toString() {
        return "GalleryFolder{" +
            "pageArray=" + pageArray +
            ", folder=" + folder +
            ", id=" + id +
            ", max=" + max +
            ", min=" + min +
            ", nomedia=" + nomedia +
            '}';
    }

    public static class PageFileIterator implements Iterator<PageFile> {
        private final SparseArrayCompat<PageFile> files;
        private int reach = 0;

        public PageFileIterator(SparseArrayCompat<PageFile> files) {
            this.files = files;
        }


        /**
         * Returns true if there are more files to be read from the file list. This is the case if we have a file that is less than the number of files
         */
        @Override
        public boolean hasNext() {
            return reach < files.size();
        }

        /**
         * Returns the next file in the iteration. Note that it is up to the caller to check if there are more files to return.
         *
         * @return the next file in the iteration or null if there are no more files to return ( in which case the iteration is halted
         */
        @Override
        public PageFile next() {
            PageFile f = files.valueAt(reach);
            reach++;
            return f;
        }
    }
}
