package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.Point3;

/**
 * Created by DreamTale on 2017/3/25.
 */

public class ComplementaryFilter {
    private Point3 resultPoint = new Point3();
    private Quaternion resultQ = new Quaternion();
    private Euler resultEuler = new Euler();
    private double leftRate = 0.98;

    public boolean step(Quaternion src1, Quaternion src2) {

        resultQ = src1.slerp(src2, 1 - leftRate);

        return true;
    }

    public boolean step(Point3 src1, Point3 src2) {
        double rightRate = 1 - leftRate;

        resultPoint.x = src1.x * leftRate + src2.x * rightRate;
        resultPoint.y = src1.y * leftRate + src2.y * rightRate;
        resultPoint.z = src1.z * leftRate + src2.z * rightRate;

        return true;
    }

    public boolean step(Euler src1, Euler src2) {
        double rightRate = 1 - leftRate;

        resultEuler.roll  = src1.roll  * leftRate + src2.roll   * rightRate;
        resultEuler.pitch = src1.pitch * leftRate + src2.pitch  * rightRate;
        resultEuler.yaw   = src1.yaw   * leftRate + src2.yaw    * rightRate;

        return true;
    }

    public Point3 getResultPoint() {
        return resultPoint;
    }

    public Quaternion getResultQ() {
        return resultQ;
    }

    public Euler getResultEuler() {
        return resultEuler;
    }

    public void setLeftRate(double leftRate) {
        this.leftRate = leftRate;
    }
}
