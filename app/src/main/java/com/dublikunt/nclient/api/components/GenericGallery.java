package com.dublikunt.nclient.api.components;

import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.dublikunt.nclient.classes.Size;
import com.dublikunt.nclient.files.GalleryFolder;
import com.dublikunt.nclient.utility.Utility;

import java.util.Locale;

public abstract class GenericGallery implements Parcelable {

    /**
     * Returns the id of this object. This is used to distinguish objects that have been created by the client
     */
    public abstract int getId();

    /**
     * Returns the type of this property. This method is called when the property is added to a PropertyGroup.
     *
     * @return the type of this property or null if there is no type associated with this property ( for example if this property is a List
     */
    public abstract Type getType();

    /**
     * Returns the number of pages in this list. This is a constant value and should be used in situations where you don't care about the size of the list
     */
    public abstract int getPageCount();

    /**
     * Returns true if this instance is valid. This is used to determine if the user has a valid input
     */
    public abstract boolean isValid();

    /**
     * Returns the title of this view. Note that this method is used to determine the view's title when it is displayed in the list of viewers.
     *
     * @return the title of this view or null if there is no title to display for the view ( for example if the view is a ListView
     */
    @NonNull
    public abstract String getTitle();

    /**
     * Returns the maximum size of the view. Note that this is a property of the view and may change over time depending on the implementation of #getView ().
     *
     * @return the maximum size of the view or null if the view is unbounded ( in which case the view's size is set to Size#UNDEFINED
     */
    public abstract Size getMaxSize();

    /**
     * Returns the minimum size of the view. This is used to determine the width and height of the view when it is rendered to the user.
     *
     * @return the minimum size of the view or null if there is no minimum size to render to the user ( in this case the view will be laid out as empty
     */
    public abstract Size getMinSize();

    /**
     * Returns the GalleryFolder associated with this object. If there is no gallery folder associated with this object null is returned.
     *
     * @return The GalleryFolder associated with this object or null if there is no gallery folder associated with this object or if the user does not have permission to access the
     */
    public abstract GalleryFolder getGalleryFolder();

    /**
     * Returns the URL that should be used to share the page at the given index. This is based on the host and the page number plus 1.
     *
     * @param i - the index of the page to share. Must be greater than or equal to zero.
     * @return the URL that should be used to share the page at the given index in the page's list
     */
    public String sharePageUrl(int i) {
        return String.format(Locale.US, "https://" + Utility.getHost() + "/g/%d/%d/", getId(), i + 1);
    }

    /**
     * Returns true if this is a local variable. Local variables are variables in the scope of a class that is declared local
     */
    public boolean isLocal() {
        return getType() == Type.LOCAL;
    }

    /**
     * Returns true if this object has gallery data. This is used to determine if the gallery is in a way that can be reproducible
     */
    public abstract boolean hasGalleryData();

    /**
     * Returns the GalleryData associated with this object. This method is called by the gallery to get information about the gallery such as the number of images per image and how much data is available for each image.
     *
     * @return The GalleryData associated with this object or null if there is no data for the gallery or if the user doesn't have permission to view
     */
    public abstract GalleryData getGalleryData();

    public enum Type {COMPLETE, LOCAL, SIMPLE}

}
