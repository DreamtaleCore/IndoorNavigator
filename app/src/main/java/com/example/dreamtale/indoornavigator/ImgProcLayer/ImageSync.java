package com.example.dreamtale.indoornavigator.ImgProcLayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by DreamTale on 2017/2/18.
 */

public class ImageSync {
    private Mat mSrc;
    private Mat mDst;

    public BaseLoaderCallback loaderCallback;
    public boolean isProceeded = false;
    public Bitmap srcBitmap;
    public Bitmap dstBitmap;

    public ImageSync(final Context context) {
        loaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch (status) {
                    case BaseLoaderCallback.SUCCESS:
                        Toast.makeText(context, "Load module successfully",
                                Toast.LENGTH_SHORT).show();
                    break;
                    default:
                        super.onManagerConnected(status);
                        Toast.makeText(context, "Load module failed",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    public void procSrc2Gray() {
        mSrc = new Mat();
        mDst = new Mat();
        Utils.bitmapToMat(srcBitmap, mSrc);
        Imgproc.cvtColor(mSrc, mDst, Imgproc.COLOR_RGB2GRAY);

        dstBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(),
                Bitmap.Config.RGB_565);
        Utils.matToBitmap(mDst, dstBitmap);

        isProceeded = true;
    }

}
