package com.example.ytw.mystriptab;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/**
 * @Date 创建时间:  2017/8/18
 * @Author: YTW
 * @Description:
 **/

public class TabStrip extends View {

    //
    private final static int HIGH_QUALITY_FLAGS = Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG;
    private final static String PREVIEW_TITLE = "Title";
    private final static int INVALID_INDEX = -1;

    private final static int DEFAULT_ANIMATION_DURATION = 350;
    private final static float DEFAULT_STRIP_FACTOR = 2.5F;
    private final static float DEFAULT_STRIP_WEIGHT = 10.0F;
    private final static float DEFAULT_CORENR_RADIUS = 5.0F;
    private final static int DEFAULT_INACTIVE_COLOR = Color.GRAY;
    private final static int DEFAULT_ACTIVE_COLOR = Color.WHITE;
    private final static int DEFAULT_STRIP_COLOR = Color.RED;
    private final static int DEFAULT_TITLE_SIZE = 0;

    private final static float TITLE_SIZE_FRACTION = 0.35F;

    private final static float MIN_FRACTION = 0.0F;
    private final static float MAX_FRACTION = 1.0F;

    private final RectF mBounds = new RectF();
    private final RectF mStripBounds = new RectF();
    private final Rect mTitleBounds = new Rect();

    //main paint
    private final Paint mStripPaint = new Paint(HIGH_QUALITY_FLAGS) {
        {
            setStyle(Style.FILL);
        }
    };

    private final Paint mTitlePaint = new TextPaint(HIGH_QUALITY_FLAGS) {
        {
            setTextAlign(Align.CENTER);
        }
    };

    //动画变量
    private final ValueAnimator mAnimator = new ValueAnimator();
    private final ArgbEvaluator mColorEvaluator = new ArgbEvaluator();
    private final ResizeInterpolator mResizeInterpolator = new ResizeInterpolator();
    private int mAnimationDuration;

    //title
    private String[] mTitles;

    //ViewPager 相关变量
    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private int mScrollState;

    //Tab Listener
    private OnTabStripSelectedIndexListener mOnTabStripSelectedIndexListener;
    private ValueAnimator.AnimatorListener mAnimatorListener;

    //tab大小
    private float mTabSize;

    //标题的size 和 margin
    private float mTitleSize;

    //Strip Type And gravity
    private StripType mStripType;
    private StripGravity mStripGravity;

    //
    private float mStripWeight;
    private float mCornersRadius;

    //indexes
    private int mLastIndex = INVALID_INDEX;
    private int mIndex = INVALID_INDEX;

    //
    private float mFraction;

    //strip坐标
    private float mStartStripX;
    private float mEndStripX;
    private float mStripLeft;
    private float mStripRight;

    //检测是否为条形模式或指示器寻呼模式
    private boolean mIsViewPagerMode;
    //检测是否从左到右
    private boolean mIsResizeIn;
    //检测是否按下
    private boolean mIsActionDown;
    // Detect if we get action down event on strip
    private boolean mIsTabActionDown;
    //当我们从标签栏和ViewPager设置索引时检测
    private boolean mIsSetIndexFromTabBar;

    //Color 变量
    private int mInactiveColor;
    private int mActiveColor;

    // 自定义 typeface
    private Typeface mTypeface;

    public TabStrip(Context context) {
        this(context, null);
    }

    public TabStrip(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabStrip(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //初始化
        setWillNotDraw(false);
        //Speed and fix for pre 17 API
        ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, null);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabStrip);

        setStripColor(typedArray.getColor(R.styleable.TabStrip_tab_color, DEFAULT_STRIP_COLOR));

        setTitleSize(typedArray.getDimension(R.styleable.TabStrip_tab_size, DEFAULT_TITLE_SIZE));

        setStripWeight(
            typedArray.getDimension(R.styleable.TabStrip_tab_weight, DEFAULT_STRIP_WEIGHT));

        setStripFactor(typedArray.getFloat(R.styleable.TabStrip_tab_factor, DEFAULT_STRIP_FACTOR));

        setStripType(typedArray.getInt(R.styleable.TabStrip_tab_type, StripType.LINE_INDEX));

        setStripGravity(
            typedArray.getInt(R.styleable.TabStrip_tab_gravity, StripGravity.BOTTOM_INDEX));

        setTypeface(typedArray.getString(R.styleable.TabStrip_tab_typeface));

        setInactiveColor(
            typedArray.getColor(R.styleable.TabStrip_tab_inactive_color, DEFAULT_INACTIVE_COLOR));

        setActiveColor(
            typedArray.getColor(R.styleable.TabStrip_tab_active_color, DEFAULT_ACTIVE_COLOR));

