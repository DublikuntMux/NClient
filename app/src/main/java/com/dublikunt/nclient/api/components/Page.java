package com.dublikunt.nclient.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import com.dublikunt.nclient.classes.Size;
import com.dublikunt.nclient.enums.ImageExt;
import com.dublikunt.nclient.enums.ImageType;

import java.io.IOException;

public class Page implements Parcelable {
    public static final Creator<Page> CREATOR = new Creator<Page>() {
        /**
         * Creates a Parcel from a Parcel. This is used to create an instance of Page in the XML - RPC framework
         *
         * @param in - The Parcel to be converted
         *
         * @return The newly created Page or null if there was an error creating the Parcel ( for example if the input was invalid
         */
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        /**
         * Creates a new array of the specified size. Note that the size of the array is specified in terms of the number of pages that this object holds.
         *
         * @param size - the size of the new array to be created
         */
        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };
    private final int page;
    private final ImageType imageType;
    private ImageExt imageExt;
    private Size size = new Size(0, 0);

    Page() {
        this.imageType = ImageType.PAGE;
        this.imageExt = ImageExt.JPG;
        this.page = 0;
    }

    public Page(ImageType type, JsonReader reader) throws IOException {
        this(type, reader, 0);
    }

    public Page(ImageType type, ImageExt ext) {
        this(type, ext, 0);
    }

    public Page(ImageType type, ImageExt ext, int page) {
        this.imageType = type;
        this.imageExt = ext;
        this.page = page;
    }

    public Page(ImageType type, JsonReader reader, int page) throws IOException {
        this.imageType = type;
        this.page = page;
        reader.beginObject();
        // Reads the size of the image.
        while (reader.peek() != JsonToken.END_OBJECT) {
            // Reads the next value from the reader.
            switch (reader.nextName()) {
                case "t":
                    imageExt = stringToExt(reader.nextString());
                    break;
                case "w":
                    size.setWidth(reader.nextInt());
                    break;
                case "h":
                    size.setHeight(reader.nextInt());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    protected Page(Parcel in) {
        page = in.readInt();
        size = in.readParcelable(Size.class.getClassLoader());
        imageExt = ImageExt.values()[in.readByte()];
        imageType = ImageType.values()[in.readByte()];
    }

    /**
     * Converts a string to an ImageExt. This is used to convert the extension of an image to a file extension when it is known that the extension is valid.
     *
     * @param ext - The extension as a string. Must be non - null
     * @return The image extension corresponding to
     */
    private static ImageExt stringToExt(String ext) {
        return charToExt(ext.charAt(0));
    }

    /**
     * Converts an ImageExt to a String. This is used to determine the type of image that should be displayed to the user.
     *
     * @param ext - the extention of the image to convert.
     * @return the string representation of the image type or null if not recognised by the ImageExt. For example " jpg
     */
    public static String extToString(ImageExt ext) {
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
     * Converts ImageExt to char. Used to create Image file names from image extension. This is a helper method for #getImage ( Image )
     *
     * @param imageExt - image extension to convert
     */
    public static char extToChar(ImageExt imageExt) {
        // Returns the image extension of the image.
        switch (imageExt) {
            case GIF:
                return 'g';
            case PNG:
                return 'p';
            case JPG:
                return 'j';
        }
        return '\0';
    }

    /**
     * Converts a character extention to an ImageExt. This is used to convert an image extension from the characters g p j in a JPG file to the corresponding image extension.
     *
     * @param ext - the char that corresponds to an image extension.
     * @return the corresponding ImageExt or null if not recognised or the char doesn't correspond to an image extension
     */
    public static ImageExt charToExt(int ext) {
        // Returns the image extension type.
        switch (ext) {
            case 'g':
                return ImageExt.GIF;
            case 'p':
                return ImageExt.PNG;
            case 'j':
                return ImageExt.JPG;
        }
        return null;
    }

    /**
     * Returns a string representation of the image extension. This is equivalent to Image#getExt () except that the result is formatted as a hexadecimal string rather than as a byte array.
     *
     * @return the string representation of the image extension or null if the extension is not known or does not correspond to a valid
     */
    public String extToString() {
        return extToString(imageExt);
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the image to a Parcel. This is used for serializing the image to disk such as when loading a zip file.
     *
     * @param dest  - The Parcel to which the image is to be written.
     * @param flags - Additional flags about how the image is written. See #writeToParcelable ( Parcel int
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(page);
        dest.writeParcelable(size, flags);
        dest.writeByte((byte) (imageExt == null ? ImageExt.JPG.ordinal() : imageExt.ordinal()));
        dest.writeByte((byte) (imageType == null ? ImageType.PAGE.ordinal() : imageType.ordinal()));
    }

    /**
     * Returns the page that this wizard is on. This is an integer between 1 and #getPageSize ()
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the ImageExt associated with this object. This is a convenience method that can be used to get the ImageExt associated with the image being displayed on the JTextPane.
     *
     * @return the ImageExt associated with this object or null if none is associated with the image being displayed on the JText
     */
    public ImageExt getImageExt() {
        return imageExt;
    }

    /**
     * Returns the image extension as a character. This is equivalent to #getImageExt ( int ) but returns a char
     */
    public char getImageExtChar() {
        return extToChar(imageExt);
    }

    /**
     * Gets the image type. This is used to determine whether or not a thumbnail should be used. If the thumbnail is an image it will be used as the thumbnail for the GIF.
     *
     * @return The image type of
     */
    public ImageType getImageType() {
        return imageType;
    }

    /**
     * Returns the size of the data. This is a view of the data as it is stored in the data store.
     *
     * @return the size of the data in this DataStore's data store ( not null ). Note that the size may be different from the data store
     */
    public Size getSize() {
        return size;
    }

    /**
     * Returns a string representation of this Page. This is used for debugging purposes and to show the page and image information in a human - readable format.
     *
     * @return a string representation of this Page including page and image information in a human - readable format or null if there is no
     */
    @Override
    public String toString() {
        return "Page{" +
            "page=" + page +
            ", imageExt=" + imageExt +
            ", imageType=" + imageType +
            ", size=" + size +
            '}';
    }
}
