package com.example.dreamtale.indoornavigator.ImgProcLayer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.widget.ViewFlipper;

import com.example.dreamtale.indoornavigator.R;

/**
 * Created by DreamTale on 2017/3/28.
 */

public class MyViewFlipper extends ViewFlipper {

    public final static int FOR_SLIDESHOW_SIGNAL = 0x521e;

    public MyViewFlipper(Context context) {
        super(context);
    }

    public MyViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Context mContext;
    private float mStartX;

    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionMode = MotionEventCompat.getActionMasked(event);
        switch (actionMode) {
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                // Tell the Main Activity to update the UI
                Message message = Message.obtain();
                message.what = FOR_SLIDESHOW_SIGNAL;  // Sesame, sesame, open the door please~

                if (event.getX() - mStartX > 100) {
                    setInAnimation(mContext, R.anim.left_in);
                    setOutAnimation(mContext, R.anim.left_out);
                    showPrevious();

                    message.arg1 = -1;       // Main information
                }
                if (mStartX - event.getX() > 100) {
                    setInAnimation(mContext, R.anim.right_in);
                    setOutAnimation(mContext, R.anim.right_out);
                    showNext();

                    message.arg1 = 1;       // Main information
                }
                mHandlerFromMain.sendMessage(message);  // emit!!
                break;
            default:
                break;
        }
        return true;
    }

    private Handler mHandlerFromMain;
    public void setHandlerFromMain(Handler mHandlerFromMain) {
        this.mHandlerFromMain = mHandlerFromMain;
    }
}
