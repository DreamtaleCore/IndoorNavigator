package com.example.dreamtale.indoornavigator.SensorLayer.Motion;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by DreamTale on 2017/2/26.
 */



public class DataSynchronizer {

    private Queue<DataStructure> accDataList = new LinkedList<>();
    private Queue<DataStructure> magDataList = new LinkedList<>();
    private Queue<DataStructure> gyrDataList = new LinkedList<>();
    private Queue<DataStructure> linDataList = new LinkedList<>();

    public void push(DataStructure mdData) {

        switch (mdData.type) {
            case DataStructure.DATA_TYPE_ACC:
                accDataList.add(mdData);
                break;
            case DataStructure.DATA_TYPE_MAG:
                magDataList.add(mdData);
                break;
            case DataStructure.DATA_TYPE_GYR:
                gyrDataList.add(mdData);
                break;
            case DataStructure.DATA_TYPE_LIN:
                linDataList.add(mdData);
                break;
        }
    }

    /**
     * synchronize the motion raw data
     * @param motionData  if returned successfully, this will be the synchronized data
     *  structure of motion data:
     * +--------------------+-----------------+------------------+------------------+----------+
     * |   magnitude data   | gyroscope data  |acceleration data |  liner-acc data  |time span |
     * +--------------------+-----------------+------------------+------------------+----------+
     * |   3 * double size  | 3 * double size | 3 * double size  | 3 * double size  |1 * double|
     * +--------------------+-----------------+------------------+------------------+----------+
     * @return success or not
     */
    public boolean synchronize(double[] motionData) {
        if(accDataList.size() > 0 &&
                magDataList.size() > 0 &&
                gyrDataList.size() > 0 &&
                linDataList.size() > 0 &&
                motionData.length == 13) {

            while (accDataList.size() > 0 &&
                    magDataList.size() > 0 &&
                    gyrDataList.size() > 0 &&
                    linDataList.size() > 0) {
                // step1: find the max timestamp of three type of sensors
                long currentTime = 0;
                currentTime = currentTime > accDataList.peek().timestamp ?
                        currentTime : accDataList.peek().timestamp;
                currentTime = currentTime > magDataList.peek().timestamp ?
                        currentTime : magDataList.peek().timestamp;
                currentTime = currentTime > gyrDataList.peek().timestamp ?
                        currentTime : gyrDataList.peek().timestamp;
                currentTime = currentTime > linDataList.peek().timestamp ?
                        currentTime : linDataList.peek().timestamp;
                // step2: dispose the time below current time
                boolean isFound = true;
                if(accDataList.peek().timestamp < currentTime) {
                    accDataList.poll();
                    isFound = false;
                }
                if(magDataList.peek().timestamp < currentTime) {
                    magDataList.poll();
                    isFound = false;
                }
                if(gyrDataList.peek().timestamp < currentTime) {
                    gyrDataList.poll();
                    isFound = false;
                }
                if(linDataList.peek().timestamp < currentTime) {
                    linDataList.poll();
                    isFound = false;
                }
                if(isFound) {
                    // package the data to motionData
                    motionData[0] = magDataList.peek().data[0];
                    motionData[1] = magDataList.peek().data[1];
                    motionData[2] = magDataList.peek().data[2];
                    motionData[3] = gyrDataList.peek().data[0];
                    motionData[4] = gyrDataList.peek().data[1];
                    motionData[5] = gyrDataList.peek().data[2];
                    motionData[6] = accDataList.peek().data[0];
                    motionData[7] = accDataList.peek().data[1];
                    motionData[8] = accDataList.peek().data[2];
                    motionData[9] =  linDataList.peek().data[0];
                    motionData[10] = linDataList.peek().data[1];
                    motionData[11] = linDataList.peek().data[2];
                    // timestamp only use the last 4 number in motion data
                    motionData[12] = currentTime;

                    // poll out all the sensors' list at once
                    accDataList.poll();
                    magDataList.poll();
                    gyrDataList.poll();
                    linDataList.poll();

//                    System.out.println("accDataList.size() = " + accDataList.size());
//                    System.out.println("magDataList.size() = " + magDataList.size());
//                    System.out.println("gyrDataList.size() = " + gyrDataList.size());

                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

}
