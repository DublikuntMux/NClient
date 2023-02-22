package com.dublikunt.nclient.api.components;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StringRes;

import com.dublikunt.nclient.R;

public class Ranges implements Parcelable {


    public static final int UNDEFINED = -1;
    public static final TimeUnit UNDEFINED_DATE = null;
    public static final Creator<Ranges> CREATOR = new Creator<Ranges>() {
        /**
         * Creates Ranges from Parcel. This is used to create ranges that are part of a range expression.
         *
         * @param in - The Parcel to parse. Must not be null.
         *
         * @return The newly created Ranges or null if there was an error parsing the Parcel. Note that the default implementation does not throw an exception
         */
        @Override
        public Ranges createFromParcel(Parcel in) {
            return new Ranges(in);
        }

        /**
         * Creates a new array of Ranges. The size of the array is specified in the argument. If you want to change the size use #newArray ( int )
         *
         * @param size - the size of the
         */
        @Override
        public Ranges[] newArray(int size) {
            return new Ranges[size];
        }
    };
    private int fromPage = UNDEFINED, toPage = UNDEFINED;
    private int fromDate = UNDEFINED, toDate = UNDEFINED;
    private TimeUnit fromDateUnit = UNDEFINED_DATE, toDateUnit = UNDEFINED_DATE;

    public Ranges() {
    }

    protected Ranges(Parcel in) {
        int date;
        fromPage = in.readInt();
        toPage = in.readInt();
        fromDate = in.readInt();
        toDate = in.readInt();
        date = in.readInt();
        fromDateUnit = date == -1 ? UNDEFINED_DATE : TimeUnit.values()[date];
        date = in.readInt();
        toDateUnit = date == -1 ? UNDEFINED_DATE : TimeUnit.values()[date];
    }

    /**
     * Returns true if this object is the default for the query. This is used to determine if a query should be executed
     */
    public boolean isDefault() {
        return fromDate == UNDEFINED && toDate == UNDEFINED
            && toPage == UNDEFINED && fromPage == UNDEFINED;
    }

    /**
     * Returns the number of the page the user is from. This is used to determine where the user will be on the next
     */
    public int getFromPage() {
        return fromPage;
    }

    /**
     * Sets the page number that this page is on. This is used to display the first page of results when there are more than one page in the result set
     *
     * @param fromPage - The page number that this page is
     */
    public void setFromPage(int fromPage) {
        this.fromPage = fromPage;
    }

    /**
     * Returns the number of the to page. This is used to determine where to go in the list of
     */
    public int getToPage() {
        return toPage;
    }

    /**
     * Sets the to page. This is used to display the page that the user clicked on when clicking on the link
     *
     * @param toPage - The page to display
     */
    public void setToPage(int toPage) {
        this.toPage = toPage;
    }

    /**
     * Returns the from date of the message. This is used to determine if the message is from a date
     */
    public int getFromDate() {
        return fromDate;
    }

