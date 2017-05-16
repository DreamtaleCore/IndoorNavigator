package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.Point3;

import java.util.Vector;

/**
 * Created by DreamTale on 2017/3/19.
 */

public abstract class BaseFilter {
    protected int windowSize = 9;
    protected Point3 mResult = new Point3();
    protected Vector<Point3> mDataList = new Vector<>();
    protected boolean mIsNeedCalibrate = false;

    public boolean process(Point3 result) {
        boolean ret = process();
        result = mResult;
        return ret;
    }
    abstract public boolean process();

    public void addData(Point3 data) {
        mDataList.add(data);
    }

    public BaseFilter(){
        // DO nothing here
    }

    public BaseFilter(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public Point3 getResult() {
        return mResult;
    }

    public boolean isNeedCalibrate() {
        return mIsNeedCalibrate;
    }
}
