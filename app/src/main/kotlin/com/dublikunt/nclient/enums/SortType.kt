package com.dublikunt.nclient.enums

import com.dublikunt.nclient.R

enum class SortType(val nameId: Int, val urlAddition: String?) {
    RECENT_ALL_TIME(R.string.sort_recent, null), POPULAR_ALL_TIME(
        R.string.sort_popular_all_time,
        "popular"
    ),
    POPULAR_WEEKLY(
        R.string.sort_popular_week,
        "popular-week"
    ),
    POPULAR_DAILY(
        R.string.sort_popular_day,
        "popular-today"
    ),
    POPULAR_MONTH(R.string.sort_popoular_month, "popular-month");

    companion object {
        fun findFromAddition(addition: String?): SortType {
            if (addition == null) return RECENT_ALL_TIME
            for (t in values()) {
                val url = t.urlAddition
                if (url != null && addition.contains(url)) {
                    return t
                }
            }
            return RECENT_ALL_TIME
        }
    }
}
