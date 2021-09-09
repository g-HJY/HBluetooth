package com.hjy.test;

import android.util.SparseArray;
import android.view.View;

/**
 * Created by Administrator on 2018/10/24.
 */

public class ViewHolder {

    public static <T extends View> T getView(View convertView, int childViewId) {
        SparseArray<View> viewHolder = (SparseArray<View>) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new SparseArray<>();
            convertView.setTag(viewHolder);
        }

        View childView = viewHolder.get(childViewId);
        if (childView == null) {
            childView = convertView.findViewById(childViewId);
            viewHolder.put(childViewId, childView);
        }

        return (T) childView;
    }
}
