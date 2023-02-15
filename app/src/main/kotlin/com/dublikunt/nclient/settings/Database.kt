package com.dublikunt.nclient.settings

import android.database.sqlite.SQLiteDatabase
import com.dublikunt.nclient.async.database.Queries
import com.dublikunt.nclient.utility.LogUtility.download

object Database {
    @JvmStatic
    var database: SQLiteDatabase? = null
        set(database) {
            field = database
            download("SETTED database$database")
            if (database != null) {
                setDBForTables(database)
            }
            Queries.StatusTable.initStatuses()
        }

    private fun setDBForTables(database: SQLiteDatabase) {
        Queries.db = database
    }
}
