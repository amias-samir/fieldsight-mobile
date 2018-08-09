package org.bcss.collect.naxa.notification;

import android.support.v7.util.DiffUtil;

import java.util.ArrayList;
import java.util.List;

public class FieldSightNotificationsDiffCallback extends DiffUtil.Callback {
    private final List<FieldSightNotification> newList;
    private final ArrayList<FieldSightNotification> oldList;

    public FieldSightNotificationsDiffCallback(List<FieldSightNotification> newList, ArrayList<FieldSightNotification> oldList) {
        this.newList = newList;
        this.oldList = oldList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        String oldId = oldList.get(oldItemPosition).getId();
        String newId = newList.get(newItemPosition).getId();
        return oldId.equals(newId);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
