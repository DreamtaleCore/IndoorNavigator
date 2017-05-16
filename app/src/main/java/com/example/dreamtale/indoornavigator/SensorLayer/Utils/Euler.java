package com.example.dreamtale.indoornavigator.SensorLayer.Utils;

/**
 * Created by DreamTale on 2017/3/11.
 */

public class Euler {
    public double roll;
    public double pitch;
    public double yaw;

    public Euler(double roll, double pitch, double yaw) {
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public Euler() {
        this.roll = this.pitch = this.yaw = 0d;
    }

    public double[] toRotationMatrix() {
        return euler2RotationMatrix(this);
    }

    public static Euler rotationMatrix2Euler(double[][] matrix) {
        assert matrix.length == 4 * 4;

        double azimuth = Math.atan2(matrix[0][1], matrix[1][1]);
        double pitch = Math.asin(-matrix[2][1]);
        double roll = Math.atan2(-matrix[2][0], matrix[2][2]);
        return new Euler(roll, pitch, azimuth);
    }

    public static double[] euler2RotationMatrix(Euler e) {

        double s_x = Math.sin(e.roll);
        double c_x = Math.cos(e.roll);

        double s_y = Math.sin(e.pitch);
        double c_y = Math.cos(e.pitch);

        double s_z = Math.sin(e.yaw);
        double c_z = Math.cos(e.yaw);

        double[] ret = new double[4*4];
        ret[0 + 0] = c_x * s_y * c_z - s_x * s_z;
        ret[0 + 1] = s_x * s_y * c_z - c_x * s_z;
        ret[0 + 2] = c_y * c_z;
        ret[0 + 3] = 0;

        ret[4 + 0] = c_x * s_y * s_z - s_x * c_z;
        ret[4 + 1] = s_x * s_y * s_z + c_x * c_z;
        ret[4 + 2] = c_y * s_z;
        ret[4 + 3] = 0;

        ret[8 + 0] = c_x * c_y;
        ret[8 + 1] = s_x * c_y;
        ret[8 + 2] = -s_y;
        ret[8 + 3] = 0;

        ret[12 + 0] = 0;
        ret[12 + 1] = 0;
        ret[12 + 2] = 0;
        ret[12 + 3] = 1;

        return ret;
    }

    public String dump() {
        String ret = "roll = ";
        ret += String.valueOf(roll) + "\r\n";
        ret += "pitch = ";
        ret += String.valueOf(pitch) + "\r\n";
        ret += "yaw = ";
        ret += String.valueOf(yaw) + "\r\n";

        return ret;
    }
}
