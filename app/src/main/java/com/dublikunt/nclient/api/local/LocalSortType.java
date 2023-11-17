package com.dublikunt.nclient.api.local;

import androidx.annotation.NonNull;

public class LocalSortType {
    public static final byte MASK_DESCENDING = (byte) (1 << 7);
    private static final byte MASK_TYPE = (byte) (MASK_DESCENDING - 1);
    @NonNull
    public final Type type;
    public final boolean descending;

    public LocalSortType(@NonNull Type type, boolean ascending) {
        this.type = type;
        this.descending = ascending;
    }

    public LocalSortType(int hash) {
        this.type = Type.values()[(hash & MASK_TYPE) % Type.values().length];
        this.descending = (hash & MASK_DESCENDING) != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalSortType that = (LocalSortType) o;

        return this.type == that.type && this.descending == that.descending;
    }

    @Override
    public int hashCode() {
        int hash = type.ordinal();
        if (descending) hash |= MASK_DESCENDING;
        return hash;
    }

    @NonNull
    @Override
    public String toString() {
        return "LocalSortType{" +
            "type=" + type +
            ", descending=" + descending +
            ", hash=" + hashCode() +
            '}';
    }

    public enum Type {TITLE, DATE, PAGE_COUNT, RANDOM}
}
