package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by DreamTale on 2017/3/11.
 */

public class FilterDispatcher {
    private Euler result;
    SensorFusion sensorFusion = new SensorFusion(3, 6);

    /**
     * process the filter step by step
     * @param src1  the first type of the sensor data in Euler
     * @param src2  the second type of the sensor data in Euler
     * @return  success or not
     */
    public boolean step(Euler src1, Euler src2) {
        // EKF here second
        double[] obs = {
                src1.roll, src1.pitch, src1.yaw,
                src2.roll, src2.pitch, src2.yaw
        };

//        for (int i = 0; i < 6; i ++) {
//            System.out.println("obs[i] = " + obs[i]);
//        }

        Mat measure = new Mat(6, 1, CvType.CV_64FC1);
        measure.put(0, 0, obs);

        if(sensorFusion.step(measure) == true) {
            double[] rlt_raw = new double[3];
            Mat rlt = sensorFusion.getX();
            System.out.println("rlt.dump() = " + rlt.dump());
            rlt.get(0, 0, rlt_raw);
            result = new Euler(
                    rlt_raw[0],
                    rlt_raw[1],
                    rlt_raw[2]
                    );
            return true;
        } else {
            return false;
        }
    }

    public Euler getResult() {
        return result;
    }
}
