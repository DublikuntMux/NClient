package com.dublikunt.nclient.enums

class ApiRequestType private constructor(id: Int, single: Boolean) {
    private val id: Byte
    val isSingle: Boolean

    init {
        this.id = id.toByte()
        isSingle = single
    }

    fun ordinal(): Byte {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ApiRequestType
        return id == that.id
    }

    override fun hashCode(): Int {
        return id.toInt()
    }

    companion object {
        val BYALL = ApiRequestType(0, false)
        val BYTAG = ApiRequestType(1, false)
        val BYSEARCH = ApiRequestType(2, false)
        val BYSINGLE = ApiRequestType(3, true)
        val RELATED = ApiRequestType(4, false)
        val FAVORITE = ApiRequestType(5, false)
        val RANDOM = ApiRequestType(6, true)
        val RANDOM_FAVORITE = ApiRequestType(7, true)
        val values = arrayOf(
            BYALL, BYTAG, BYSEARCH, BYSINGLE, RELATED, FAVORITE, RANDOM, RANDOM_FAVORITE
        )
    }
}
