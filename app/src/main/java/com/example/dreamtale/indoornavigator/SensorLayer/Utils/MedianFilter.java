package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by DreamTale on 2017/3/19.
 */

public class MedianFilter extends BaseFilter {
    @Override
    public boolean process() {
        mIsNeedCalibrate = false;
        if(mDataList.size() < windowSize) {
            return false;
        }

        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        List<Double> zs = new ArrayList<>();
        for (int i = 0; i < windowSize; i++) {
            xs.add(mDataList.elementAt(i).x);
            ys.add(mDataList.elementAt(i).y);
            zs.add(mDataList.elementAt(i).z);
        }

        Collections.sort(xs);
        Collections.sort(ys);
        Collections.sort(zs);

        mResult.x = xs.get(windowSize / 2);
        mResult.y = ys.get(windowSize / 2);
        mResult.z = zs.get(windowSize / 2);

        mDataList.remove(0);

        return true;
    }
}
