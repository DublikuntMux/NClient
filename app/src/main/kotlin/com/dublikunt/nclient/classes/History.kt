package com.dublikunt.nclient.classes

import java.util.Date

class History(value: String, set: Boolean) {
    val value: String
    private var date: Date

    init {
        if (set) {
            val p = value.indexOf('|')
            date = Date(value.substring(0, p).toLong())
            this.value = value.substring(p + 1)
        } else {
            this.value = value
            date = Date()
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val history = o as History
        return value == history.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object {
        fun setToList(set: Set<String>): List<History> {
            val h: MutableList<History> = ArrayList(set.size)
            for (s in set) h.add(History(s, true))
            h.sortWith { o2: History, o1: History ->
                var o = o1.date.compareTo(o2.date)
                if (o == 0) o = o1.value.compareTo(o2.value)
                o
            }
            return h
        }

        fun listToSet(list: List<History>): Set<String> {
            val s = HashSet<String>(list.size)
            for (h in list) s.add(h.date.time.toString() + "|" + h.value)
            return s
        }
    }
}
