package com.example.dreamtale.indoornavigator.PaintLayer;

import com.example.dreamtale.indoornavigator.SensorLayer.Utils.Euler;
import com.threed.jpct.Object3D;

import org.opencv.core.Point3;

/**
 * Created by DreamTale on 2017/2/9.
 */

public class MyStatePoint{
    public long timeStamp = 0;                     // the time stamp is the point's id
    public int imageIndex = 0;
    public Point3 coordinate = new Point3();       // contains x, y, z as the geometry position
    public Euler pose = new Euler();               // roll, pitch, yaw

    public MyBodyFrame obs = null;

    public enum StateSort {
        STATE_CURRENT,
        STATE_NORMAL,
        STATE_PICTURE,
        STATE_UPSTAIRS,
        STATE_ELEVATOR
    }                                   // The state sorts
    public StateSort stateSort = StateSort.STATE_NORMAL;
    public String imagePath = "";     // If at this place have been take a picture
    public boolean isSelected = false; // if it's true, highlight this point
    // TODO: 2017/2/9 Bind with picture and geometry functions
}
