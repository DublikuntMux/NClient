package com.dublikunt.nclient.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.api.components.Gallery;
import com.dublikunt.nclient.api.components.GalleryData;
import com.dublikunt.nclient.api.components.GenericGallery;
import com.dublikunt.nclient.api.components.Page;
import com.dublikunt.nclient.api.components.Tag;
import com.dublikunt.nclient.api.components.TagList;
import com.dublikunt.nclient.async.database.Queries;
import com.dublikunt.nclient.classes.Size;
import com.dublikunt.nclient.enums.ImageExt;
import com.dublikunt.nclient.enums.Language;
import com.dublikunt.nclient.enums.TagStatus;
import com.dublikunt.nclient.files.GalleryFolder;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.utility.LogUtility;
import com.dublikunt.nclient.utility.Utility;

import org.jsoup.nodes.Element;

import java.util.Locale;

public class SimpleGallery extends GenericGallery {
    public static final Creator<SimpleGallery> CREATOR = new Creator<SimpleGallery>() {
        /**
         * Creates a SimpleGallery from Parcel. This is used to create an instance of the Gallery class and can be overridden by subclasses that need to create their own instances
         *
         * @param in - The Parcel to create the gallery from
         *
         * @return The newly created SimpleGallery or null if there was an error creating the gallery ( in which case the error will be logged
         */
        @Override
        public SimpleGallery createFromParcel(Parcel in) {
            return new SimpleGallery(in);
        }

        /**
         * Creates a new array of SimpleGallery. Note that it is up to the implementer to make sure the array is large enough to hold the requested number of SimpleGalleries
         *
         * @param size - the number of SimpleGalleries to
         */
        @Override
        public SimpleGallery[] newArray(int size) {
            return new SimpleGallery[size];
        }
    };
    private final String title;
    private final ImageExt thumbnail;
    private final int id, mediaId;
    private Language language = Language.UNKNOWN;
    private TagList tags;

    public SimpleGallery(Parcel in) {
        title = in.readString();
        id = in.readInt();
        mediaId = in.readInt();
        thumbnail = ImageExt.values()[in.readByte()];
        language = Language.values()[in.readByte()];
    }

    @SuppressLint("Range")
    public SimpleGallery(Cursor c) {
        title = c.getString(c.getColumnIndex(Queries.HistoryTable.TITLE));
        id = c.getInt(c.getColumnIndex(Queries.HistoryTable.ID));
        mediaId = c.getInt(c.getColumnIndex(Queries.HistoryTable.MEDIAID));
        thumbnail = ImageExt.values()[c.getInt(c.getColumnIndex(Queries.HistoryTable.THUMB))];
    }

    public SimpleGallery(Context context, Element e) {
        String temp;
        String tags = e.attr("data-tags").replace(' ', ',');
        this.tags = Queries.TagTable.getTagsFromListOfInt(tags);
        language = Gallery.loadLanguage(this.tags);
        Element a = e.getElementsByTag("a").first();
        temp = a.attr("href");
        id = Integer.parseInt(temp.substring(3, temp.length() - 1));
        a = e.getElementsByTag("img").first();
        temp = a.hasAttr("data-src") ? a.attr("data-src") : a.attr("src");
        mediaId = Integer.parseInt(temp.substring(temp.indexOf("galleries") + 10, temp.lastIndexOf('/')));
        thumbnail = Page.charToExt(temp.charAt(temp.length() - 3));
        title = e.getElementsByTag("div").first().text();
        // Update the maximum id for the context.
        if (context != null && id > Global.getMaxId()) Global.updateMaxId(context, id);
    }

    public SimpleGallery(Gallery gallery) {
        title = gallery.getTitle();
        mediaId = gallery.getMediaId();
        id = gallery.getId();
        thumbnail = gallery.getThumb();
    }

    /**
     * Converts ImageExt to string. This is used to determine the format of the image to be displayed in JPG and PNG files.
     *
     * @param ext - the extension to convert. Must be one of the constants in ImageExt.
     * @return the string representation of the extension or null if not recognised by this extension type ( such as GIF PNG JPG
     */
    private static String extToString(ImageExt ext) {
        // Returns the extension of the image.
        switch (ext) {
            case GIF:
                return "gif";
            case PNG:
                return "png";
            case JPG:
                return "jpg";
        }
        return null;
    }

    /**
     * Returns the language associated with this message. This method is called by Jabber to get the language associated with this message.
     *
     * @return the language associated with this message or null if there is no language associated with this message or if the message does not have a
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Checks if there are tags that are ignored. This is used to prevent tags from appearing in the file when we want to download a file
     *
     * @param s - string to look for
     */
    public boolean hasIgnoredTags(String s) {
        // Returns true if tags is null.
        if (tags == null) return false;
        for (Tag t : tags.getAllTagsList())
            // Check if the tag is an AVOIDED tag.
            if (s.contains(t.toQueryTag(TagStatus.AVOIDED))) {
                LogUtility.download("Found: " + s + ",," + t.toQueryTag());
                return true;
            }
        return false;
    }

