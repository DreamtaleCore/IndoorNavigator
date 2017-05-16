package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by DreamTale on 2017/2/25.
 */

public class SensorFusion extends ExtendKF {

    /**
     * The construct of define a sensor fusion through EKF
     * @param nObs  the number of observations
     * @param mMes  this number of measurements
     */
    public SensorFusion(int nObs, int mMes) {
        super(nObs, mMes);
        assert n == 3 && m == 6;
        // We approximate the process noise using a small constant
        Mat q = new Mat();
        Core.multiply(Mat.eye(n, n, CvType.CV_64FC1), new Scalar(0.001), q);
        this.setQ(q);

        // Same for measurement
        Mat r = new Mat();
        Core.multiply(Mat.eye(m, m, CvType.CV_64FC1), new Scalar(0.001), r);
        this.setR(r);
    }

    @Override
    protected void model(Mat fx, Mat F, Mat hx, Mat H) {
        // Process model is f(x) = x
        this.getX().copyTo(fx);

        // So process model Jacobian is identity matrix
        Mat.eye(n, n, CvType.CV_64FC1).clone().copyTo(F);

        // Measurement function
        List<Mat> fxList = new LinkedList<>();
        fxList.add(fx);     fxList.add(fx);
        Core.vconcat(fxList, hx);

        // Jacobian of measurement function
        List<Mat> fList = new LinkedList<>();
        fList.add(F);     fList.add(F);
        Core.vconcat(fList, H);
    }
}
