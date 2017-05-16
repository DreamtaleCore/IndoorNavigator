package com.example.dreamtale.indoornavigator.SensorLayer.Camera;

import android.util.Size;

/**
 * Created by DreamTale on 2017/2/3.
 */
public class CompareSizesByArea implements java.util.Comparator<Size> {

    @Override
    public int compare(Size o1, Size o2) {
        return Long.signum((long)o1.getWidth() * o1.getHeight()
        - (long) o2.getWidth() * o2.getHeight());
    }
}
