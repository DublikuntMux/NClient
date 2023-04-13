package com.dublikunt.nclient.api.comments

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.StringRes
import com.dublikunt.nclient.R

open class Ranges : Parcelable {
    var fromPage = UNDEFINED
    var toPage = UNDEFINED
    var fromDate = UNDEFINED
    var toDate = UNDEFINED
    var fromDateUnit = UNDEFINED_DATE
    var toDateUnit = UNDEFINED_DATE

    constructor()
    protected constructor(`in`: Parcel) {
        var date: Int
        fromPage = `in`.readInt()
        toPage = `in`.readInt()
        fromDate = `in`.readInt()
        toDate = `in`.readInt()
        date = `in`.readInt()
        fromDateUnit = if (date == -1) UNDEFINED_DATE else TimeUnit.values()[date]
        date = `in`.readInt()
        toDateUnit = if (date == -1) UNDEFINED_DATE else TimeUnit.values()[date]
    }

    val isDefault: Boolean
        get() = fromDate == UNDEFINED && toDate == UNDEFINED && toPage == UNDEFINED && fromPage == UNDEFINED

    fun toQuery(): String {
        val pageCreated = false
        val builder = StringBuilder()
        if (fromPage != UNDEFINED && toPage != UNDEFINED && fromPage == toPage) {
            builder.append("pages:").append(fromPage).append(' ')
        } else {
            if (fromPage != UNDEFINED) builder.append("pages:>=").append(fromPage).append(' ')
            if (toPage != UNDEFINED) builder.append("pages:<=").append(toPage).append(' ')
        }
        if (fromDate != UNDEFINED && toDate != UNDEFINED && fromDate == toDate) {
            builder.append("uploaded:").append(fromDate).append(fromDateUnit!!.`val`)
        } else {
            if (fromDate != UNDEFINED) builder.append("uploaded:>=").append(fromDate).append(
                fromDateUnit!!.`val`
            ).append(' ')
            if (toDate != UNDEFINED) builder.append("uploaded:<=").append(toDate).append(
                toDateUnit!!.`val`
            )
        }
        return builder.toString().trim { it <= ' ' }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(fromPage)
        dest.writeInt(toPage)
        dest.writeInt(fromDate)
        dest.writeInt(toDate)
        dest.writeInt(if (fromDateUnit == UNDEFINED_DATE) -1 else fromDateUnit!!.ordinal)
        dest.writeInt(if (toDateUnit == UNDEFINED_DATE) -1 else toDateUnit!!.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    enum class TimeUnit(@field:StringRes val string: Int, val `val`: Char) {
        HOUR(R.string.hours, 'h'), DAY(R.string.days, 'd'), WEEK(
            R.string.weeks,
            'w'
        ),
        MONTH(R.string.months, 'm'), YEAR(R.string.years, 'y');

    }

    companion object {
        const val UNDEFINED = -1
        val UNDEFINED_DATE: TimeUnit? = null

        @JvmField
        val CREATOR: Creator<Ranges> = object : Creator<Ranges> {
            override fun createFromParcel(`in`: Parcel): Ranges {
                return Ranges(`in`)
            }

            override fun newArray(size: Int): Array<Ranges?> {
                return arrayOfNulls(size)
            }
        }
    }
}