    /**
     * Returns the id of this object. This is used to distinguish objects that have been created by the client
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * Returns the type of this property. This will be Type#SIMPLE if the property is an array of Property instances.
     *
     * @return the type of this property as determined by #getType (). The default implementation returns Type#SIMPLE
     */
    @Override
    public Type getType() {
        return Type.SIMPLE;
    }

    /**
     * Returns the number of pages in this view. This is a constant value that can be used to determine how many pages are available
     */
    @Override
    public int getPageCount() {
        return 0;
    }

    /**
     * Checks if the id is greater than 0. This is used to prevent duplicate messages from being sent to the
     */
    @Override
    public boolean isValid() {
        return id > 0;
    }

    /**
     * Returns the title of the activity. Note that this will be null if the activity is an instance of Activity#isPlayable () playable or if there is no title associated with the activity.
     *
     * @return the title of the activity or null if the activity is an instance of Activity#isPlayable ()
     */
    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    /**
     * Returns the maximum size of the view. Note that this is not the size of the data that is stored in the view.
     *
     * @return the maximum size of the view or null if there is no limit on the size of the data that is stored in
     */
    @Override
    public Size getMaxSize() {
        return null;
    }

    /**
     * Returns the minimum size of the view. Note that this is not the size of the content but the minimum size of the content itself.
     *
     * @return the minimum size of the view or null if there is no minimum size to return from this method or it is a
     */
    @Override
    public Size getMinSize() {
        return null;
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the object to a Parcel. This will be written to a Parcel in the format used by the write method of com. alfresco. xacml. parcel. Parcel#writeToParcel ( com. alfresco. xacml. parcel. Parcel int )
     *
     * @param dest  - The Parcel to which the object is to be written.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeInt(id);
        dest.writeInt(mediaId);
        dest.writeByte((byte) thumbnail.ordinal());
        dest.writeByte((byte) language.ordinal());
        //TAGS AREN'T WRITTEN
    }

    /**
     * Gets the URI of the thumbnail. Note that this is not the same as the URI returned by #getMedia () but it includes the host and port in the URI.
     *
     * @return The URI of the thumbnail or null if there is no thumbnail to be used for this media id ( in which case the host is set to the host of this MediaProvider
     */
    public Uri getThumbnail() {
        // Returns the URL of the image.
        if (thumbnail == ImageExt.GIF) {
            return Uri.parse(String.format(Locale.US, "https://i." + Utility.getHost() + "/galleries/%d/1.gif", mediaId));
        }
        return Uri.parse(String.format(Locale.US, "https://t." + Utility.getHost() + "/galleries/%d/thumb.%s", mediaId, extToString(thumbnail)));
    }

    /**
     * Returns the media id associated with this Media. This is used to distinguish between different media types and their associated ID
     */
    public int getMediaId() {
        return mediaId;
    }

    /**
     * Returns the thumbnail associated with this Image. If there is no thumbnail this method returns null. Note that this method is public for unit testing and should not be called by user code.
     *
     * @return the thumbnail associated with this Image or null if there is no thumbnail associated with this Image or if there is
     */
    public ImageExt getThumb() {
        return thumbnail;
    }

    /**
     * Returns the GalleryFolder associated with this user. Note that this does not mean that the user has permission to view the gallery in which case it returns null.
     *
     * @return The GalleryFolder associated with this user or null if none can be found in the user's gallery
     */
    @Override
    public GalleryFolder getGalleryFolder() {
        return null;
    }

    /**
     * Returns a string representation of this SimpleGallery. The string representation is suitable for use in debugging and to verify that the values are sensible.
     *
     * @return a string representation of this SimpleGallery for debugging and to verify that the values are sensible ( and valid )
     */
    @NonNull
    @Override
    public String toString() {
        return "SimpleGallery{" +
            "language=" + language +
            ", title='" + title + '\'' +
            ", thumbnail=" + thumbnail +
            ", id=" + id +
            ", mediaId=" + mediaId +
            '}';
    }

    /**
     * Returns true if there is gallery data false otherwise. This is used to determine if we should show the gallery
     */
    @Override
    public boolean hasGalleryData() {
        return false;
    }

    /**
     * Returns the GalleryData associated with this Media. This can be null if there is no data associated with this Media.
     *
     * @return The GalleryData associated with this Media or null if there is no data associated with this Media or if there is no
     */
    @Override
    public GalleryData getGalleryData() {
        return null;
    }
}
