package com.dublikunt.nclientv2.components.status

import com.dublikunt.nclientv2.async.database.Queries

object StatusManager {
    const val DEFAULT_STATUS = "None"
    private val statusMap = HashMap<String, Status>()
    @JvmStatic
    fun getByName(name: String): Status? {
        return statusMap[name]
    }

    @JvmStatic
    fun add(name: String, color: Int): Status {
        return add(Status(color, name))
    }

    @JvmStatic
    fun add(status: Status): Status {
        Queries.StatusTable.insert(status)
        statusMap[status.name] = status
        return status
    }

    @JvmStatic
    fun remove(status: Status) {
        Queries.StatusTable.remove(status.name)
        statusMap.remove(status.name)
    }

    val names: List<String>
        get() {
            val st: MutableList<String> = java.util.ArrayList(statusMap.keys)
            st.sortWith{ obj: String, str: String ->
                obj.compareTo(str, true)
            }
            st.remove(DEFAULT_STATUS)
            return st
        }

    fun toList(): List<Status> {
        val statuses = ArrayList(statusMap.values)
        statuses.sortWith{ o1: Status, o2: Status ->
            o1.name.compareTo(o2.name, true)
        }
        statuses.remove(getByName(DEFAULT_STATUS))
        return statuses
    }

    fun updateStatus(oldStatus: Status?, newName: String, newColor: Int): Status {
        if (oldStatus == null) return add(newName, newColor)
        val newStatus = Status(newColor, newName)
        Queries.StatusTable.update(oldStatus, newStatus)
        statusMap.remove(oldStatus.name)
        statusMap[newStatus.name] = newStatus
        return newStatus
    }
}