    /**
     * Sets the from date. This is used to filter the results based on the time they were sent to the API
     *
     * @param fromDate - The from date to
     */
    public void setFromDate(int fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Returns the date the event occurred on. This is an integer in the range 0 to 2147483647
     */
    public int getToDate() {
        return toDate;
    }

    /**
     * Sets the date to which this event applies. This is used to determine when the event should be sent to the user
     *
     * @param toDate - the date to which this event
     */
    public void setToDate(int toDate) {
        this.toDate = toDate;
    }

    /**
     * Returns the TimeUnit that this TimeInterval represents. This is equivalent to TimeUnit#getFromDateUnit () but more efficient.
     *
     * @return the TimeUnit that this TimeInterval represents or TimeUnit#SECONDS if none is specified in the constructor or null
     */
    public TimeUnit getFromDateUnit() {
        return fromDateUnit;
    }

    /**
     * Sets the from date unit. This is used to convert dates before they are sent to the database.
     *
     * @param fromDateUnit - the from date unit to convert dates
     */
    public void setFromDateUnit(TimeUnit fromDateUnit) {
        this.fromDateUnit = fromDateUnit;
    }

    /**
     * Returns the TimeUnit that this TimeSpan represents. This is equivalent to #getToDateUnit () except that it does not throw an exception if this TimeSpan represents a time before the end of the time period.
     *
     * @return the TimeUnit that this TimeSpan represents or TimeUnit#SECONDS if this is a time before the end of the time
     */
    public TimeUnit getToDateUnit() {
        return toDateUnit;
    }

    /**
     * Sets the TimeUnit to use when converting this Timestamp to a date. This is useful for converting time units that don't have a timezone such as UTC to local time.
     *
     * @param toDateUnit - the TimeUnit to use when converting this Timestamp to a date
     */
    public void setToDateUnit(TimeUnit toDateUnit) {
        this.toDateUnit = toDateUnit;
    }

    /**
     * Converts the parameters to a query string. This is used for debugging and to provide information about the results of the query.
     *
     * @return the query string to be used in the URL query ( without the querystring ) or null if there are no parameters
     */
    public String toQuery() {
        boolean pageCreated = false;
        StringBuilder builder = new StringBuilder();
        // Append to builder. append pages fromPage toPage if fromPage and toPage are equal.
        if (fromPage != UNDEFINED && toPage != UNDEFINED && fromPage == toPage) {
            builder.append("pages:").append(fromPage).append(' ');
        } else {
            // Append to builder builder. append fromPage.
            if (fromPage != UNDEFINED) builder.append("pages:>=").append(fromPage).append(' ');
            // Appends to the page to the page.
            if (toPage != UNDEFINED) builder.append("pages:<=").append(toPage).append(' ');
        }

        // Upload the uploaded date.
        if (fromDate != UNDEFINED && toDate != UNDEFINED && fromDate == toDate) {
            builder.append("uploaded:").append(fromDate).append(fromDateUnit.val);
        } else {
            // Upload the uploaded file to the builder.
            if (fromDate != UNDEFINED)
                builder.append("uploaded:>=").append(fromDate).append(fromDateUnit.val).append(' ');
            // Appends the uploaded date to builder.
            if (toDate != UNDEFINED)
                builder.append("uploaded:<=").append(toDate).append(toDateUnit.val);
        }
        return builder.toString().trim();
    }

    /**
     * Writes the object to a Parcel. This method is intended for use by applications that want to persist data in a transaction such as a database.
     *
     * @param dest  - The com. google. gwt. user. client. parcel. Parcel object.
     * @param flags - Additional flags about how the object should be written
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(fromPage);
        dest.writeInt(toPage);
        dest.writeInt(fromDate);
        dest.writeInt(toDate);
        dest.writeInt(fromDateUnit == UNDEFINED_DATE ? -1 : fromDateUnit.ordinal());
        dest.writeInt(toDateUnit == UNDEFINED_DATE ? -1 : toDateUnit.ordinal());
    }

    /**
     * Returns 0 for no contents 1 for content and 2 for content. This is used to determine whether or not a file is in use
     */
    @Override
    public int describeContents() {
        return 0;
    }

    public enum TimeUnit {
        HOUR(R.string.hours, 'h'),
        DAY(R.string.days, 'd'),
        WEEK(R.string.weeks, 'w'),
        MONTH(R.string.months, 'm'),
        YEAR(R.string.years, 'y');
        @StringRes
        final
        int string;
        final char val;

        TimeUnit(int string, char val) {
            this.string = string;
            this.val = val;
        }

        /**
         * Returns the string associated with this object. This is an integer that can be used to convert a value to a string
         */
        public int getString() {
            return string;
        }

        /**
         * Returns the value of this object. This is a constant so it can be used in place of get
         */
        public char getVal() {
            return val;
        }
    }

}
