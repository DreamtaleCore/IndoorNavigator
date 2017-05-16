package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by DreamTale on 2017/2/19.
 */


public abstract class ExtendKF {

    /**
     * The structure of main Kalman Filter parameters
     */
    private class ekf_t {
        public int n;       // number of state values
        public int m;       // number of observables

        public Mat x;       // state vector     | n*1

        public Mat P;       // prediction  error covariance     | n*n
        public Mat Q;       // process noise covariance         | n*n
        public Mat R;       // measurement error covariance     | m*m

        public Mat G;       // Kalman gain; a.k.a K             | n*m

        public Mat F;       // Jacobian of process model        | n*n
        public Mat H;       // Jacobian of process model        | m*n

        public Mat Ht;      // transpose of measurement Jacobian| n*m
        public Mat Ft;      // transpose of process Jacobian    | n*n
        public Mat Pp;      // P, post-prediction, pre-update   | n*n

        public Mat fx;      // output of user defined f() state-transition function |n*1
        public Mat hx;      // output of user defined h() measurement function      |m*1

        /**
         * The construct of define a EKF
         * @param nObs  the number of observations
         * @param mMes  this number of measurements
         */
        public ekf_t(int nObs, int mMes) {
            n = nObs;
            m = mMes;
            init();
        }

        private void init() {
            x = Mat.zeros(n, 1, CvType.CV_64FC1);

            P = Mat.zeros(n, n, CvType.CV_64FC1);;
            Q = Mat.zeros(n, n, CvType.CV_64FC1);;
            R = Mat.zeros(m, m, CvType.CV_64FC1);;

            G = Mat.zeros(n, m, CvType.CV_64FC1);

            F = Mat.zeros(n, n, CvType.CV_64FC1);
            H = Mat.zeros(m, n, CvType.CV_64FC1);

            Ht = Mat.zeros(n, m, CvType.CV_64FC1);
            Ft = Mat.zeros(n, n, CvType.CV_64FC1);
            Pp = Mat.zeros(n, n, CvType.CV_64FC1);

            fx = Mat.zeros(n, 1, CvType.CV_64FC1);
            hx = Mat.zeros(m, 1, CvType.CV_64FC1);
        }
    }
    private ekf_t ekf;
    protected int n, m;

    public boolean step(Mat z) {

        this.model(ekf.fx, ekf.F, ekf.hx, ekf.H);
//        System.out.println("ekf.fx.dump() = " + ekf.fx.dump());
//        System.out.println("ekf.hx.dump() = " + ekf.hx.dump());
//        System.out.println("ekf.F.dump() = " + ekf.F.dump());
//        System.out.println("ekf.H.dump() = " + ekf.H.dump());

        // P_k = F_{k-1} P_{k-1}F^T_{k-1} + Q_{k-1}
        Mat tmp00 = new Mat();
        Core.multiply(ekf.F, ekf.P, tmp00);
        Mat tmp01 = new Mat();
        Core.multiply(tmp00, ekf.F.t(), tmp01);
        Core.add(tmp01, ekf.Q, ekf.Pp);
//        System.out.println("ekf.Pp = " + ekf.Pp.dump());

        // G_k = P_k H^T (H_k Pk H^T + R)^{-1}
        Mat tmp10 = new Mat();
        Core.gemm(ekf.Pp, ekf.H.t(), 1.0, new Mat(), 0, tmp10);
        Mat tmp11 = new Mat();
        Core.gemm(ekf.H, ekf.Pp, 1.0, new Mat(), 0, tmp11);
        Mat tmp12 = new Mat();
        Core.gemm(tmp11, ekf.H.t(), 1.0, new Mat(), 0, tmp12);
        Mat tmp13 = new Mat();
        Core.add(tmp12, ekf.R, tmp13);
        Core.gemm(tmp10, tmp13.inv(), 1.0, new Mat(), 0, ekf.G);
//        System.out.println("ekf.G = " + ekf.G.dump());

        // \hat{x}_k = \hat{x_k} + G_k(z_k - h(\hat{x}_k))
        Mat tmp20 = new Mat();
        Core.subtract(z, ekf.hx, tmp20);
        Mat tmp21 = new Mat();
        Core.gemm(ekf.G, tmp20, 1.0, new Mat(), 0, tmp21);
        Core.add(ekf.fx, tmp21, ekf.x);
//        System.out.println("ekf.x.dump() = " + ekf.x.dump());

        // P_k = (I - G_k H_k) P_k
        Mat tmp30 = new Mat();
        Core.gemm(ekf.G, ekf.H, 1.0, new Mat(), 0, tmp30);
        Mat tmp31 = new Mat();
        Core.subtract(Mat.eye(n, n, CvType.CV_64FC1), tmp30, tmp31);
        Core.gemm(tmp31, ekf.Pp, 1.0, new Mat(), 0, ekf.P);
//        System.out.println("ekf.P.dump() = " + ekf.P.dump());

        return true;   // success
    }

    protected Mat x;                // The current state.
    protected ExtendKF(int rows, int cols) {
        ekf = new ekf_t(rows, cols);
        n = ekf.n;
        m = ekf.m;
        this.x = ekf.x;
    }

    protected abstract void model(Mat fx, Mat F, Mat hx, Mat H);

    protected void setP(Mat value) {
        this.ekf.P = value;
    }

    protected void setQ(Mat value) {
        this.ekf.Q = value;
    }

    protected void setR(Mat value) {
        this.ekf.R = value;
    }

    public Mat getX() {
        return this.ekf.x;
    }

    public void setX(Mat value) {
        this.ekf.x = value;
    }

    public void dispose() {
        // TODO: 2017/3/11 Bug here
        // this.dispose();
    }
}
