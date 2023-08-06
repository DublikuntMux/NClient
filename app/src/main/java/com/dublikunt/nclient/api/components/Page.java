package com.dublikunt.nclient.api.components;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dublikunt.nclient.api.enums.ImageExt;
import com.dublikunt.nclient.api.enums.ImageType;
import com.dublikunt.nclient.components.classes.Size;

import org.jetbrains.annotations.Contract;

import java.io.IOException;

public class Page implements Parcelable {
    public static final Creator<Page> CREATOR = new Creator<Page>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
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

    public Page(ImageType type, @NonNull JsonReader reader, int page) throws IOException {
        this.imageType = type;
        this.page = page;
        reader.beginObject();
        while (reader.peek() != JsonToken.END_OBJECT) {
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

    protected Page(@NonNull Parcel in) {
        page = in.readInt();
        size = in.readParcelable(Size.class.getClassLoader());
        imageExt = ImageExt.values()[in.readByte()];
        imageType = ImageType.values()[in.readByte()];
    }

    private static ImageExt stringToExt(@NonNull String ext) {
        return charToExt(ext.charAt(0));
    }

    @Nullable
    @Contract(pure = true)
    public static String extToString(@NonNull ImageExt ext) {
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

    public static char extToChar(@NonNull ImageExt imageExt) {
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

    @Nullable
    @Contract(pure = true)
    public static ImageExt charToExt(int ext) {
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

    public String extToString() {
        return extToString(imageExt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(page);
        dest.writeParcelable(size, flags);
        dest.writeByte((byte) (imageExt == null ? ImageExt.JPG.ordinal() : imageExt.ordinal()));
        dest.writeByte((byte) (imageType == null ? ImageType.PAGE.ordinal() : imageType.ordinal()));
    }

    public int getPage() {
        return page;
    }

    public ImageExt getImageExt() {
        return imageExt;
    }

    public char getImageExtChar() {
        return extToChar(imageExt);
    }

    public ImageType getImageType() {
        return imageType;
    }

    public Size getSize() {
        return size;
    }

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
