package com.dublikunt.nclient.api.enums

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
        @JvmField
        val BYALL = ApiRequestType(0, false)

        @JvmField
        val BYTAG = ApiRequestType(1, false)

        @JvmField
        val BYSEARCH = ApiRequestType(2, false)

        @JvmField
        val BYSINGLE = ApiRequestType(3, true)
        val RELATED = ApiRequestType(4, false)

        @JvmField
        val FAVORITE = ApiRequestType(5, false)

        @JvmField
        val RANDOM = ApiRequestType(6, true)

        @JvmField
        val RANDOM_FAVORITE = ApiRequestType(7, true)

        @JvmField
        val values = arrayOf(
            BYALL, BYTAG, BYSEARCH, BYSINGLE, RELATED, FAVORITE, RANDOM, RANDOM_FAVORITE
        )
    }
}
