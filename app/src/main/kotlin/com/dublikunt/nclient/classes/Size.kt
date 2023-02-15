package com.dublikunt.nclient.classes

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

open class Size : Parcelable {
    var width: Int
    var height: Int

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    protected constructor(`in`: Parcel) {
        width = `in`.readInt()
        height = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(width)
        dest.writeInt(height)
    }

    override fun toString(): String {
        return "Size{" +
            "width=" + width +
            ", height=" + height +
            '}'
    }

    companion object CREATOR : Creator<Size> {
        override fun createFromParcel(parcel: Parcel): Size {
            return Size(parcel)
        }

        override fun newArray(size: Int): Array<Size?> {
            return arrayOfNulls(size)
        }
    }
}
