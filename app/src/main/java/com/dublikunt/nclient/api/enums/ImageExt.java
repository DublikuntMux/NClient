package com.dublikunt.nclient.api.enums;

import androidx.annotation.NonNull;

public enum ImageExt {
    JPG("jpg"), PNG("png"), GIF("gif");

    private final char firstLetter;
    private final String name;

    ImageExt(@NonNull String name) {
        this.name = name;
        this.firstLetter = name.charAt(0);
    }

    public char getFirstLetter() {
        return firstLetter;
    }

    public String getName() {
        return name;
    }
}
