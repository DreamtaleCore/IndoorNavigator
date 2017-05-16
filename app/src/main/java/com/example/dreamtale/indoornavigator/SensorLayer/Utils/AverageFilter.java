package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.Point3;

/**
 * Created by DreamTale on 2017/3/8.
 */

public class AverageFilter extends BaseFilter {

    @Override
    public boolean process() {
        mIsNeedCalibrate = false;
        if(mDataList.size() < windowSize) {
            return false;
        }
        Point3 sum = new Point3(0d, 0d, 0d);
        for (Point3 elem : mDataList) {
            sum.x += elem.x;
            sum.y += elem.y;
            sum.z += elem.z;
        }
        // The queue's first element get out
        mResult.x = sum.x / (double)mDataList.size();
        mResult.y = sum.y / (double)mDataList.size();
        mResult.z = sum.z / (double)mDataList.size();

        // [ATTENTION] only used for calibrate
        // get the variance of list to judge weather need to calibrate the
        // gyroscope data accumulative error
        double variance = 0d;
        for (Point3 elem : mDataList) {
            variance += (elem.x - mResult.x) * (elem.x - mResult.x);
            variance += (elem.y - mResult.y) * (elem.y - mResult.y);
            variance += (elem.z - mResult.z) * (elem.z - mResult.z);
        }
        variance = variance / (double) mDataList.size() / 3.0d;
        // todo: 1.6e-5 is compute from test3/CmpSensorFusion(3).csv[line:325~333]
        if(variance < 1.6e-5) {
            mIsNeedCalibrate = true;
        }

        mDataList.remove(0);
        return true;
    }
}
