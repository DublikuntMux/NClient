package com.dublikunt.nclient.components.photoview;

import android.view.MotionEvent;

public interface OnSingleFlingListener {

    boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
}
