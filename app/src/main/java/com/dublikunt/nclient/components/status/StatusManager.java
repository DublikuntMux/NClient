package com.dublikunt.nclient.components.status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dublikunt.nclient.async.database.Queries;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class StatusManager {
    public static final String DEFAULT_STATUS = "None";
    private static final HashMap<String, Status> statusMap = new HashMap<>();

    public static Status getByName(String name) {
        return statusMap.get(name);
    }

    @NonNull
    @Contract("_, _ -> new")
    public static Status add(String name, int color) {
        return add(new Status(color, name));
    }

    @NonNull
    @Contract("_ -> param1")
    static Status add(Status status) {
        Queries.StatusTable.insert(status);
        statusMap.put(status.name, status);
        return status;
    }

    public static void remove(@NonNull Status status) {
        Queries.StatusTable.remove(status.name);
        statusMap.remove(status.name);
    }

    @NonNull
    public static List<String> getNames() {
        List<String> st = new ArrayList<>(statusMap.keySet());
        Collections.sort(st, String::compareToIgnoreCase);
        st.remove(DEFAULT_STATUS);
        return st;
    }

    @NonNull
    public static List<Status> toList() {
        ArrayList<Status> statuses = new ArrayList<>(statusMap.values());
        Collections.sort(statuses, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        statuses.remove(getByName(DEFAULT_STATUS));
        return statuses;
    }

    @NonNull
    public static Status updateStatus(@Nullable Status oldStatus, String newName, int newColor) {
        if (oldStatus == null)
            return add(newName, newColor);
        Status newStatus = new Status(newColor, newName);
        Queries.StatusTable.update(oldStatus, newStatus);
        statusMap.remove(oldStatus.name);
        statusMap.put(newStatus.name, newStatus);
        return newStatus;
    }
}
