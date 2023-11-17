package com.dublikunt.nclient.components.classes;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public class Size implements Parcelable {
    public static final Creator<Size> CREATOR = new Creator<Size>() {
        @NonNull
        @Contract("_ -> new")
        @Override
        public Size createFromParcel(Parcel in) {
            return new Size(in);
        }

        @NonNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public Size[] newArray(int size) {
            return new Size[size];
        }
    };
    private int width, height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected Size(@NonNull Parcel in) {
        width = in.readInt();
        height = in.readInt();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(width);
        dest.writeInt(height);
    }

    @Override
    public String toString() {
        return "Size{" +
            "width=" + width +
            ", height=" + height +
            '}';
    }
}
