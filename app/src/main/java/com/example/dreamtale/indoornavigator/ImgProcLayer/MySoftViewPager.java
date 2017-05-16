package com.example.dreamtale.indoornavigator.ImgProcLayer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.nineoldandroids.view.ViewHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by DreamTale on 2017/3/28.
 */

public class MySoftViewPager extends ViewPager {

    private float mTrans;
    private float mScale;

    // The max min_scaled rate
    private static final float SCALE_MAX = 0.5f;
    private static final String TAG = "MySoftViewPager";

    // To storage the view --> current position
    private HashMap<Integer, View> mChildrenViews = new LinkedHashMap<Integer, View>();

    private View mLeft;
    private View mRight;

    public MySoftViewPager(Context context) {
        super(context);
    }

    public MySoftViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels)
    {

        // Filter it
        float effectOffset = isSmall(positionOffset) ? 0 : positionOffset;

        mLeft = findViewFromObject(position);
        mRight = findViewFromObject(position + 1);

        // Add effects
        animateStack(mLeft, mRight, effectOffset, positionOffsetPixels);
        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    public void setObjectForPosition(View view, int position)
    {
        mChildrenViews.put(position, view);
    }

    /**
     * get View by position
     * @param position
     * @return
     */
    public View findViewFromObject(int position)
    {
        return mChildrenViews.get(position);
    }

    private boolean isSmall(float positionOffset)
    {
        return Math.abs(positionOffset) < 0.0001;
    }

    protected void animateStack(View left, View right, float effectOffset,
                                int positionOffsetPixels)
    {
        if (right != null)
        {
            mScale = (1 - SCALE_MAX) * effectOffset + SCALE_MAX;
            mTrans = -getWidth() - getPageMargin() + positionOffsetPixels;
            ViewHelper.setScaleX(right, mScale);
            ViewHelper.setScaleY(right, mScale);
            ViewHelper.setTranslationX(right, mTrans);
        }
        if (left != null)
        {
            left.bringToFront();
        }
    }
}
