package com.example.dreamtale.indoornavigator.PaintLayer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by DreamTale on 2017/2/12.
 */

public class MySurfaceView extends GLSurfaceView {


    // <editor-fold desc="Private parameters declarations BLOCK">
    MyRenderer mRenderer = new MyRenderer();

    private static final int DOUBLE_POINT_DISTANCE = 250;
    private static final long DOUBLE_CLICK_DELAY = 350;

    private enum GestureMode {
        GESTURE_ROTATE,
        GESTURE_ZOOM,
        GESTURE_DRAG,
        GESTURE_NONE
    }
    GestureMode mGestureMode = GestureMode.GESTURE_NONE;

    private float mLastPosX, mLastPosY, mCurrentPosX, mCurrentPosY;
    private long mLastDownTime = 0, mCurrentDownTime;

    private float mX, mY, mCurrentDistance, mLastDistance = 1f;
    // </editor-fold>

    // <editor-fold desc="Constructor BLOCK">
    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MySurfaceView(Context context) {
        super(context);
        init();
    }
    // </editor-fold>

    private void init() {
        setEGLConfigChooser(new EGLConfigChooser() {
            @Override
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                // Ensure that we get a 16bit framebuffer. Otherwise, we'll fall
                // back to Pixel flinger on some device (read: Samsung I7500)
                int[] attributes = new int[] {EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE};
                EGLConfig[] configs = new EGLConfig[1];
                int[] result = new int[1];
                egl.eglChooseConfig(display, attributes, configs, 1, result);
                return configs[0];
            }
        });
        this.setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionMode = MotionEventCompat.getActionMasked(event);
        if(actionMode == MotionEvent.ACTION_DOWN) {
            mLastPosX = event.getX();
            mLastPosY = event.getY();
            mRenderer.pickUpInt((int)(event.getX()), (int)(event.getY()));
            mGestureMode = GestureMode.GESTURE_ROTATE;
            mCurrentDownTime = event.getDownTime();
            if(mCurrentDownTime - mLastDownTime < DOUBLE_CLICK_DELAY) {
                mRenderer.recovery();
            }

            mLastDownTime = mCurrentDownTime;
            return true;
        } else if(actionMode == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_POINTER_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            mX = mY = -1;
            mGestureMode = GestureMode.GESTURE_NONE;
            return true;
        } else if(actionMode == MotionEvent.ACTION_MOVE) {

            mCurrentPosX = event.getX();
            mCurrentPosY = event.getY();
            switch (mGestureMode) {
                case GESTURE_ROTATE:
                    mX = (mCurrentPosX - mLastPosX) / 100f;
                    mY = (mCurrentPosY - mLastPosY) / 100f;
                    mRenderer.rotateWorld(mY, mX, 0f);
                    break;
                case GESTURE_ZOOM:
                    mCurrentDistance = computeDistance(event);
                    if(mCurrentDistance > DOUBLE_POINT_DISTANCE) {
                        float scaleZoom = mCurrentDistance - mLastDistance;
                        mRenderer.moveWorld(0f, 0f, scaleZoom / 3f);
                        mLastDistance = mCurrentDistance;
                    }
                    break;
                case GESTURE_DRAG:
                    mX = (mCurrentPosX - mLastPosX) / 3f;
                    mY = (mCurrentPosY - mLastPosY) / 3f;
                    mRenderer.moveWorld(mX, mY, 0f);
                    break;
                case GESTURE_NONE:
                    break;
            }
            mLastPosX = mCurrentPosX;
            mLastPosY = mCurrentPosY;

            return true;
        } else if(actionMode == MotionEvent.ACTION_POINTER_DOWN) {
            mCurrentDistance = computeDistance(event);
            if(mCurrentDistance > DOUBLE_POINT_DISTANCE) {
                mLastDistance = mCurrentDistance;
                mGestureMode = GestureMode.GESTURE_ZOOM;
            }
            else {
                mGestureMode = GestureMode.GESTURE_DRAG;
            }
            mLastPosX = event.getX();
            mLastPosY = event.getY();
        }

        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return super.onTouchEvent(event);
    }

    private float computeDistance(MotionEvent event) {
        try {
            float x = event.getX(0) - event.getX(1);
            x = x > 0 ? x : -x;
            float y = event.getY(0) - event.getY(1);
            y = y > 0 ? y : -y;
            return (float) Math.sqrt(x * x + y * y);
        }catch (Exception e) {
            return -1f;
        }
    }

    public void addNewStatePoint(MyStatePoint statePoint) {
        mRenderer.addNewStatePoint(statePoint);
        // Render manually
        requestRender();
    }

    public void recovery() {
        mRenderer.recovery();
        requestRender();
    }

    public MyStatePoint getCurrentStatePoint() {
        return mRenderer.getCurrentStatePoint();
    }

    public void setHandlerFromMain(Handler handler) {
        mRenderer.setHandlerFromMain(handler);
    }
}
