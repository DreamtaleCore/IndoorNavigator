package com.example.dreamtale.indoornavigator.PaintLayer;

import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

import org.opencv.core.Point;

/**
 * Created by DreamTale on 2017/5/6.
 */

public class MyBodyFrame {

    private Object3D mFrame;
    private float mXLen = 100f;
    private float mYLen = 100f;
    private float mZLen = 100f;
    private RGBColor mXColor = new RGBColor(0, 255, 255);
    private RGBColor mYColor = new RGBColor(255, 255, 0);
    private RGBColor mZColor = new RGBColor(255, 0, 255);
    private Polyline mXLine, mYLine, mZLine;
    private Object3D mParent;
    private World mWorld;

    public MyBodyFrame(World world, Object3D parent, float xLen, float yLen, float zLen) {
        mWorld = world;
        mParent = parent;
        mXLen = xLen;
        mYLen = yLen;
        mZLen = zLen;
    }

    public MyBodyFrame(World world, Object3D parent, float len) {
        mWorld = world;
        mParent = parent;
        mXLen = mYLen = mZLen = len;
    }

    public void setLineColor(RGBColor x, RGBColor y, RGBColor z) {
        mXColor = x;
        mYColor = y;
        mZColor = z;
    }

    public void init() {

        SimpleVector[] xLinePoints = new SimpleVector[2];
        SimpleVector[] yLinePoints = new SimpleVector[2];
        SimpleVector[] zLinePoints = new SimpleVector[2];
        xLinePoints[0] = yLinePoints[0] = zLinePoints[0] = new SimpleVector(0, 0, 0);
        xLinePoints[1] = new SimpleVector(mXLen, 0, 0);
        yLinePoints[1] = new SimpleVector(0, mYLen, 0);
        zLinePoints[1] = new SimpleVector(0, 0, -mZLen);
        mXLine = new Polyline(xLinePoints, mXColor);
        mYLine = new Polyline(yLinePoints, mYColor);
        mZLine = new Polyline(zLinePoints, mZColor);
        float minLen = mXLen < mYLen ? mXLen < mZLen ? mXLen : mZLen : mYLen < mZLen ? mYLen : mZLen;

        mFrame = Primitives.getCube(minLen / 2.5f);

        mXLine.setWidth(minLen / 10f);
        mYLine.setWidth(minLen / 10f);
        mZLine.setWidth(minLen / 10f);
        mXLine.setParent(mFrame);
        mYLine.setParent(mFrame);
        mZLine.setParent(mFrame);
        mXLine.setVisible(true);
        mYLine.setVisible(true);
        mZLine.setVisible(true);
        mWorld.addPolyline(mXLine);
        mWorld.addPolyline(mYLine);
        mWorld.addPolyline(mZLine);
    }

    public void setAdditionalColor(RGBColor color) {
        mFrame.setAdditionalColor(color);
    }

    public void setOrigin(SimpleVector origin) {
        mFrame.setOrigin(origin);
    }

    public void setTransparency(int transparency) {
        mFrame.setTransparency(transparency);
    }

    public void rotateZ(float z) {
        mFrame.rotateZ(z);
    }

    public void rotateY(float y) {
        mFrame.rotateY(y);
    }

    public void rotateX(float x) {
        mFrame.rotateX(x);
    }

    public void setCenter(SimpleVector center) {
        mFrame.setCenter(center);
    }

    public void strip() {
        mFrame.strip();
    }

    public void build() {
        mFrame.build();
    }

    public void setCollisionMode(int mode) {
        mFrame.setCollisionMode(mode);
    }

    public int getID() {
        return mFrame.getID();
    }

    public void setScale(float scale) {
        mFrame.setScale(scale);
    }

    public void generate() {
        mParent.addChild(mFrame);
        mWorld.addObject(mFrame);
    }
}
