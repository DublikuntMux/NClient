package com.dublikunt.nclient.components.status

import android.graphics.Color

class Status internal constructor(color: Int, name: String) {

    val color: Int


    val name: String

    init {
        this.color = Color.argb(0x7f, Color.red(color), Color.green(color), Color.blue(color))
        this.name = name
    }

    fun opaqueColor(): Int {
        return color or -0x1000000
    }
}
