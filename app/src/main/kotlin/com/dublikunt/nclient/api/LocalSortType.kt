package com.dublikunt.nclient.api

class LocalSortType {
    val type: Type

    @JvmField
    val descending: Boolean

    constructor(type: Type, ascending: Boolean) {
        this.type = type
        descending = ascending
    }

    constructor(hash: Int) {
        type = Type.values()[(hash and MASK_TYPE.toInt()) % Type.values().size]
        descending = hash and MASK_DESCENDING.toInt() != 0
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as LocalSortType
        return type == that.type && descending == that.descending
    }

    override fun hashCode(): Int {
        var hash = type.ordinal
        if (descending) hash = hash or MASK_DESCENDING.toInt()
        return hash
    }

    override fun toString(): String {
        return "LocalSortType{" +
            "type=" + type +
            ", descending=" + descending +
            ", hash=" + hashCode() +
            '}'
    }

    enum class Type {
        TITLE, DATE, PAGE_COUNT, RANDOM
    }

    companion object {
        const val MASK_DESCENDING = (1 shl 7).toByte() //10000000
        private const val MASK_TYPE = (MASK_DESCENDING - 1).toByte() //01111111
    }
}
