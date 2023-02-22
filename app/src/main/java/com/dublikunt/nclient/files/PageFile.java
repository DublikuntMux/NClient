package com.dublikunt.nclient.files;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.dublikunt.nclient.api.components.Page;
import com.dublikunt.nclient.enums.ImageExt;
import com.dublikunt.nclient.settings.Global;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageFile extends File implements Parcelable {
    public static final Creator<PageFile> CREATOR = new Creator<PageFile>() {
        /**
         * Creates a PageFile from a Parcel. This is used to create an instance of the page file in the XML - RPC protocol.
         *
         * @param in - The Parcel that contains the content of the page file.
         *
         * @return The newly created PageFile instance or null if the content cannot be parsed or is not a valid XML - RPC page
         */
        @Override
        public PageFile createFromParcel(Parcel in) {
            return new PageFile(in);
        }

        /**
         * Creates a new array of PageFile. The size of the array is specified in the constructor. This method is called by the page manager when it wants to create a new array of the specified size
         *
         * @param size - the size of the
         */
        @Override
        public PageFile[] newArray(int size) {
            return new PageFile[size];
        }
    };
    private static final Pattern DEFAULT_THUMBNAIL = Pattern.compile("^0*1\\.(gif|png|jpg)$", Pattern.CASE_INSENSITIVE);
    private final ImageExt ext;
    private final int page;

    public PageFile(ImageExt ext, File file, int page) {
        super(file.getAbsolutePath());
        this.ext = ext;
        this.page = page;
    }

    protected PageFile(Parcel in) {
        super(in.readString());
        page = in.readInt();
        ext = ImageExt.values()[in.readByte()];
    }

    /**
     * Returns a PageFile that is an image with a fast thumbnail. This is used to avoid downloading the thumbnail twice in a row and the first time we're downloading it.
     *
     * @param folder - The folder to search. Must be a directory.
     * @return A PageFile or null if there is no thumbnail in the folder or the file doesn't exist
     */
    private static @Nullable
    PageFile fastThumbnail(File folder) {
        for (ImageExt ext : ImageExt.values()) {
            String name = "001." + ext.name();
            File file = new File(folder, name);
            // Returns a PageFile object for the file.
            if (file.exists()) return new PageFile(ext, file, 1);
        }
        return null;
    }

    /**
     * Gets the thumbnail for the given page. This is a fast way to get thumbnails that are in the gallery folder or a fallback to the most appropriate thumbnail.
     *
     * @param context - The context to use. Must not be null.
     * @param id      - The id of the page to get the thumbnail for. Must not be null.
     * @return The thumbnail or null if none could be found or it could not be determined for some reason such as if the thumbnail is not in the gallery folder
     */
    public static @Nullable
    PageFile getThumbnail(Context context, int id) {
        File file = Global.findGalleryFolder(context, id);
        // Returns the file or null if the file is null.
        if (file == null) return null;
        PageFile pageFile = fastThumbnail(file);
        // Returns the page file that contains the page.
        if (pageFile != null) return pageFile;
        File[] files = file.listFiles();
        // Returns the list of files that are currently in the list.
        if (files == null) return null;
        for (File f : files) {
            Matcher m = DEFAULT_THUMBNAIL.matcher(f.getName());
            // Skip the match if any of the matches.
            if (!m.matches()) continue;
            ImageExt ext = Page.charToExt(Objects.requireNonNull(m.group(1)).charAt(0));
            return new PageFile(ext, f, 1);
        }
        return null;
    }

    /**
     * Converts this to a URI. This is equivalent to { @link Uri#fromFile ( String ) } except that the URI is converted to a URI before being returned.
     *
     * @return this as a URI or null if this is not a valid URI ( for example if it's an HTTP or HTTPS URL
     */
    public Uri toUri() {
        return Uri.fromFile(this);
    }

    /**
     * Returns the ImageExt that this Image is using. Note that this is a copy of the ext property in order to avoid copying the data from a different thread.
     *
     * @return the ImageExt that this Image is using or null if none is set ( for example if it is an unlimited size
     */
    public ImageExt getExt() {
        return ext;
    }

    /**
     * Returns the page that this wizard is on. This is an integer between 1 and #getPageSize ()
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the object to a Parcel. This will be called by the Parcelable interface. The flags parameter is ignored and may be set to 0.
     *
     * @param dest  - The parcel to which the object is to be written.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.getAbsolutePath());
        dest.writeInt(page);
        dest.writeByte((byte) ext.ordinal());
    }


}
