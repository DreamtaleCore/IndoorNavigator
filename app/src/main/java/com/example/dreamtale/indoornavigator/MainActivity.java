package com.example.dreamtale.indoornavigator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dreamtale.indoornavigator.DebugUtils.DebugDataWriter;
import com.example.dreamtale.indoornavigator.ImgProcLayer.ImageSync;
import com.example.dreamtale.indoornavigator.ImgProcLayer.MyViewFlipper;
import com.example.dreamtale.indoornavigator.PaintLayer.MyRenderer;
import com.example.dreamtale.indoornavigator.PaintLayer.MyStatePoint;
import com.example.dreamtale.indoornavigator.PaintLayer.MySurfaceView;
import com.example.dreamtale.indoornavigator.SensorLayer.Camera.CompareSizesByArea;
import com.example.dreamtale.indoornavigator.SensorLayer.Camera.ImageSaver;
import com.example.dreamtale.indoornavigator.SensorLayer.Motion.DataStructure;
import com.example.dreamtale.indoornavigator.SensorLayer.Motion.DataSynchronizer;
import com.example.dreamtale.indoornavigator.SensorLayer.Utils.AverageFilter;
import com.example.dreamtale.indoornavigator.SensorLayer.Utils.ComplementaryFilter;
import com.example.dreamtale.indoornavigator.SensorLayer.Utils.Euler;
import com.example.dreamtale.indoornavigator.SensorLayer.Utils.FilterDispatcher;
import com.example.dreamtale.indoornavigator.SensorLayer.Utils.MedianFilter;
import com.example.dreamtale.indoornavigator.Share.MyIUiListener;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.open.utils.ThreadManager;
import com.tencent.tauth.Tencent;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.util.BitmapHelper;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    // <editor-fold desc="private parameters declarations BLOCK">
    private Context mContext = this;
    public static Activity mActivity = null;
    MySurfaceView mMySurfaceView = null;
    // </editor-fold>

    // <editor-fold desc="Maintain main activity BLOCK">
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mActivity != null) {
            copy(mActivity);
        }
        super.onCreate(savedInstanceState);
        generateTexture();

        setContentView(R.layout.activity_main);

        initInteractionWithUi();
        switchViewsById(R.id.include_main);
        // Start the sensor service
        initAndSetupAllSensors();
        startMotionSensors();
        initShare();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disposeCamera();
        mMySurfaceView.onPause();
        // pause the actions about sensors
        mSensorManager.unregisterListener(mSensorEventListener);
        mIsFirstIn = true;
        if (mActivity == null) {
            mActivity = this;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        if (mActivity != null) {
            copy(mActivity);
        }
        super.onResume();
        mMySurfaceView.onResume();
        activeCamera();

        // Check OpenCV weather load successfully
        if(!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Internal OpenCV lib not found",
                    Toast.LENGTH_SHORT).show();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this,
                    mImageSync.loaderCallback);
        } else {
            Toast.makeText(this, "OpenCV lib found inside the package, using it",
                    Toast.LENGTH_SHORT).show();
        }

        // Like onCreate, reopen the servers of sensors
        mIsFirstIn = true;
        initAndSetupAllSensors();
    }

    public void copy(Object src) {
        Field[] fs = src.getClass().getDeclaredFields();
        for (Field f : fs) {
            f.setAccessible(true);
            try {
                f.set(this, f.get(src));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onActivityShareResult(requestCode, resultCode, data);
    }
    // </editor-fold>

    // <editor-fold desc="Interactions with UI BLOCK">

    private void initIncludeView() {
        View viewConsole = findViewById(R.id.include_console);
        viewConsole.setVisibility(View.GONE);
        View viewContent = findViewById(R.id.include_main);
        viewContent.setVisibility(View.GONE);
        View viewCamera = findViewById(R.id.include_camera);
        viewCamera.setVisibility(View.GONE);
        View viewGallery = findViewById(R.id.include_gallery);
        viewGallery.setVisibility(View.GONE);
        View viewTools = findViewById(R.id.include_manage);
        viewTools.setVisibility(View.GONE);
        View viewSlide = findViewById(R.id.include_slideshow);
        viewSlide.setVisibility(View.GONE);
    }
    private void switchViewsById(int viewId) {
        initIncludeView();
        View view = findViewById(viewId);
        view.setVisibility(View.VISIBLE);
        if(viewId == R.id.include_main) {
            if (mIsCameraOpened) {
                disposeCamera();
            }
        } else if(viewId == R.id.include_slideshow){
            switchIntoSlideshow();
        }
    }

    MyStatePoint mTestStatePoint = new MyStatePoint();
    private void initInteractionWithUi() {

        final FloatingActionButton fabStart = (FloatingActionButton) findViewById(R.id.fabStart);
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Random ran = new Random();
                mTestStatePoint.coordinate.x += ran.nextFloat() * 10f;
                mTestStatePoint.coordinate.y += ran.nextFloat() * 10f;
                mTestStatePoint.coordinate.z += ran.nextFloat() * 10f;

                mTestStatePoint.stateSort = MyStatePoint.StateSort.STATE_NORMAL;

                mTestStatePoint.pose.pitch += ran.nextDouble() * Math.PI / 10f;
                mTestStatePoint.pose.roll  += ran.nextDouble() * Math.PI / 10f;
                mTestStatePoint.pose.yaw   += ran.nextDouble() * Math.PI / 10f;
                if(Math.abs(mTestStatePoint.pose.pitch) > Math.PI / 2) {
                    mTestStatePoint.pose.pitch = 0;
                }
                if(Math.abs(mTestStatePoint.pose.roll) > Math.PI / 2) {
                    mTestStatePoint.pose.roll = 0;
                }
                if(Math.abs(mTestStatePoint.pose.yaw) > Math.PI / 2) {
                    mTestStatePoint.pose.yaw = 0;
                }

                // TODO: 2017/3/26 Need to be realized this function
                fabStart.setImageResource(R.drawable.ic_play_pause);

                mSensorDataProcessor.calibrate();
                mSensorDataProcessor.startProc();
            }
        });

        FloatingActionButton fabTakePhotoMode = (FloatingActionButton) findViewById(R.id.fabEditMode);
        fabTakePhotoMode.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                switchViewsById(R.id.include_camera);
                if(!mIsCameraOpened)
                    activeCamera();
            }
        });
        FloatingActionButton fabTakePicture = (FloatingActionButton) findViewById(R.id.fabTakePicture);
        fabTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        FloatingActionButton fabImgProc = (FloatingActionButton) findViewById(R.id.fabGalleryProcess);
        fabImgProc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mImageSync != null) {
                    if(!mImageSync.isProceeded)
                        mImageSync.procSrc2Gray();
                    if(!mIsGallerySwitched) {
                        mGalleryImage.setImageBitmap(mImageSync.dstBitmap);
                    } else {
                        mGalleryImage.setImageBitmap(mImageSync.srcBitmap);
                    }
                    mIsGallerySwitched = !mIsGallerySwitched;
                }
            }
        });

        // <editor-fold desc="Needn't changed temp BLOCK">

        // The original setting with basic ui
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // </editor-fold>

        // Init view's layouts
        mMySurfaceView = (MySurfaceView) findViewById(R.id.surfaceViewMain);
        mTextureView = (TextureView) findViewById(R.id.textureViewCamera);

        mGalleryImage = (ImageView) findViewById(R.id.galleryImageMain);

        mTvAccX = (TextView) findViewById(R.id.tvAccX);
        mTvAccY = (TextView) findViewById(R.id.tvAccY);
        mTvAccZ = (TextView) findViewById(R.id.tvAccZ);
        mTvVelX = (TextView) findViewById(R.id.tvVelX);
        mTvVelY = (TextView) findViewById(R.id.tvVelY);
        mTvVelZ = (TextView) findViewById(R.id.tvVelZ);
        mTvPathX = (TextView) findViewById(R.id.tvPathX);
        mTvPathY = (TextView) findViewById(R.id.tvPathY);
        mTvPathZ = (TextView) findViewById(R.id.tvPathZ);

        mTvRoll  = (TextView) findViewById(R.id.tvRoll);
        mTvPitch = (TextView) findViewById(R.id.tvPitch);
        mTvYaw   = (TextView) findViewById(R.id.tvYaw);

        mCbDefaultSwitch = (CheckBox) findViewById(R.id.cbDefaultSwitch);
        mSbHeightAdjust = (SeekBar) findViewById(R.id.sbHeightAdjust);
        mTvHeightAdjusted = (TextView) findViewById(R.id.tvHeightAdjusted);
        mSbLeftRateAdjust = (SeekBar) findViewById(R.id.sbLeftRateAdjust);
        mTvLeftRateAdjusted = (TextView) findViewById(R.id.tvLeftRateAdjusted);
        mSbPosePrecisionAdjust = (SeekBar) findViewById(R.id.sbPosePrecisionAdjust);
        mTvPosePrecisionAdjusted = (TextView) findViewById(R.id.tvPosePrecisionAdjusted);
        mSbJudderThresholdAdjust = (SeekBar) findViewById(R.id.sbJudderThresholdAdjust);
        mTvJudderThresholdAdjusted = (TextView) findViewById(R.id.tvJudderThresholdAdjusted);

        mNvMenu = (NavigationView) findViewById(R.id.nav_view);
        mVfSlideshowImage = (MyViewFlipper) findViewById(R.id.vfSlideshowImage);
        mTvSlideshowImageInfo = (TextView) findViewById(R.id.tvSlideshowImageInfo);

        initGalleryView();

        initManageView();

        initSlideshowView();

        // todo: Make test here
        // testEigenCompute();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(mIsCameraOpened)
            disposeCamera();
        if (id == R.id.nav_camera) {
            if(!mIsCameraOpened)
                activeCamera();
            switchViewsById(R.id.include_camera);

        } else if (id == R.id.nav_gallery) {
            switchViewsById(R.id.include_gallery);
        } else if (id == R.id.nav_slideshow) {
            switchViewsById(R.id.include_slideshow);
        } else if (id == R.id.nav_manage) {
            switchViewsById(R.id.include_manage);
        } else if (id == R.id.nav_share) {
            shareToQQZone();
        } else if (id == R.id.nav_send) {
            sendToQQ();
        } else if (id == R.id.nav_home) {
            switchViewsById(R.id.include_main);
        } else if (id == R.id.nav_console) {
            switchViewsById(R.id.include_console);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onActionReset_Clicked(MenuItem item) {
        mMySurfaceView.recovery();
    }

    private long mLastPressBackTimestamp = 0;
    private final long INTERMISSION_TIME = 200;  // 200 ms, 0.2s
    NavigationView mNvMenu;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mLastPressBackTimestamp < INTERMISSION_TIME) {
                return super.onKeyDown(keyCode, event);
            } else {
                mLastPressBackTimestamp = currentTime;
                Toast.makeText(mContext, "Double click back to exit", Toast.LENGTH_SHORT).show();
                switchViewsById(R.id.include_main);
                return false;
            }
        }
        return false;
    }

    // </editor-fold>

    // <editor-fold desc="Camera operations BLOCK">
    private enum StateOptions {
        STATE_PREVIEW,
        STATE_WAITING_LOCK,
        STATE_WAITING_PRE_CAPTURE,
        STATE_WAITING_NO_PRE_CAPTURE,
        STATE_PICTURE_TAKEN
    }
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private String mCameraId;
    private TextureView mTextureView;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mImageReader;
    // TODO: 2017/3/29 Create image dir to storage the images
    private volatile File mPictureSavedFile = null;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private StateOptions mState = StateOptions.STATE_PREVIEW;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mFlashSupported;
    private int mSensorOrientation;
    private boolean mIsCameraOpened = false;
    private MyStatePoint mSpCurrent = null;
    private Vector<MyStatePoint> mSpImages = new Vector<>();
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    mCameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    camera.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    mCameraOpenCloseLock.release();
                    camera.close();
                    mCameraDevice = null;
                    System.out.println("Camera device occur error!");
                }
            };
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

            mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mPictureSavedFile));
        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            Integer aeState, afState;
            switch (mState) {
                case STATE_PREVIEW:
                    //Reset something
                    break;
                case STATE_WAITING_LOCK:
                    afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if( null == afState) {
                        captureStillPicture();
                    } else if(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AF_STATE can be null in some devices
                        aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if(aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = StateOptions.STATE_PICTURE_TAKEN;
                            captureStillPicture();

                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                case STATE_WAITING_PRE_CAPTURE:
                    aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if(aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = StateOptions.STATE_WAITING_NO_PRE_CAPTURE;
                    }
                    break;
                case STATE_WAITING_NO_PRE_CAPTURE:
                    aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if(aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = StateOptions.STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                case STATE_PICTURE_TAKEN:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);
        }
    };

    // Can be used by other blocks
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void activeCamera() {
        startBackgroundThread();
        if(mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            System.out.println("Opening the camera...");
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            System.out.println("Waiting for opening camera...");
        }
        mIsCameraOpened = true;
    }
    private void disposeCamera() {
        closeCamera();
        stopCameraBackgroundThread();
        mIsCameraOpened = false;
    }
    // Internal functions just for block Camera
    private void runPreCaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mState = StateOptions.STATE_WAITING_PRE_CAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void captureStillPicture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // Use the same AE and AF mode as the preview
            // Do NOT lost the target
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation() + 90;
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {

                    mSpCurrent = mMySurfaceView.getCurrentStatePoint();
                    if (mSpCurrent == null) {
                        if (mPictureSavedFile == null) {
                            mPictureSavedFile = new File(getExternalFilesDir(null), "pic.jpg");
                        }
                    } else {
                        String filename = "" + mSpCurrent.obs.getID() + ".jpg";
                        mPictureSavedFile = new File(getExternalFilesDir(null), filename);
                    }

                    unlockFocus();

                    if (mSpCurrent != null) {
                        // Set the current state point as image point
                        mSpCurrent.imagePath = mPictureSavedFile.toString();
                        mSpCurrent.stateSort = MyStatePoint.StateSort.STATE_PICTURE;
                        mSpCurrent.obs.setAdditionalColor(new MyRenderer().colorTable(
                                MyStatePoint.StateSort.STATE_PICTURE));
                        mSpCurrent.obs.setScale(2.0f);
                        // Add the image to list for gallery show
                        // Record the image index for fast access and show
                        mSpCurrent.imageIndex = mSpImages.size();
                        mSpImages.add(mSpCurrent);
                        // Show to the user
                        Snackbar.make(findViewById(R.id.main_layout), "Image saved successfully!",
                                Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    }
                }
            };
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if(mCameraDevice ==  null) {
            return;
        }
    }
    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private Integer getOrientation(int rotation) {
        int retRotation = 0;
        switch (rotation) {
            case 0:
                retRotation = 90;
                break;
            case 90:
                retRotation = 0;
                break;
            case 180:
                retRotation = 270;
                break;
            case 270:
                retRotation = 180;
                break;
            default:
                break;
        }
        return (retRotation + mSensorOrientation + 270) % 270;
    }
    private void createCameraPreviewSession() {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        assert texture != null;
// TODO: 2017/2/3 Buffer size
        texture.setDefaultBufferSize(1024, 768);
        Surface surface = new Surface(texture);
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if(null == mCameraDevice) {
                                return;
                            }
                            mCaptureSession = session;
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            setAutoFlash(mPreviewRequestBuilder);
                            mPreviewRequest = mPreviewRequestBuilder.build();
                            try {
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            System.out.println("onConfigureFailed");
                        }
                    }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if(mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openCamera(int width, int height) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(mContext, "No camera permission!!", Toast.LENGTH_SHORT).show();
            requestCameraPermission();

            return;
        }
        setupCameraOutputs(width, height);
