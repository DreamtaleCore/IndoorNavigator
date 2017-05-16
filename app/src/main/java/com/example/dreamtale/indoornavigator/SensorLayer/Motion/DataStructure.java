package com.example.dreamtale.indoornavigator.SensorLayer.Motion;

/**
 * Created by DreamTale on 2017/2/26.
 */

public class DataStructure {
    public static final int DATA_TYPE_MAG = 0x01;
    public static final int DATA_TYPE_GYR = 0x02;
    public static final int DATA_TYPE_ACC = 0x04;
    public static final int DATA_TYPE_LIN = 0x08;

    public long timestamp;
    public double[] data = new double[3];
    public int type;
    public DataStructure() {
        this.data[0] = this.data[1] = this.data[2] = 0f;
    }
}