        setAnimationDuration(typedArray.getInteger(R.styleable.TabStrip_tab_animation_duration,
            DEFAULT_ANIMATION_DURATION));

        setCornersRadius(typedArray.getDimension(R.styleable.TabStrip_tab_corners_radius,
            DEFAULT_CORENR_RADIUS));

        //get title
        String[] titles = null;
        try {
            final int titlesResId = typedArray.getResourceId(R.styleable.TabStrip_tab_title, 0);
            titles =
                titlesResId == 0 ? null : typedArray.getResources().getStringArray(titlesResId);
        } catch (Exception e) {
            titles = null;
            e.printStackTrace();
        } finally {
            if (titles == null) {
                if (isInEditMode()) {
                    titles = new String[new Random().nextInt(5) + 1];
                    Arrays.fill(titles, PREVIEW_TITLE);
                } else {
                    titles = new String[0];
                }
            }
            setTitles(titles);
        }

        //初始化动画
        mAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateIndicatorPosition((float) valueAnimator.getAnimatedValue());
            }
        });

        typedArray.recycle();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // get measure size
        final float width = MeasureSpec.getSize(widthMeasureSpec);
        final float height = MeasureSpec.getSize(heightMeasureSpec);

        // set bounds for nts
        mBounds.set(0.0f, 0.0f, width, height);

        if (mTitles.length == 0 || width == 0 || height == 0) return;

        // get smaller side
        mTabSize = width / (float) mTitles.length;
        if ((int) mTitleSize == DEFAULT_TITLE_SIZE) {
            setTitleSize((height - mStripWeight) * TITLE_SIZE_FRACTION);
        }

        //set start position of strip for preview or on  start
        if (isInEditMode() || !mIsViewPagerMode) {
            mIsSetIndexFromTabBar = true;

            // set random in preview mode
            if (isInEditMode()) {
                mIndex = new Random().nextInt(mTitles.length);
            }

            mStartStripX =
                (mIndex * mTabSize) + (mStripType == StripType.POINT ? mTabSize * 0.5f : 0.0f);
            mEndStripX = mStartStripX;
            updateIndicatorPosition(MAX_FRACTION);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        // Set bound of strip
        mStripBounds.set(mStripLeft - (mStripType == StripType.POINT ? mStripWeight * 0.5f : 0.0f),
            mStripGravity == StripGravity.BOTTOM ? mBounds.height() - mStripWeight : 0.0f,
            mStripRight - (mStripType == StripType.POINT ? mStripWeight * 0.5f : 0.0f),
            mStripGravity == StripGravity.BOTTOM ? mBounds.height() : mStripWeight);

        // Draw strip
        if (mCornersRadius == 0) {
            canvas.drawRect(mStripBounds, mStripPaint);
        } else {
            canvas.drawRoundRect(mStripBounds, mCornersRadius, mCornersRadius, mStripPaint);
        }

        // draw tab titles
        for (int i = 0; i < mTitles.length; i++) {
            final String title = mTitles[i];

            final float leftTitleOffset = (mTabSize * i) + (mTabSize * 0.5f);

            mTitlePaint.getTextBounds(title, 0, title.length(), mTitleBounds);
            final float topTitleOffset =
                (mBounds.height() - mStripWeight) * 0.5f + mTitleBounds.height() * 0.5f
                    - mTitleBounds.bottom;

            //get interpolater fraction for left last and current tab
            final float interpolation = mResizeInterpolator.getResizeInterpolation(mFraction, true);
            final float lastInterpolation =
                mResizeInterpolator.getResizeInterpolation(mFraction, false);

            if (mIsSetIndexFromTabBar) {
                if (mIndex == i) {
                    updateCurrentTitle(interpolation);
                } else if (mLastIndex == i) {
                    updateLastTitle(lastInterpolation);
                } else {
                    updateInactiveTitle();
                }
            } else {
                if (i != mIndex && i != mIndex + 1) {
                    updateInactiveTitle();
                } else if (i == mIndex + 1) {
                    updateCurrentTitle(interpolation);
                } else if (i == mIndex) {
                    updateLastTitle(lastInterpolation);
                }
            }

            canvas.drawText(title, leftTitleOffset,
                topTitleOffset + (mStripGravity == StripGravity.TOP ? mStripWeight : 0.0f),
                mTitlePaint

            );
        }
    }

    private void updateInactiveTitle() {

    }

    private void updateLastTitle(float lastInterpolation) {

    }

    private void updateCurrentTitle(float interpolation) {

    }

    private void updateIndicatorPosition(float fraction) {
        //update general fraction
        mFraction = fraction;

        //设置strip左边的坐标
        mStripLeft =
            mStartStripX + (mResizeInterpolator.getResizeInterpolation(fraction, mIsResizeIn) * (
                mEndStripX
                    - mStartStripX));

        //设置strip右边的坐标
        mStripRight = (mStartStripX + (mStripType == StripType.LINE ? mTabSize : mStripWeight)) + (
            mResizeInterpolator.getResizeInterpolation(fraction, !mIsResizeIn)
                * (mEndStripX - mStartStripX));

        postInvalidate();
    }

    private void setTitles(String[] titles) {
        for (int i = 0; i < titles.length; i++) {
            titles[i] = titles[i].toUpperCase();
        }
        mTitles = titles;
        requestLayout();
    }

    private void setCornersRadius(float cornersRadius) {
        mCornersRadius = cornersRadius;
        postInvalidate();
    }

    private void setAnimationDuration(int duration) {
        mAnimationDuration = duration;
        mAnimator.setDuration(mAnimationDuration);
        resetScroller();
    }

    /**
     * 重新启动滚动条并重置滚动时间等于动画持续时间
     */
    private void resetScroller() {
        if (mViewPager == null) {
            return;
        }
        try {
            final Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            final ResizeViewPagerScroller scroller = new ResizeViewPagerScroller(getContext());
            scrollerField.set(mViewPager, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveColor(int color) {
        mActiveColor = color;
        postInvalidate();
    }

    private void setInactiveColor(int color) {
        mInactiveColor = color;
        postInvalidate();
    }

    private void setTypeface(String typeface) {
        if (TextUtils.isEmpty(typeface)) {
            return;
        }

        Typeface tempTypeface;
        try {
            tempTypeface = Typeface.createFromAsset(getContext().getAssets(), typeface);
        } catch (Exception e) {
            tempTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
            e.printStackTrace();
        }
        setTypeface(tempTypeface);
    }

    private void setTypeface(Typeface typeface) {
        mTypeface = typeface;
        mTitlePaint.setTypeface(typeface);
        postInvalidate();
    }

    private void setStripGravity(int gravity) {
        switch (gravity) {
            case StripGravity.TOP_INDEX:
                setStripGravity(StripGravity.TOP);
                break;
            case StripGravity.BOTTOM_INDEX:
            default:
                setStripGravity(StripGravity.BOTTOM);
                break;
        }
    }

    public void setStripGravity(final StripGravity stripGravity) {
        mStripGravity = stripGravity;
        requestLayout();
    }

    private void setStripType(int type) {
        switch (type) {
            case StripType.POINT_INDEX:
                setStripType(StripType.POINT);
                break;
            case StripType.LINE_INDEX:
            default:
                setStripType(StripType.LINE);
                break;
        }
    }

    public void setStripType(final StripType stripType) {
        mStripType = stripType;
        requestLayout();
    }

    private void setStripFactor(float factor) {
        mResizeInterpolator.setFactor(factor);
    }

    private void setStripWeight(float weight) {
        mStripWeight = weight;
        requestLayout();
    }

    private void setTitleSize(float titleSize) {
        mTitleSize = titleSize;
        mStripPaint.setTextSize(titleSize);
        postInvalidate();
    }

    private void setStripColor(int color) {
        mStripPaint.setColor(color);
        postInvalidate();
    }

    private static class ResizeInterpolator implements Interpolator {

        private float mFactor;

        private boolean mResizeIn;

        public float getFactor() {
            return mFactor;
        }

        public void setFactor(float factor) {
            mFactor = factor;
        }

        @Override public float getInterpolation(float input) {
            if (mResizeIn) {
                return (float) (1.0F - Math.pow((1.0F - input), 2.0F * mFactor));
            } else {
                return (float) (Math.pow(input, 2.0F * mFactor));
            }
        }

        public float getResizeInterpolation(final float input, final boolean resizeIn) {
            mResizeIn = resizeIn;
            return getInterpolation(input);
        }
    }

    public enum StripType {
        LINE, POINT;
        private final static int LINE_INDEX = 0;
        private final static int POINT_INDEX = 1;
    }

    public enum StripGravity {
        BOTTOM, TOP;

        private final static int BOTTOM_INDEX = 0;
        private final static int TOP_INDEX = 1;
    }

    private class ResizeViewPagerScroller extends Scroller {

        public ResizeViewPagerScroller(Context context) {
            super(context, new AccelerateDecelerateInterpolator());
        }

        @Override public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mAnimationDuration);
        }

        @Override public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mAnimationDuration);
        }
    }

    public interface OnTabStripSelectedIndexListener {

        void onStartTabSelected(final String title, final int index);

        void onEndTabSelected(final String title, final int index);
    }
}