//        configureTransform(width, height);
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            if(!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            throw new RuntimeException("Interrupted while trying to lock a camera opening.", e);
        }

    }
    private void setupCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera here
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if(map == null) {
                    continue;
                }
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/ 2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                // Rotation here
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("Error: Null pointer in camera searching.");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "No camera permission!!", Toast.LENGTH_SHORT).show();
            } else {
                onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
    private void stopCameraBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if(null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if(null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if(null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void takePicture() {
        lookFocus();
    }
    private void lookFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
        mState = StateOptions.STATE_WAITING_LOCK;
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    // </editor-fold>
    
    // <editor-fold desc="Gallery with OpenCV functions BLOCK">
    private ImageSync mImageSync = new ImageSync(this);
    private ImageView mGalleryImage;
    private static boolean mIsGallerySwitched = false;
    private Handler mGalleryHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MyRenderer.FOR_GALLERY_SIGNAL) {
                int imgId = msg.arg1;
                Bitmap bitmap = BitmapFactory.decodeFile(mSpImages.elementAt(imgId).imagePath);
                mGalleryImage.setImageBitmap(bitmap);
                switchViewsById(R.id.include_gallery);
            }
        }
    };

    void initGalleryView()
    {
        mMySurfaceView.setHandlerFromMain(mGalleryHandler);
        mImageSync.srcBitmap = BitmapFactory.decodeResource( getResources(), R.drawable.img_girl);
        mGalleryImage.setImageBitmap(mImageSync.srcBitmap);
    }
    // </editor-fold>

    // <editor-fold desc="Motion sensor Block">
    private Sensor mMotionSensor;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private double mLastTimestamp;
    private TextView mTvAccX;
    private TextView mTvAccY;
    private TextView mTvAccZ;
    private TextView mTvVelX;
    private TextView mTvVelY;
    private TextView mTvVelZ;
    private TextView mTvPathX;
    private TextView mTvPathY;
    private TextView mTvPathZ;

    private TextView mTvRoll;
    private TextView mTvPitch;
    private TextView mTvYaw;

    private boolean mIsFirstIn = true;

    // structure of motion data:
    // +--------------------+-----------------+------------------+------------------+----------+
    // |   magnitude data   | gyroscope data  |acceleration data |  liner-acc data  |time span |
    // +--------------------+-----------------+------------------+------------------+----------+
    // |   3 * double size  | 3 * double size | 3 * double size  | 3 * double size  |1 * double|
    // +--------------------+-----------------+------------------+------------------+----------+
    private final int SENSOR_NUM = 13;
    private double[] mMotionData = new double[SENSOR_NUM]; // The last one is the time span | ms
    private Handler mSensorHandler;
    private final double UPDATE_UI_FREQUENCY = 5;
    private int mTimeCounter4UpdateUI = 0;
    private DataSynchronizer mMdSynchronizer = new DataSynchronizer();
    private SensorDataProcessor mSensorDataProcessor;
    private double mPoseThreshold    = 0.005;
    private File mFileForDebug;

    private void initAndSetupAllSensors() {
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        // Register accelerometer
        mMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(mMotionSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mMotionSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(mContext, "No ACCELEROMETER detected", Toast.LENGTH_SHORT).show();
        }
        // Register magnetic field
        mMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(mMotionSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mMotionSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(mContext, "No MAGNETIC_FIELD detected", Toast.LENGTH_SHORT).show();
        }
        // Register gyroscope
        mMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if(mSensorManager != null) {
            mSensorManager.registerListener(mSensorEventListener, mMotionSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(mContext, "No GYROSCOPE detected", Toast.LENGTH_SHORT).show();
        }
        // Register linear acceleration for init
        mMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if(mSensorManager != null) {
            mSensorManager.registerListener(mSensorEventListener, mMotionSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(mContext, "No LINEAR_ACCELERATION detected", Toast.LENGTH_SHORT).show();
        }

        String filePath = String.valueOf(getExternalFilesDir(null));
        mFileForDebug = new File(filePath, "sensorLogForDebug.csv");
        try {
            mFileForDebug.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMotionSensors() {

        mSensorDataProcessor = new SensorDataProcessor();
        mSensorDataProcessor.start();

        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                DataStructure mdCurrent = new DataStructure();

                mdCurrent.data[0] = event.values[0];
                mdCurrent.data[1] = event.values[1];
                mdCurrent.data[2] = event.values[2];
                // timestamp Unit:ms
                mdCurrent.timestamp = (long) (event.timestamp / 1e7);
                // If the sensor data is unreliable return
                if(event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    return;
                }
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_GYROSCOPE:
                        mdCurrent.type = DataStructure.DATA_TYPE_GYR;
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        mdCurrent.type = DataStructure.DATA_TYPE_ACC;
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        mdCurrent.type = DataStructure.DATA_TYPE_MAG;
                        break;
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        mdCurrent.type = DataStructure.DATA_TYPE_LIN;
                        break;
                    default:
                        break;
                }
                mMdSynchronizer.push(mdCurrent);

                if(mMdSynchronizer.synchronize(mMotionData)) {
                    if(mIsFirstIn) {
                        mLastTimestamp = mMotionData[SENSOR_NUM - 1];
                        mIsFirstIn = false;
                    } else {
                        double currentTimestamp = mMotionData[12];
                        double timeSpan = currentTimestamp - mLastTimestamp;
                        // At here, the motion data update as time span
//                        System.out.println("timeSpan = " + timeSpan);
                        mMotionData[SENSOR_NUM - 1] = timeSpan;
                        mSensorHandler.obtainMessage(0, mMotionData).sendToTarget();

                        mLastTimestamp = currentTimestamp;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    class SensorDataProcessor extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            mSensorHandler = new Handler() {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case 0:
                            double[] srcData = (double[])msg.obj;
                            final double[] dstData = new double[SENSOR_NUM];
                            // Process sensor data here
                            final boolean ret = procSensorRawData(srcData, dstData);
                            mTimeCounter4UpdateUI++;

                            // Update the UI
                            if (mTimeCounter4UpdateUI > (1000 / UPDATE_UI_FREQUENCY) / 5) {

                                mTimeCounter4UpdateUI = 0;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(ret) {
                                            // TODO: 2017/3/26  height need extra debug
                                            if (mIsUseDefaultValue) {
                                                // Do nothing here because the height
                                                // we computed is default value
                                            } else {
                                                dstData[8] = mHeightInScene;
                                            }
                                            
                                            MyStatePoint msp = new MyStatePoint();
                                            msp.coordinate.x = (float) dstData[6];
                                            msp.coordinate.y = (float) dstData[7];
                                            msp.coordinate.z = (float) dstData[8];

                                            double length = Math.sqrt(
                                                            dstData[6] * dstData[6] +
                                                            dstData[7] * dstData[7] +
                                                            dstData[8] * dstData[8]
                                            );

                                            msp.pose.roll    = dstData[9];
                                            msp.pose.pitch   = dstData[10];
                                            msp.pose.yaw     = dstData[11];
                                            msp.stateSort = MyStatePoint.StateSort.STATE_NORMAL;

                                            // Only if the judder is greater than the judder-threshold
                                            // then update the UI
                                            if (length >= mJudderThreshold * dstData[12] *
                                                    (1000 / UPDATE_UI_FREQUENCY) / 5) {
                                                mMySurfaceView.addNewStatePoint(msp);
                                            }

                                            mTvAccX.setText("" + dstData[0]);
                                            mTvAccY.setText("" + dstData[1]);
                                            mTvAccZ.setText("" + dstData[2]);
                                            mTvVelX.setText("" + dstData[3]);
                                            mTvVelY.setText("" + dstData[4]);
                                            mTvVelZ.setText("" + dstData[5]);
                                            mTvPathX.setText("" + dstData[6]);
                                            mTvPathY.setText("" + dstData[7]);
                                            mTvPathZ.setText("" + dstData[8]);
                                        }
                                        mTvRoll.setText("" + dstData[9]);
                                        mTvPitch.setText("" + dstData[10]);
                                        mTvYaw.setText("" + dstData[11]);
                                    }
                                });
                            }

                            break;
                    }
                }
            };
            Looper.loop();
        }

        // <editor-fold desc="Sensor private parameters BLOCK">
        private double[] mSpeed = {0f, 0f, 0f};
        private double[] mShift = {0f, 0f, 0f};
        private double[] mAngle = {0f, 0f, 0f};
        private volatile Euler mLastAccPose = new Euler();
        private volatile boolean mIsStart = false;
        private volatile boolean mIsNeedCalibrate = false;
        private volatile boolean mIsFirst = true;
        private volatile double[] mAccOffset = {0f, 0f, 0f};
        private volatile double[] mGyrOffset = {0f, 0f, 0f};
        private volatile int mInitCounter = 0;

        private volatile MedianFilter mMfMag = new MedianFilter();
        private volatile MedianFilter mMfGyr = new MedianFilter();
        private volatile MedianFilter mMfAcc = new MedianFilter();
        private volatile MedianFilter mMfLin = new MedianFilter();

        private volatile AverageFilter mAfMag = new AverageFilter();
        private volatile AverageFilter mAfGyr = new AverageFilter();
        private volatile AverageFilter mAfAcc = new AverageFilter();
        private volatile AverageFilter mAfLin = new AverageFilter();

        private volatile FilterDispatcher mFusion = null;

        private volatile ComplementaryFilter mCFilter = new ComplementaryFilter();

        private volatile DebugDataWriter mDdwDstData = new DebugDataWriter(mContext);
        private volatile DebugDataWriter mDdwFusionData = new DebugDataWriter(mContext);
        // </editor-fold>

        /**
         * Deal with the raw sensor data to pose data
         * @param motionData [in] raw sensor data
         * structure of motion data:
         * +--------------------+-----------------+------------------+------------------+----------+
         * |   magnitude data   | gyroscope data  |acceleration data |  liner-acc data  |time span |
         * +--------------------+-----------------+------------------+------------------+----------+
         * |   3 * double size  | 3 * double size | 3 * double size  | 3 * double size  |1 * double|
         * +--------------------+-----------------+------------------+------------------+----------+
         * @param motionData    [out]
         * @param dstData [out] mobile data for debug
         */
        private boolean procSensorRawData(final double[] motionData, double[] dstData) {
            // Median filter here first to remove the weird data

            // <editor-fold desc="Median filter here first to remove the weird data BLOCK">
            mMfMag.addData(new Point3(motionData[0], motionData[1], motionData[2]));
            mMfGyr.addData(new Point3(motionData[3], motionData[4], motionData[5]));
            mMfAcc.addData(new Point3(motionData[6], motionData[7], motionData[8]));
            mMfLin.addData(new Point3(motionData[9], motionData[10], motionData[11]));
            boolean bMag = mMfMag.process();
            boolean bGyr = mMfGyr.process();
            boolean bAcc = mMfAcc.process();
            boolean bLin = mMfLin.process();
            if(bMag && bGyr && bAcc && bLin) {
                motionData[0]  = mMfMag.getResult().x;
                motionData[1]  = mMfMag.getResult().y;
                motionData[2]  = mMfMag.getResult().z;
                motionData[3]  = mMfGyr.getResult().x;
                motionData[4]  = mMfGyr.getResult().y;
                motionData[5]  = mMfGyr.getResult().z;
                motionData[6]  = mMfAcc.getResult().x;
                motionData[7]  = mMfAcc.getResult().y;
                motionData[8]  = mMfAcc.getResult().z;
                motionData[9]  = mMfLin.getResult().x;
                motionData[10] = mMfLin.getResult().y;
                motionData[11] = mMfLin.getResult().z;
            } else {
                return false;
            }
            // </editor-fold>

            // <editor-fold desc="Average filter here for smoothing the data BLOCK">
            mAfMag.addData(new Point3(motionData[0], motionData[1], motionData[2]));
            mAfGyr.addData(new Point3(motionData[3], motionData[4], motionData[5]));
            mAfAcc.addData(new Point3(motionData[6], motionData[7], motionData[8]));
            mAfLin.addData(new Point3(motionData[9], motionData[10], motionData[11]));
            bMag = mAfMag.process();
            bGyr = mAfGyr.process();
            bAcc = mAfAcc.process();
            bLin = mAfLin.process();
            if(bMag && bGyr && bAcc && bLin) {
                motionData[0]  = mAfMag.getResult().x;
                motionData[1]  = mAfMag.getResult().y;
                motionData[2]  = mAfMag.getResult().z;
                motionData[3]  = mAfGyr.getResult().x;
                motionData[4]  = mAfGyr.getResult().y;
                motionData[5]  = mAfGyr.getResult().z;
                motionData[6]  = mAfAcc.getResult().x;
                motionData[7]  = mAfAcc.getResult().y;
                motionData[8]  = mAfAcc.getResult().z;
                motionData[9]  = mAfLin.getResult().x;
                motionData[10] = mAfLin.getResult().y;
                motionData[11] = mAfLin.getResult().z;
            } else {
                return false;
            }
            // </editor-fold>

            if(!this.mIsStart) {
                // Record the accelerate sensor data in order to get the average for init
                mAccOffset[0] += motionData[9];
                mAccOffset[1] += motionData[10];
                mAccOffset[2] += motionData[11];

                mGyrOffset[0] += motionData[3];
                mGyrOffset[1] += motionData[4];
                mGyrOffset[2] += motionData[5];

                mInitCounter++;

                return false;
            }
            else {
                if(this.mIsFirst) {
                    if(mInitCounter > 0) {

                        // Compute the offset of accelerate sensor data
                        mAccOffset[0] /= (double) mInitCounter;
                        mAccOffset[1] /= (double) mInitCounter;
                        mAccOffset[2] /= (double) mInitCounter;

                        // Compute the offset of gyroscope sensor data
                        mGyrOffset[0] /= (double) mInitCounter;
                        mGyrOffset[1] /= (double) mInitCounter;
                        mGyrOffset[2] /= (double) mInitCounter;

                        System.out.println("##################################");
                        System.out.println("mAccOffset[0] = " + mAccOffset[0]);
                        System.out.println("mAccOffset[1] = " + mAccOffset[1]);
                        System.out.println("mAccOffset[2] = " + mAccOffset[2]);
                        System.out.println("mGyrOffset[0] = " + mGyrOffset[0]);
                        System.out.println("mGyrOffset[1] = " + mGyrOffset[1]);
                        System.out.println("mGyrOffset[2] = " + mGyrOffset[2]);
                        System.out.println("mInitCounter = " + mInitCounter);
                    }
                }
                // The frequency: 100Hz
                double t = motionData[12] * 0.01f;
                // For exception to restrict the data for accuracy
                if (t > 0.011 || t < 0.009)
                    return false;

                motionData[9]  -= mAccOffset[0];
                motionData[10] -= mAccOffset[1];
                motionData[11] -= mAccOffset[2];

                // Step1: use magnitude data & accelerate data to compute the
                //        absolute pose data with noise-some think as Gaussian
                // TODO: 2017/3/5 Maybe convert this to c++ code for efficiency

//                System.out.println("t = " + t);
//                System.out.println();

                double Ax = motionData[6] - mAccOffset[0];
                double Ay = motionData[7] - mAccOffset[1];
                double Az = motionData[8] - mAccOffset[2];
                final double Ex = motionData[0];
                final double Ey = motionData[1];
                final double Ez = motionData[2];
                double Hx = Ey * Az - Ez * Ay;
                double Hy = Ez * Ax - Ex * Az;
                double Hz = Ex * Ay - Ey * Ax;
                final double normH = Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
                if (normH < 0.1f) {
                    // device is close to free fall (or in space?), or close to
                    // magnetic north pole. Typical values are  > 100.
                    return false;
                }
                final double invH = 1.0f / normH;
                Hx *= invH;
                Hy *= invH;
                Hz *= invH;
                final double invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
                Ax *= invA;
                Ay *= invA;
                Az *= invA;
                final double Mx = Ay * Hz - Az * Hy;
                final double My = Az * Hx - Ax * Hz;
                final double Mz = Ax * Hy - Ay * Hx;

                // R is the rotation matrix for understanding
                //  /  R[0][0]   R[0][1]   R[0][2]  \
                //  |  R[1][0]   R[1][1]   R[1][2]  |
                //  \  R[2][0]   R[2][2]   R[2][2]  /
                double[][] R = new double[3][3];
                R[0][0] = Hx;
                R[0][1] = Hy;
                R[0][2] = Hz;
                R[1][0] = Mx;
                R[1][1] = My;
                R[1][2] = Mz;
                R[2][0] = Ax;
                R[2][1] = Ay;
                R[2][2] = Az;

                // Azimuth, angle of rotation about the -z axis. likewise: yaw
                // When facing north, this angle is 0, when facing south, this angle is &pi;.
                // Likewise, when facing east, this angle is &pi;/2, and when facing west,
                // this angle is -&pi;/2. The range of values is -&pi; to &pi;.
                double azimuth = (float) Math.atan2(R[0][1], R[1][1]);
                // Pitch, angle of rotation about the x axis.
                // This value represents the angle between a plane parallel to the device's
                // screen and a plane parallel to the ground. Assuming that the bottom
                // edge of the device faces the user and that the screen is face-up, tilting
                // the top edge of the device toward the ground creates a positive pitch angle.
                // The range of values is -&pi; to &pi;
                double pitch = (float) Math.asin(-R[2][1]);
                // Roll, angle of rotation about the y axis. This value represents the angle
                // between a plane perpendicular to the device's screen and a plane perpendicular
                // to the ground. Assuming that the bottom edge of the device faces the user
                // and that the screen is face-up, tilting the left edge of the device toward
                // the ground creates a positive roll angle.
                // The range of values is -&pi;/2 to &pi;/2.
                double roll = (float) Math.atan2(-R[2][0], R[2][2]);

                // Brief it so just like as below:
                azimuth = Math.atan2(Hy, My);
                pitch   = Math.asin(-Ay);
                roll    = Math.atan2(-Ax, Az);

                //Step2: Use gyroscope data to integrate the pose data
                //       with high temporary accuracy but error grows with time
                // TODO: 2017/3/5 Make clear the position
                if (this.mIsFirst) {
                    mLastAccPose.pitch = mAngle[0] = roll;
                    mLastAccPose.roll  = mAngle[1] = pitch;
                    mLastAccPose.yaw   = mAngle[2] = azimuth;

                    System.out.println("Init the sensors successfully.");

                    // write debug information for debug
                    mDdwDstData.setFileName("MotionPoseData.csv");
                    String hdLine = "acc_x,acc_y,acc_z,vel_x,vel_y,vel_z,"
                            +"path_x,path_y,path_z,roll,pitch,yaw\r\n";
                    mDdwDstData.setDataSetHeader(hdLine);
                    mDdwFusionData.setFileName("CmpSensorFusion.csv");
                    hdLine = "src1_roll,src1_pitch,src1_yaw,src2_roll,src2_pitch,"
                            + "src2_yaw,src1_roll_check,src1_pitch_check," +
                            "src1_yaw_check,result_roll,result_pitch,result_yaw\r\n";
                    mDdwFusionData.setDataSetHeader(hdLine);

                    if (mIsUseDefaultValue) {
                        // 0.95 is default value
                        mCFilter.setLeftRate(0.95);
                    } else {
                        mCFilter.setLeftRate(mComplementaryLeftRate);
                    }

                    // Disable the first init
                    this.mIsFirst = false;
                    this.mIsNeedCalibrate = false;
                } else {
                    double dGyroX = (motionData[3] - mGyrOffset[0]) * t;
                    double dGyroY = (motionData[4] - mGyrOffset[1]) * t;
                    // yaw and azimuth have different direction between acc_mag & gyro
                    double dGyroZ = -(motionData[5] - mGyrOffset[2]) * t;

                    // Deal with gimbal lock
                    if (mLastAccPose.pitch > 2 && pitch < -2 ||
                            mLastAccPose.pitch < -2 && pitch > 2) {
                        mAngle[0] = pitch;
                    }
                    if (mLastAccPose.roll > 2 && roll < -2 ||
                            mLastAccPose.roll < -2 && roll > 2) {
                        mAngle[1] = roll;
                    }
                    if (mLastAccPose.yaw > 2 && azimuth < -2 ||
                            mLastAccPose.yaw < -2 && azimuth > 2) {
                        mAngle[2] = azimuth;
                    }

                    mAngle[0] += dGyroX;
                    mAngle[1] += dGyroY;
                    mAngle[2] += dGyroZ;

                    Euler src1 = new Euler(mAngle[0], mAngle[1], mAngle[2]);
                    Euler src2 = new Euler(roll, pitch, azimuth);

                    mCFilter.step(src1, src2);

                    Euler pose = mCFilter.getResultEuler();

                    dstData[0] = roll;
                    dstData[1] = pitch;
                    dstData[2] = azimuth;

                    dstData[3] = mAngle[0];
                    dstData[4] = mAngle[1];
                    dstData[5] = mAngle[2];

                    dstData[9]  = pose.roll;
                    dstData[10] = pose.pitch;
                    dstData[11] = pose.yaw;

                    mLastAccPose.roll  = mAngle[0] = pose.roll;
                    mLastAccPose.pitch = mAngle[1] = pose.pitch;
                    mLastAccPose.yaw   = mAngle[2] = pose.yaw;

                    // Write info for debug
                    String tcLine = String.valueOf(dstData[0]) + ","
                            + String.valueOf(dstData[1]) + ","
                            + String.valueOf(dstData[2]) + ","
                            + String.valueOf(dstData[3]) + ","
                            + String.valueOf(dstData[4]) + ","
                            + String.valueOf(dstData[5]) + ","
                            + String.valueOf(dstData[6]) + ","
                            + String.valueOf(dstData[7]) + ","
                            + String.valueOf(dstData[8]) + ","
                            + String.valueOf(dstData[9]) + ","
                            + String.valueOf(dstData[10]) + ","
                            + String.valueOf(dstData[11]) + "\r\n";
                    //mDdwFusionData.writeData(tcLine);

                    // Compute the path there
                    boolean ret;
                    ret = calcTheWorld(pose, new Point3(motionData[9],
                            motionData[10], motionData[11]), t);
                    if(ret == true) {
                        dstData[0] = mAccW[0];
                        dstData[1] = mAccW[1];
                        dstData[2] = mAccW[2];
                        dstData[3] = mWorldSpeed[0];
                        dstData[4] = mWorldSpeed[1];
                        dstData[5] = mWorldSpeed[2];
                        dstData[6] = mWorldPath[0];
                        dstData[7] = mWorldPath[1];
                        dstData[8] = mWorldPath[2];
                        dstData[9]  = pose.roll;
                        dstData[10] = pose.pitch;
                        dstData[11] = pose.yaw;
                        dstData[12] = t;
                        return true;
                    }
                }
                return false;
            }
        }

        private volatile double[] mWorldSpeed = {0d, 0d, 0d};
        private volatile double[] mWorldPath = {0d, 0d, 0d};
        private final double mLineAccThreshold = 0.03;
        private volatile Euler mLastPose = new Euler();
        private volatile double[] mAccW = new double[4];
        private boolean calcTheWorld(Euler pose, Point3 acc, double t) {
            double dRoll  = pose.roll  - mLastPose.roll;
            double dPitch = pose.pitch - mLastPose.pitch;
            double dYaw   = pose.yaw   - mLastPose.yaw;

            // update the last pose
            mLastPose.roll = pose.roll;
            mLastPose.pitch = pose.pitch;
            mLastPose.yaw = pose.yaw;

            if(dRoll > mPoseThreshold || dPitch > mPoseThreshold || dYaw > mPoseThreshold) {

                double normAcc = Math.sqrt(acc.x*acc.x + acc.y*acc.y + acc.z*acc.z);
                // TODO: 2017/3/25 Adjust here
                if(true || normAcc > mLineAccThreshold) {

                    double[] r = pose.toRotationMatrix();
                    Mat R = new Mat(4, 4, CvType.CV_64FC1);
                    R.put(0, 0, r);

                    double[] accB_t = {acc.x, acc.y, acc.z, 1};
                    Mat accB = new Mat(4, 1, CvType.CV_64FC1);
                    accB.put(0, 0, accB_t);

                    // Body to World
                    Mat accW = new Mat();
                    Core.gemm(R.inv(), accB, 1.0, new Mat(), 0, accW);

                    accW.get(0, 0, mAccW);

                    mWorldSpeed[0] += mAccW[0] * t;
                    mWorldSpeed[1] += mAccW[1] * t;
                    mWorldSpeed[2] += mAccW[2] * t;

                    // Not every one can run so fast, so ignore this case
                    if (Math.sqrt(
                            mWorldSpeed[0] * mWorldSpeed[0] +
                            mWorldSpeed[0] * mWorldSpeed[0] +
                            mWorldSpeed[0] * mWorldSpeed[0]
                    ) > 20) {
                        return false;
                    }

                    mWorldPath[0] += mWorldSpeed[0] * t;
                    mWorldPath[1] += mWorldSpeed[1] * t;
                    mWorldPath[2] += mWorldSpeed[2] * t;

//                    String tcLine = String.valueOf(mAccW[0]) + ","
//                            + String.valueOf(mAccW[1]) + ","
//                            + String.valueOf(mAccW[2]) + ","
//                            + String.valueOf(mWorldSpeed[0]) + ","
//                            + String.valueOf(mWorldSpeed[1]) + ","
//                            + String.valueOf(mWorldSpeed[2]) + ","
//                            + String.valueOf(mWorldPath[0]) + ","
//                            + String.valueOf(mWorldPath[1]) + ","
//                            + String.valueOf(mWorldPath[2]) + ","
//                            + String.valueOf(pose.roll) + ","
//                            + String.valueOf(pose.pitch) + ","
//                            + String.valueOf(pose.yaw) + "\r\n";

//                    mDdwDstData.writeData(tcLine);

                } else {
                    // Take as silent
                    return false;
                }
            } else {
                mWorldSpeed[0] = mWorldSpeed[1] = mWorldSpeed[2] = 0;
                return false;
            }

            return true;
        }

        private void init() {
            this.mSpeed[0] = this.mSpeed[1] = this.mSpeed[2] = 0f;
            this.mShift[0] = this.mShift[1] = this.mShift[2] = 0f;
            this.mAngle[0] = this.mAngle[1] = this.mAngle[2] = 0f;
            this.mAccOffset[0] = this.mAccOffset[1] = this.mAccOffset[2] = 0f;
            this.mGyrOffset[0] = this.mGyrOffset[1] = this.mGyrOffset[2] = 0f;
            mInitCounter = 0;
            mIsStart = false;
            mIsFirst = true;
        }

        public SensorDataProcessor() {
            init();
        }

        public void startProc() {
            if(mFusion == null) {

                mFusion = new FilterDispatcher();
                this.mIsStart = true;
            }
        }

        public void calibrate() {
            mWorldSpeed[0] = mWorldSpeed[1] = mWorldSpeed[2] = 0d;
            mIsNeedCalibrate = true;
        }

        public void pauseProc() {
            if(mFusion != null) {
                mFusion = null;
            }
        }
    }
    // </editor-fold>

    // <editor-fold desc="Manage to adjust parameters BLOCK">
    private double mHeightInScene = 0.5;
    private double mComplementaryLeftRate = 0.95;
    private double mJudderThreshold = 0.3;
    private CheckBox mCbDefaultSwitch;
    private SeekBar mSbHeightAdjust;
    private TextView mTvHeightAdjusted;
    private SeekBar mSbLeftRateAdjust;
    private TextView mTvLeftRateAdjusted;
    private SeekBar mSbPosePrecisionAdjust;
    private TextView mTvPosePrecisionAdjusted;
    private SeekBar mSbJudderThresholdAdjust;
    private TextView mTvJudderThresholdAdjusted;
    private boolean mIsUseDefaultValue = false;

    public void initManageView() {

        mCbDefaultSwitch.setChecked(false);

        mCbDefaultSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((CheckBox)v).isChecked()) {

                    mPoseThreshold = 0.01;
                    mComplementaryLeftRate = 0.95;
                    mHeightInScene = 0.5;

                    mSbHeightAdjust.setEnabled(false);
                    mTvHeightAdjusted.setEnabled(false);
                    mSbLeftRateAdjust.setEnabled(false);
                    mTvLeftRateAdjusted.setEnabled(false);
                    mSbPosePrecisionAdjust.setEnabled(false);
                    mTvPosePrecisionAdjusted.setEnabled(false);
                    mSbJudderThresholdAdjust.setEnabled(false);
                    mTvJudderThresholdAdjusted.setEnabled(false);
                    mIsUseDefaultValue = true;
                } else {
                    mSbHeightAdjust.setEnabled(true);
                    mTvHeightAdjusted.setEnabled(true);
                    mSbLeftRateAdjust.setEnabled(true);
                    mTvLeftRateAdjusted.setEnabled(true);
                    mSbPosePrecisionAdjust.setEnabled(true);
                    mTvPosePrecisionAdjusted.setEnabled(true);
                    mSbJudderThresholdAdjust.setEnabled(true);
                    mTvJudderThresholdAdjusted.setEnabled(true);
                    mIsUseDefaultValue = false;
                }
            }
        });

        //
        mSbHeightAdjust.setMax(800);
        mSbHeightAdjust.setProgress((int) (mHeightInScene * 2));
        mTvHeightAdjusted.setText("" + mHeightInScene);
        mSbHeightAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double rate = (double) progress / (double) mSbHeightAdjust.getMax();
                // Max height in scene is 400m
                mHeightInScene = Math.round(400 * rate * 10d) / 10d;
                mTvHeightAdjusted.setText("" + mHeightInScene);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }
        });

        mSbLeftRateAdjust.setMax(100);
        mSbLeftRateAdjust.setProgress((int) (mComplementaryLeftRate * 100));
        mTvLeftRateAdjusted.setText("" + mComplementaryLeftRate);
        mSbLeftRateAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mComplementaryLeftRate = (double)progress / 100d;
                mTvLeftRateAdjusted.setText("" + mComplementaryLeftRate);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }
        });

        // Can set from 0.01~0.10 to mPoseThreshold
        mSbPosePrecisionAdjust.setMax(1000);
        mSbPosePrecisionAdjust.setProgress((int) (mPoseThreshold * 10000));
        mTvPosePrecisionAdjusted.setText("" + mPoseThreshold);
        mSbPosePrecisionAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPoseThreshold = (double)progress / 10000d;
                mTvPosePrecisionAdjusted.setText("" + mPoseThreshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }
        });

        // Can set from 0~1m to mJudderThresholdAdjust
        mSbJudderThresholdAdjust.setMax(100);
        mSbJudderThresholdAdjust.setProgress((int) (mJudderThreshold * 100));
        mTvJudderThresholdAdjusted.setText("" + mJudderThreshold);
        mSbJudderThresholdAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mJudderThreshold = (double) progress / 100d;
                mTvJudderThresholdAdjusted.setText("" + mJudderThreshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing here
            }
        });

    }


    // </editor-fold>

    // <editor-fold desc="Slideshow for history re-play BLOCK">
    private MyViewFlipper mVfSlideshowImage;
    private TextView mTvSlideshowImageInfo;
    private int mStatePointShowIndex = 0;
    private int mStatePointShowSize;
    private Handler mSlideshowHandler = new Handler() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MyViewFlipper.FOR_SLIDESHOW_SIGNAL) {

                mStatePointShowIndex += msg.arg1;

                if(mStatePointShowIndex >= mStatePointShowSize)
                    mStatePointShowIndex -= mStatePointShowSize;
                if(mStatePointShowIndex < 0)
                    mStatePointShowIndex += mStatePointShowSize;

                updateStatePointInfo();
            }
        }
    };


    public void updateStatePointInfo() {

        if (mStatePointShowIndex >= mSpImages.size()) {
            Toast.makeText(mContext, "No image yet", Toast.LENGTH_SHORT).show();
            return;
        }
        MyStatePoint msp = mSpImages.elementAt(mStatePointShowIndex);
        String info = "Image index:  " + msp.imageIndex + "\r\n";
        info +=       "Current pose: [ " + String.format("%.4f", msp.pose.roll) + ", " +
                String.format("%.4f", msp.pose.pitch) + ", " +
                String.format("%.4f", msp.pose.yaw) + " ]\r\n";
        info +=       "Coordinate:    [ " + String.format("%.4f", msp.coordinate.x) + ", " +
                String.format("%.4f", msp.coordinate.y) + ", " +
                String.format("%.4f", msp.coordinate.z) + " ]\r\n";

        mTvSlideshowImageInfo.setText(info);
    }

    public void switchIntoSlideshow() {
        mVfSlideshowImage.removeAllViews();

        mStatePointShowSize = mSpImages.size();
        for (int i = 0; i < mStatePointShowSize; i++) {
            ImageView iv = new ImageView(this);
            Bitmap bm = BitmapFactory.decodeFile(mSpImages.elementAt(i).imagePath);
            iv.setImageBitmap(bm);
            mVfSlideshowImage.addView(iv);
        }

        updateStatePointInfo();
    }

    public void initSlideshowView() {
        mVfSlideshowImage.setContext(mContext);
        mVfSlideshowImage.setHandlerFromMain(mSlideshowHandler);
    }
    // </editor-fold>

    // <editor-fold desc="Share to friends BLOCK">
    private Tencent mTencent;
    private MyIUiListener mIUiListener;


    private void initShare() {
        mTencent = Tencent.createInstance("1106152542", getApplicationContext());
        mIUiListener = new MyIUiListener();
    }

    private String mScreenShootPath;
    private void screenshot(){
        // TODO: 2017/5/9 Need to hide the menu
        // Get the screen image
        View dView = getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        if (bmp != null)
        {
            try {
                // set the file path
                File file = new File(getExternalFilesDir(null), "indoor_navigator.png");
                mScreenShootPath = file.getPath();
                FileOutputStream os = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
            }
        }
    }

    private Bundle generateShareParams(boolean isSend) {
        screenshot();
        Bundle params = new Bundle();
        if (isSend) {
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            params.putString(QQShare.SHARE_TO_QQ_TITLE, "Indoor navigator --a cool way");
            params.putString(QQShare.SHARE_TO_QQ_SUMMARY, "The next century navigator");
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL,"https://github.com/DreamtaleCore");
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, mScreenShootPath);
            params.putString(QQShare.SHARE_TO_QQ_EXT_INT, "mailTo: dreamtalewind@gmail.com");
        }
        // the other one is share
        else {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
            params.putString(QzoneShare.SHARE_TO_QQ_TITLE, "Indoor navigator --a cool way");
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, "The next century navigator");
            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL,"https://github.com/DreamtaleCore");
            ArrayList<String> imgUrlList = new ArrayList<>();
            // TODO: 2017/5/9 Debug here
            imgUrlList.add("http://f.hiphotos.baidu.com/image/h%3D200/sign=6f05c5f929738bd4db21b531918a876c/6a600c338744ebf8affdde1bdef9d72a6059a702.jpg");
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imgUrlList);
            System.out.println("imgUrlList = " + imgUrlList);
        }
        return params;
    }

    private Bundle mParams = null;
    private void sendToQQ() {
        mParams = generateShareParams(true);

        if (mActivity == null) {
            mActivity = this;
        }
        // Share operations must realized in the main thread
        ThreadManager.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mTencent.shareToQQ(mActivity, mParams, mIUiListener);
            }
        });
    }

    private void shareToQQZone() {
        mParams = generateShareParams(false);

        if (mActivity == null) {
            mActivity = this;
        }
        // Share operations must realized in the main thread
        ThreadManager.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                mTencent.shareToQzone(mActivity, mParams, mIUiListener);
            }
        });
    }

    void onActivityShareResult(int requestCode, int resultCode, Intent data) {
        Tencent.onActivityResultData(requestCode, resultCode, data, mIUiListener);
        if(requestCode == Constants.REQUEST_API) {
            if (resultCode == Constants.REQUEST_QQ_SHARE
                    || resultCode == Constants.REQUEST_QZONE_SHARE
                    || resultCode == Constants.REQUEST_OLD_SHARE) {
                Tencent.handleResultData(data, mIUiListener);
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="Data Visible">
    void generateTexture() {
        Texture texture = new Texture(BitmapHelper.rescale(
                BitmapHelper.convert(getDrawable(
                        R.drawable.center_building_map)), 1024, 1024));

        TextureManager.getInstance().addTexture("indoor map", texture);
    }

    // </editor-fold>


    // <editor-fold desc="Test cpp BLOCK">

    // No comments

    // </editor-fold>
}
