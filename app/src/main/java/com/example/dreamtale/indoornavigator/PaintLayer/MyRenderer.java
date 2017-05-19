package com.example.dreamtale.indoornavigator.PaintLayer;

import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;

import com.example.dreamtale.indoornavigator.MainActivity;
import com.example.dreamtale.indoornavigator.R;
import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

import org.opencv.core.Point3;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by DreamTale on 2017/2/12.
 */

public class MyRenderer implements GLSurfaceView.Renderer {

    // <editor-fold desc="private parameters BLOCK">

    public final static int FOR_GALLERY_SIGNAL = 0x521c;
    private World mWorld = null;
    private Light mSun = null;
    private Light mSunBack = null;
    private FrameBuffer mFrameBuffer = null;
    private RGBColor mBackgroundColor = new RGBColor(10, 50, 100);
    private Camera mCamera = null;
    private Object3D mCreator = null;   // All 3d-object are in mCreator
                                        // and can be transform & rotate & zoom with it
    private Object3D mCoordinate = null;
    private Polyline mZAxis;
    private long mCurrentTime = System.currentTimeMillis();
    private int mFps = 0;

    private float mHistoryPathX = 0f;
    private float mHistoryPathY = 0f;
    private float mHistoryPathZ = 0f;
    Vector<MyStatePoint> mStatePoints = new Vector<>();
    // </editor-fold>

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mFrameBuffer != null) {
            mFrameBuffer.dispose();
        }
        mFrameBuffer = new FrameBuffer(gl, width, height);

        if (MainActivity.mActivity == null) {
            mWorld = new World();
            mWorld.setAmbientLight(50, 50, 50);

            // Originally, the mCreator only contains a XY-plane & Z-axis
            mCreator = Primitives.getCube(400);
            mCreator.setAdditionalColor(20, 20, 20);
            mCreator.setTransparency(2);
            mCreator.strip();
            mCreator.build();

            // Step 1: add the XY-plane
            mCoordinate = Primitives.getPlane(1, 400);
            mCoordinate.setAdditionalColor(200, 200, 200);
            mCoordinate.setOrigin(new SimpleVector(0, 0, 0));
            mCoordinate.setTransparency(2);
            mCoordinate.setTexture("indoor map");
            mCoordinate.strip();
            mCoordinate.build();
            mCreator.addChild(mCoordinate);
            mWorld.addObject(mCoordinate);
            // Step 2: add the Z-axis
            SimpleVector[] linePoints = new SimpleVector[2];
            linePoints[0] = new SimpleVector(0, 0, 0);
            linePoints[1] = new SimpleVector(0, 0, -100);
            mZAxis = new Polyline(linePoints, new RGBColor(255, 200, 20));
            mZAxis.setWidth(2);
            mZAxis.setParent(mCreator);
            mCoordinate = new Object3D(mCreator);
            // Step 3: Add them to the world
            mWorld.addObject(mCreator);
            mWorld.addPolyline(mZAxis);

            mCamera = mWorld.getCamera();
            mCamera.moveCamera(Camera.CAMERA_MOVEOUT, 100);
//        mCamera.moveCamera(Camera.CAMERA_MOVEUP, -200);
            mCamera.lookAt(mCreator.getTransformedCenter());
            //mCamera.rotateCameraX(-45);

            mSun = new Light(mWorld);
            mSunBack = new Light(mWorld);
            mSun.setIntensity(200, 200, 200);
            mSunBack.setIntensity(50, 50, 50);
            SimpleVector sv = new SimpleVector();
            sv.set(mCreator.getTransformedCenter());
            sv.y -= 300;
            sv.z -= 300;
            mSun.setPosition(sv);
            sv.set(mCreator.getTransformedCenter());
            sv.y += 300;
            sv.z += 300;
            mSunBack.setPosition(sv);
            // Optimize the memory
            MemoryHelper.compact();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if(mMove.x != 0) {
            mCreator.translate((float) mMove.x, 0, 0);
            mMove.x = 0;
        }
        if(mMove.y != 0) {
            mCreator.translate(0, (float) mMove.y, 0);
            mMove.y = 0;
        }
        if(mMove.z != 0) {
            mCreator.translate(0, 0, (float) mMove.z);
            mMove.z = 0;
        }

        if(mRotate.x != 0) {
            mCreator.rotateX((float) mRotate.x);
            mRotate.x = 0;
        }
        if(mRotate.y != 0) {
            mCreator.rotateY((float) mRotate.y);
            mRotate.y = 0;
        }

        mFrameBuffer.clear(mBackgroundColor);
        mWorld.renderScene(mFrameBuffer);
        mWorld.draw(mFrameBuffer);
        mFrameBuffer.display();


        if(System.currentTimeMillis() - mCurrentTime >= 1000) {
            System.out.println("The fps:  " + mFps);
            mFps = 0;
            mCurrentTime = System.currentTimeMillis();
        }
        mFps++;
    }

    // <editor-fold desc="todo Interactions with upper classes BLOCK">
    private Point3 mRotate = new Point3(20f, 0f, 0f);
    private Point3 mMove = new Point3(0f, 0f, 0f);
    public void recovery() {
        // TODO: 2017/2/12 Make recovery
        // Optimize the memory
        MemoryHelper.compact();
    }

    public void rotateWorld(float x, float y, float z) {
        mRotate.x = -x;
        mRotate.y = -y;
        mRotate.z = z;
    }

    public void moveWorld(float x, float y, float z) {
        mMove.x = x;
        mMove.y = y;
        mMove.z = -z;
    }

    private int mDefaultTransparency = 8;
    private int mCurrentTransparency = 10;
    private int mLastStatePointIdx = -1;

    public RGBColor colorTable(MyStatePoint.StateSort sort) {
        RGBColor color = null;
        switch (sort) {
            case STATE_CURRENT:
                color = new RGBColor(255, 20, 20);
                break;
            case STATE_ELEVATOR:
                color = new RGBColor(255, 255, 20);
                break;
            case STATE_UPSTAIRS:
                color = new RGBColor(20, 255, 255);
                break;
            case STATE_NORMAL:
                color = new RGBColor(20, 255, 20);
                break;
            case STATE_PICTURE:
                color = new RGBColor(255, 20, 255);
                break;
            default:
                color = new RGBColor(150, 150, 150);
                break;
        }
        return color;
    }

    public void addNewStatePoint(MyStatePoint statePoint) {
        float x = (float) statePoint.coordinate.x;
        float y = (float) statePoint.coordinate.y;
        // In the coordinate of OpenGL, the z axis is point into the surface of screen
        float z = -(float) statePoint.coordinate.z;

        if (mCoordinate.getScale() < Math.sqrt(x * x + y * y + z * z)) {
            mCoordinate.setScale(mCoordinate.getScale() * 1.5f);
        }

        SimpleVector[] points = new SimpleVector[2];
        points[0] = new SimpleVector(mHistoryPathX, mHistoryPathY, mHistoryPathZ);
        points[1] = new SimpleVector(x, y, z);
        Polyline line = new Polyline(points, new RGBColor(255, 255, 255));
        line.setParent(mCreator);
        mWorld.addPolyline(line);


        if(mLastStatePointIdx >= 0) {
            MyStatePoint sp = mStatePoints.elementAt(mLastStatePointIdx);
            sp.obs.setAdditionalColor(colorTable(sp.stateSort));
            sp.obs.setTransparency(mDefaultTransparency);
        }

        statePoint.obs = new MyBodyFrame(mWorld, mCreator, 2f);
        statePoint.obs.init();
        statePoint.obs.setAdditionalColor(colorTable(MyStatePoint.StateSort.STATE_CURRENT));

        statePoint.obs.setOrigin(new SimpleVector(x, y, z));
        statePoint.obs.setTransparency(mDefaultTransparency);

        statePoint.obs.rotateZ((float) statePoint.pose.yaw);
        statePoint.obs.rotateY((float) statePoint.pose.pitch);
        statePoint.obs.rotateX((float) statePoint.pose.roll);

        statePoint.obs.setCenter(new SimpleVector(x, y, z));

        statePoint.obs.strip();
        statePoint.obs.build();
        statePoint.obs.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
        // mCreator.addChild(statePoint.obs);
        // mWorld.addObject(statePoint.obs);

        statePoint.obs.generate();

        mStatePoints.add(statePoint);
        mLastStatePointIdx = mStatePoints.size() - 1;

        mHistoryPathX = x;
        mHistoryPathY = y;
        mHistoryPathZ = z;
        // Optimize the memory
        MemoryHelper.compact();
    }

    Handler mHandlerFromMain;
    public void setHandlerFromMain(Handler handler) {
        mHandlerFromMain = handler;
    }

    public int pickUpInt(int x, int y) {
        //fY = fb.getHeight() - fY;
        SimpleVector dir = Interact2D.reproject2D3DWS( mCamera, mFrameBuffer, x, y).normalize();
        Object[] res=mWorld.calcMinDistanceAndObject3D(mCamera.getPosition(), dir, 10000 );

        Object3D picked = (Object3D)res[1];

        if( picked == null)
            return -1;
        for (int i = 0; i < mStatePoints.size(); i++) {
            if (picked.getID() == mStatePoints.elementAt(i).obs.getID()) {
                // Reset the last state point's color and transparency
                if(mLastStatePointIdx > 0) {
                    MyStatePoint sp = mStatePoints.elementAt(mLastStatePointIdx);
                    sp.obs.setAdditionalColor(colorTable(sp.stateSort));
                    sp.obs.setTransparency(mDefaultTransparency);
                }

                // Highlight the selected one
                MyStatePoint sp = mStatePoints.elementAt(i);
                sp.obs.setAdditionalColor(colorTable(MyStatePoint.StateSort.STATE_CURRENT));
                sp.obs.setTransparency(mCurrentTransparency);

                if (sp.stateSort == MyStatePoint.StateSort.STATE_PICTURE) {
                    if(sp.imagePath != "") {
                        // Tell the Main Activity to update the UI
                        Message message = Message.obtain();
                        message.arg1 = sp.imageIndex;       // Main information
                        message.what = FOR_GALLERY_SIGNAL;  // Sesame, sesame, open the door please~
                        mHandlerFromMain.sendMessage(message);  // emit!!
                    }
                }

                mLastStatePointIdx = i;
                return i;
            }
        }

        return -1;
    }

    public MyStatePoint getCurrentStatePoint() {
        if(mStatePoints.size() > 0) {
            return mStatePoints.lastElement();
        } else {
            return null;
        }
    }

    // </editor-fold>

}
