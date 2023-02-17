package com.dublikunt.nclient.enums

enum class ImageExt(name: String) {
    JPG("jpg"), PNG("png"), GIF("gif");

    val firstLetter: Char = name[0]
}
