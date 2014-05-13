/*
 * Copyright (C) 2013 Roy Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dolphinwang.imagecoverflow;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.Scroller;

/**
 * 
 * @author dolphinWang
 * @time 2013-11-29
 * 
 */
public class CoverFlowView<T extends CoverFlowAdapter> extends View {

    public enum CoverFlowGravity {
        TOP, BOTTOM, CENTER_VERTICAL;
    };

    public enum CoverFlowLayoutMode {
        MATCH_PARENT, WRAP_CONTENT;
    }

    /**** static field ****/
    private static final int DURATION = 200;

    protected final int INVALID_POSITION = -1;

    // Used to indicate a no preference for a position type.
    static final int NO_POSITION = -1;

    // the visible views left and right
    protected int VISIBLE_VIEWS = 3;

    // space between each two of children
    protected final int CHILD_SPACING = -200;

    // 基础alphaֵ
    private final int ALPHA_DATUM = 76;
    private int STANDARD_ALPHA;
    // 基础缩放值
    private static final float CARD_SCALE = 0.15f;
    private static float MOVE_POS_MULTIPLE = 3.0f;
    private static final int TOUCH_MINIMUM_MOVE = 5;
    private static final float MOVE_SPEED_MULTIPLE = 1;
    private static final float MAX_SPEED = 6.0f;
    private static final float FRICTION = 10.0f;

    private static final int LONG_CLICK_DELAY = ViewConfiguration
            .getLongPressTimeout();
    /**** static field ****/

    private RecycleBin mRecycler;
    protected int mCoverFlowCenter;
    private T mAdapter;

    private int mVisibleChildCount;
    private int mItemCount;

    /**
     * True if the data has changed since the last layout
     */
    boolean mDataChanged;

    protected CoverFlowGravity mGravity;

    protected CoverFlowLayoutMode mLayoutMode;

    private Rect mCoverFlowPadding;

    private PaintFlagsDrawFilter mDrawFilter;

    private Matrix mChildTransfromMatrix;

    private Paint mDrawChildPaint;

    private RectF mTouchRect;

    private int mWidth;
    private boolean mTouchMoved;
    private float mTouchStartPos;
    private float mTouchStartX;
    private float mTouchStartY;

    private float mOffset;
    private int mLastOffset;

    private float mStartOffset;
    private long mStartTime;

    private float mStartSpeed;
    private float mDuration;
    private Runnable mAnimationRunnable;
    private VelocityTracker mVelocity;

    private int mChildHeight;
    private int mChildTranslateY;

    private boolean reflectEnable = false;
    private boolean reflectShaderEnable = true;
    private float reflectHeightFraction;
    private int reflectGap;

    private boolean topImageClickEnable;

    private CoverFlowListener<T> mCoverFlowListener;

    private TopImageLongClickListener mLongClickListener;
    private LongClickRunnable mLongClickRunnable;
    private boolean mLongClickPosted;
    private boolean mLongClickTriggled;

    private int mTopImageIndex;

    private Scroller mScroller;

    /**
     * Record origin width and height of images
     */
    private SparseArray<int[]> mImageRecorder;

    public CoverFlowView(Context context) {
        super(context);
        init();
    }

    public CoverFlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
        init();
    }

    public CoverFlowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ImageCoverFlowView);

        int totalVisibleChildren = a.getInt(
                R.styleable.ImageCoverFlowView_visibleImage, 2);
        if (totalVisibleChildren % 2 == 0) {
            throw new IllegalArgumentException(
                    "visible image must be an odd number");
        }

        VISIBLE_VIEWS = totalVisibleChildren >> 1;

        reflectEnable = a.getBoolean(
                R.styleable.ImageCoverFlowView_enableReflection, false);
        topImageClickEnable = a.getBoolean(
                R.styleable.ImageCoverFlowView_topImageClickEnable, true);
        if (reflectEnable) {
            reflectHeightFraction = a.getFraction(
                    R.styleable.ImageCoverFlowView_reflectionHeight, 100, 0,
                    0.3f);
            if (reflectHeightFraction > 100)
                reflectHeightFraction = 100;
            reflectHeightFraction /= 100;
            reflectGap = a.getDimensionPixelSize(
                    R.styleable.ImageCoverFlowView_reflectionGap, 0);
            reflectShaderEnable = a
                    .getBoolean(
                            R.styleable.ImageCoverFlowView_reflectionShaderEnable,
                            true);
        }

        mGravity = CoverFlowGravity.values()[a.getInt(
                R.styleable.ImageCoverFlowView_coverflowGravity,
                CoverFlowGravity.CENTER_VERTICAL.ordinal())];

        mLayoutMode = CoverFlowLayoutMode.values()[a.getInt(
                R.styleable.ImageCoverFlowView_coverflowLayoutMode,
                CoverFlowLayoutMode.WRAP_CONTENT.ordinal())];

        a.recycle();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(true);

        mChildTransfromMatrix = new Matrix();

        mTouchRect = new RectF();

        mImageRecorder = new SparseArray<int[]>();

        mDrawChildPaint = new Paint();
        mDrawChildPaint.setAntiAlias(true);
        mDrawChildPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mCoverFlowPadding = new Rect();

        mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                | Paint.FILTER_BITMAP_FLAG);

        mScroller = new Scroller(getContext(),
                new AccelerateDecelerateInterpolator());
    }

    /**
     * if subclass override this method, should call super method.
     * 
     * @param adapter
     *            extends CoverFlowAdapter
     */
    public void setAdapter(T adapter) {
        mAdapter = adapter;

        if (mAdapter != null) {
            mItemCount = mAdapter.getCount();
            if (mItemCount < (VISIBLE_VIEWS << 1) + 1) {
                throw new IllegalArgumentException(
                        "total count in adapter must larger than visible images!");
            }

            mRecycler = new RecycleBin();
        }

        resetList();

        requestLayout();
    }

    public T getAdapter() {
        return mAdapter;
    }

    public void setCoverFlowListener(CoverFlowListener<T> l) {
        mCoverFlowListener = l;
    }

    private void resetList() {
        if (mRecycler != null) {

            mRecycler.clear();
        }

        mChildHeight = 0;

        mOffset = 0;
        mLastOffset = -1;

        STANDARD_ALPHA = (255 - ALPHA_DATUM) / VISIBLE_VIEWS;

        if (mGravity == null) {
            mGravity = CoverFlowGravity.CENTER_VERTICAL;
        }

        if (mLayoutMode == null) {
            mLayoutMode = CoverFlowLayoutMode.WRAP_CONTENT;
        }

        mImageRecorder.clear();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * 这个函数除了计算父控件的宽高之外，最重要的是计算出出现在屏幕上的图片的宽高
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mCoverFlowPadding.left = getPaddingLeft();
        mCoverFlowPadding.right = getPaddingRight();
        mCoverFlowPadding.top = getPaddingTop();
        mCoverFlowPadding.bottom = getPaddingBottom();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int visibleCount = (VISIBLE_VIEWS << 1) + 1;

        int avaiblableHeight = heightSize - mCoverFlowPadding.top
                - mCoverFlowPadding.bottom;

        int maxChildHeight = 0;

        for (int i = 0; i < visibleCount; ++i) {
            int childHeight = obtainImage(i).getHeight();

            maxChildHeight = (maxChildHeight < childHeight) ? childHeight
                    : maxChildHeight;
        }

        if (heightMode == MeasureSpec.EXACTLY
                || heightMode == MeasureSpec.AT_MOST) {
            // if height which parent provided is less than child need, scale
            // child height to parent provide
            if (avaiblableHeight < maxChildHeight) {
                mChildHeight = avaiblableHeight;
            } else {
                // if larger than, depends on layout mode
                // if layout mode is match_parent, scale child height to parent
                // provide
                if (mLayoutMode == CoverFlowLayoutMode.MATCH_PARENT) {
                    mChildHeight = avaiblableHeight;
                    // if layout mode is wrap_content, keep child's original
                    // height
                } else if (mLayoutMode == CoverFlowLayoutMode.WRAP_CONTENT) {
                    mChildHeight = maxChildHeight;

                    // adjust parent's height
                    if (heightMode == MeasureSpec.AT_MOST) {
                        heightSize = mChildHeight + mCoverFlowPadding.top
                                + mCoverFlowPadding.bottom;
                    }
                }
            }
        } else {
            // height mode is unspecified
            if (mLayoutMode == CoverFlowLayoutMode.MATCH_PARENT) {
                mChildHeight = avaiblableHeight;
            } else if (mLayoutMode == CoverFlowLayoutMode.WRAP_CONTENT) {
                mChildHeight = maxChildHeight;

                // adjust parent's height
                heightSize = mChildHeight + mCoverFlowPadding.top
                        + mCoverFlowPadding.bottom;
            }
        }

        // 根据gravity调整y轴位移
        if (mGravity == CoverFlowGravity.CENTER_VERTICAL) {
            mChildTranslateY = (heightSize >> 1) - (mChildHeight >> 1);
        } else if (mGravity == CoverFlowGravity.TOP) {
            mChildTranslateY = mCoverFlowPadding.top;
        } else if (mGravity == CoverFlowGravity.BOTTOM) {
            mChildTranslateY = heightSize - mCoverFlowPadding.bottom
                    - mChildHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
        mVisibleChildCount = visibleCount;
        mWidth = widthSize;
    }

    /**
     * subclass should never override this method, because all of child will
     * draw on the canvas directly
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.setDrawFilter(mDrawFilter);

        final float offset = mOffset;
        int i = 0;
        int mid = (int) Math.floor(offset + 0.5);

        int rightChild = (mVisibleChildCount % 2 == 0) ? (mVisibleChildCount >> 1) - 1
                : mVisibleChildCount >> 1;
        int leftChild = mVisibleChildCount >> 1;

        // draw the left children
        int startPos = mid - leftChild;
        for (i = startPos; i < mid; ++i) {
            drawChild(canvas, i, i - offset);
        }

        // draw the right children
        int endPos = mid + rightChild;
        for (i = endPos; i >= mid; --i) {
            drawChild(canvas, i, i - offset);
        }

        if (mLastOffset != (int) offset) {
            imageOnTop(getActuallyPosition((int) offset));
            mLastOffset = (int) offset;
        }

        super.onDraw(canvas);
    }

    protected final void drawChild(Canvas canvas, int position, float offset) {

        int actuallyPosition = getActuallyPosition(position);
        final Bitmap child = obtainImage(actuallyPosition);

        int[] wAndh = mImageRecorder.get(actuallyPosition);
        if (wAndh == null) {
            wAndh = new int[] { child.getWidth(), child.getHeight() };
            mImageRecorder.put(actuallyPosition, wAndh);
        } else {
            wAndh[0] = child.getWidth();
            wAndh[1] = child.getHeight();
        }

        if (child != null && !child.isRecycled() && canvas != null) {

            makeChildTransfromMatrix(mChildTransfromMatrix, mDrawChildPaint,
                    child, position, offset);

            canvas.drawBitmap(child, mChildTransfromMatrix, mDrawChildPaint);
        }
    }

    /**
     * <ul>
     * <li>对bitmap进行伪3d变换</li>
     * </ul>
     * 
     * @param childTransfromMatrix
     * @param mDrawChildPaint
     * @param child
     * @param position
     * @param offset
     */
    private void makeChildTransfromMatrix(Matrix childTransfromMatrix,
            Paint mDrawChildPaint, Bitmap child, int position, float offset) {
        mChildTransfromMatrix.reset();

        float scale = 1 - Math.abs(offset) * CARD_SCALE;
        // 延x轴移动的距离应该根据center图片决定
        float translateX = 0;

        final float originalChildHeightScale = (float) mChildHeight
                / child.getHeight();
        final float childHeightScale = originalChildHeightScale * scale;
        final int childWidth = (int) (child.getWidth() * childHeightScale);
        final int centerChildWidth = (int) (child.getWidth() * originalChildHeightScale);
        int leftSpace = ((mWidth >> 1) - mCoverFlowPadding.left)
                - (centerChildWidth >> 1);
        int rightSpace = (((mWidth >> 1) - mCoverFlowPadding.right) - (centerChildWidth >> 1));

        if (offset <= 0)
            translateX = ((float) leftSpace / VISIBLE_VIEWS)
                    * (VISIBLE_VIEWS + offset) + mCoverFlowPadding.left;

        else
            translateX = mWidth - ((float) rightSpace / VISIBLE_VIEWS)
                    * (VISIBLE_VIEWS - offset) - childWidth
                    - mCoverFlowPadding.right;

        float alpha = (float) 255 - Math.abs(offset) * STANDARD_ALPHA;

        if (alpha < 0) {
            alpha = 0;
        } else if (alpha > 255) {
            alpha = 255;
        }

        mDrawChildPaint.setAlpha((int) alpha);

        mChildTransfromMatrix.preTranslate(0, -(child.getHeight() >> 1));
        // matrix中的postxxx为顺序执行，相反prexxx为倒叙执行
        mChildTransfromMatrix.postScale(childHeightScale, childHeightScale);

        // if actually child height is larger or less than original child height
        // need to change translate distance of y-axis
        float adjustedChildTranslateY = 0;
        if (childHeightScale != 1) {
            adjustedChildTranslateY = (mChildHeight - child.getHeight()) >> 1;
        }

        mChildTransfromMatrix.postTranslate(translateX, mChildTranslateY
                + adjustedChildTranslateY);

        // Log.d(VIEW_LOG_TAG, "position= " + position + " mChildTranslateY= "
        // + mChildTranslateY + adjustedChildTranslateY);

        getCustomTransformMatrix(mChildTransfromMatrix, mDrawChildPaint, child,
                position, offset);

        mChildTransfromMatrix.postTranslate(0, (child.getHeight() >> 1));
    }

    /**
     * <ul>
     * <li>This is an empty method.</li>
     * <li>Giving user a chance to make more transform base on standard.</li>
     * </ul>
     * 
     * @param childTransfromMatrix
     *            matrix to make transform
     * @param mDrawChildPaint
     *            paint, user can set alpha
     * @param child
     *            bitmap to draw
     * @param position
     * @param offset
     *            offset to center(zero)
     */
    protected void getCustomTransformMatrix(Matrix childTransfromMatrix,
            Paint mDrawChildPaint, Bitmap child, int position, float offset) {

        /** example code to make image y-axis rotation **/
        // Camera c = new Camera();
        // c.save();
        // Matrix m = new Matrix();
        // c.rotateY(10 * (-offset));
        // c.getMatrix(m);
        // c.restore();
        // m.preTranslate(-(child.getWidth() >> 1), -(child.getHeight() >> 1));
        // m.postTranslate(child.getWidth() >> 1, child.getHeight() >> 1);
        // mChildTransfromMatrix.preConcat(m);
    }

    private void imageOnTop(int position) {
        mTopImageIndex = position;

        final int[] wAndh = mImageRecorder.get(position);

        final int tempReflectHeight = (int) (wAndh[1] * reflectHeightFraction);
        final float childScale = (float) mChildHeight
                / (wAndh[1] + tempReflectHeight + reflectGap);
        final int childHeight = (int) (mChildHeight * childScale - childScale
                * tempReflectHeight - childScale * reflectGap);
        final int childWidth = (int) (childScale * wAndh[0]);

        Log.e(VIEW_LOG_TAG, "height ==>" + childHeight + " width ==>"
                + childWidth);

        mTouchRect.left = (mWidth >> 1) - (childWidth >> 1);
        mTouchRect.top = mChildTranslateY;
        mTouchRect.right = mTouchRect.left + childWidth;
        mTouchRect.bottom = mTouchRect.top + childHeight;

        Log.e(VIEW_LOG_TAG, "rect==>" + mTouchRect);

        if (mCoverFlowListener != null) {
            mCoverFlowListener.imageOnTop(this, position, mTouchRect.left,
                    mTouchRect.top, mTouchRect.right, mTouchRect.bottom);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        int action = event.getAction();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (mScroller.computeScrollOffset()) {
                mScroller.abortAnimation();
                invalidate();
            }
            stopLongClick();
            triggleLongClick(event.getX(), event.getY());
            touchBegan(event);
            return true;
        case MotionEvent.ACTION_MOVE:
            touchMoved(event);
            return true;
        case MotionEvent.ACTION_UP:
            touchEnded(event);
            stopLongClick();
            return true;
        }

        return false;
    }

    private void triggleLongClick(float x, float y) {
        if (mTouchRect.contains(x, y) && mLongClickListener != null
                && topImageClickEnable && !mLongClickPosted) {
            final int actuallyPosition = mTopImageIndex;

            mLongClickRunnable.setPosition(actuallyPosition);
            postDelayed(mLongClickRunnable, LONG_CLICK_DELAY);
        }
    }

    private void stopLongClick() {
        if (mLongClickRunnable != null) {
            removeCallbacks(mLongClickRunnable);
            mLongClickPosted = false;
            mLongClickTriggled = false;
        }
    }

    private void touchBegan(MotionEvent event) {
        endAnimation();

        float x = event.getX();
        mTouchStartX = x;
        mTouchStartY = event.getY();
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mStartOffset = mOffset;

        mTouchMoved = false;

        mTouchStartPos = (x / mWidth) * MOVE_POS_MULTIPLE - 5;
        mTouchStartPos /= 2;

        mVelocity = VelocityTracker.obtain();
        mVelocity.addMovement(event);
    }

    private void touchMoved(MotionEvent event) {
        float pos = (event.getX() / mWidth) * MOVE_POS_MULTIPLE - 5;
        pos /= 2;

        if (!mTouchMoved) {
            float dx = Math.abs(event.getX() - mTouchStartX);
            float dy = Math.abs(event.getY() - mTouchStartY);

            if (dx < TOUCH_MINIMUM_MOVE && dy < TOUCH_MINIMUM_MOVE)
                return;

            mTouchMoved = true;

            stopLongClick();
        }

        mOffset = mStartOffset + mTouchStartPos - pos;

        invalidate();
        mVelocity.addMovement(event);
    }

    private void touchEnded(MotionEvent event) {
        float pos = (event.getX() / mWidth) * MOVE_POS_MULTIPLE - 5;
        pos /= 2;

        if (mTouchMoved || (mOffset - Math.floor(mOffset)) != 0) {
            mStartOffset += mTouchStartPos - pos;
            mOffset = mStartOffset;

            mVelocity.addMovement(event);

            mVelocity.computeCurrentVelocity(1000);
            double speed = mVelocity.getXVelocity();

            speed = (speed / mWidth) * MOVE_SPEED_MULTIPLE;
            if (speed > MAX_SPEED)
                speed = MAX_SPEED;
            else if (speed < -MAX_SPEED)
                speed = -MAX_SPEED;

            startAnimation(-speed);
        } else {
            Log.e(VIEW_LOG_TAG,
                    " touch ==>" + event.getX() + " , " + event.getY());
            if (mTouchRect != null) {
                if (mTouchRect.contains(event.getX(), event.getY())
                        && mCoverFlowListener != null && topImageClickEnable
                        && !mLongClickTriggled) {
                    final int actuallyPosition = mTopImageIndex;

                    mCoverFlowListener.topImageClicked(this, actuallyPosition);
                }
            }
        }

        mVelocity.clear();
        mVelocity.recycle();
    }

    private void startAnimation(double speed) {
        if (mAnimationRunnable != null)
            return;

        double delta = speed * speed / (FRICTION * 2);
        if (speed < 0)
            delta = -delta;

        double nearest = mStartOffset + delta;
        nearest = Math.floor(nearest + 0.5);

        mStartSpeed = (float) Math.sqrt(Math.abs(nearest - mStartOffset)
                * FRICTION * 2);
        if (nearest < mStartOffset)
            mStartSpeed = -mStartSpeed;

        mDuration = Math.abs(mStartSpeed / FRICTION);
        mStartTime = AnimationUtils.currentAnimationTimeMillis();

        mAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                driveAnimation();
            }
        };
        post(mAnimationRunnable);
    }

    private void driveAnimation() {
        float elapsed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime) / 1000.0f;
        if (elapsed >= mDuration)
            endAnimation();
        else {
            updateAnimationAtElapsed(elapsed);
            post(mAnimationRunnable);
        }
    }

    private void endAnimation() {
        if (mAnimationRunnable != null) {
            mOffset = (float) Math.floor(mOffset + 0.5);

            invalidate();

            removeCallbacks(mAnimationRunnable);
            mAnimationRunnable = null;
        }
    }

    private void updateAnimationAtElapsed(float elapsed) {
        if (elapsed > mDuration)
            elapsed = mDuration;

        float delta = Math.abs(mStartSpeed) * elapsed - FRICTION * elapsed
                * elapsed / 2;
        if (mStartSpeed < 0)
            delta = -delta;

        mOffset = mStartOffset + delta;
        invalidate();
    }

    /**
     * Invalidate image on special position
     * 
     * @param position
     */
    public void invalidatePosition(int position) {
        if (mAdapter == null || position < 0) {
            return;
        }

        final int count = mAdapter.getCount();

        if (position >= count) {
            return;
        }

        mRecycler.removeCachedBitmap(position);

        // If bitmap want to invalidate is showing on screen
        // call view to redraw
        if (position >= mTopImageIndex - VISIBLE_VIEWS
                && position <= mTopImageIndex + VISIBLE_VIEWS) {
            invalidate();
        }
    }

    /**
     * Convert draw-index to index in adapter
     * 
     * @param position
     *            position to draw
     * @return
     */
    private int getActuallyPosition(int position) {
        if (mAdapter == null) {
            return INVALID_POSITION;
        }

        int max = mAdapter.getCount();

        position += VISIBLE_VIEWS;
        while (position < 0 || position >= max) {
            if (position < 0) {
                position += max;
            } else if (position >= max) {
                position -= max;
            }
        }

        return position;
    }

    private Bitmap obtainImage(int position) {
        Bitmap bitmap = mRecycler.getCachedBitmap(position);

        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = mAdapter.getImage(position);

            if (reflectEnable) {
                Bitmap bitmapWithReflect = BitmapUtils.createReflectedBitmap(
                        bitmap, reflectHeightFraction, reflectGap,
                        reflectShaderEnable);

                if (bitmapWithReflect != null) {
                    mRecycler.addBitmap2Cache(position, bitmapWithReflect);

                    return bitmapWithReflect;
                } else {
                    mRecycler.addBitmap2Cache(position, bitmap);
                }
            } else {

                mRecycler.addBitmap2Cache(position, bitmap);

            }

        }

        return bitmap;
    }

    /**
     * user is better to set a density of screen to make picture's movement more
     * comfortable
     * 
     * @param density
     */
    public void setScreenDensity(int density) {
        float temp = MOVE_POS_MULTIPLE * density;
        MOVE_POS_MULTIPLE = temp;
    }

    public void setVisibleImage(int count) {
        if (count % 2 == 0) {
            throw new IllegalArgumentException(
                    "visible image must be an odd number");
        }

        VISIBLE_VIEWS = count / 2;
        STANDARD_ALPHA = (255 - ALPHA_DATUM) / VISIBLE_VIEWS;
    }

    public void setCoverFlowGravity(CoverFlowGravity gravity) {
        mGravity = gravity;
    }

    public void setCoverFlowLayoutMode(CoverFlowLayoutMode mode) {
        mLayoutMode = mode;
    }

    public void enableReflection(boolean enable) {
        this.reflectEnable = enable;
    }

    public void enableReflectionShader(boolean enable) {
        reflectShaderEnable = enable;
    }

    public void setReflectionHeight(int fraction) {
        if (fraction < 0)
            fraction = 30;
        else if (fraction > 100)
            fraction = 100;

        reflectHeightFraction = fraction;
    }

    public void setReflectionGap(int gap) {
        if (gap < 0)
            gap = 0;

        reflectGap = gap;
    }

    public void disableTopImageClick() {
        topImageClickEnable = false;
    }

    public void enableTopImageClick() {
        topImageClickEnable = true;
    }

    public void setSelection(int position) {
        final int max = mAdapter.getCount();
        if (position < 0 || position >= max) {
            throw new IllegalArgumentException(
                    "Position want to select can not less than 0 or larger than max of adapter provide!");
        }

        if (mTopImageIndex != position) {
            if (mScroller.computeScrollOffset()) {
                mScroller.abortAnimation();
            }

            final int from = (int) (mOffset * 100);
            final int disX = (int) ((position - VISIBLE_VIEWS) * 100) - from;
            mScroller.startScroll(
                    from,
                    0,
                    disX,
                    0,
                    DURATION
                            * Math.min(
                                    Math.abs(position + max - mTopImageIndex),
                                    Math.abs(position - mTopImageIndex)));

            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            final int currX = mScroller.getCurrX();

            mOffset = (float) currX / 100;

            invalidate();
        }
    }

    public void setTopImageLongClickListener(TopImageLongClickListener listener) {
        mLongClickListener = listener;

        if (listener == null) {
            mLongClickRunnable = null;
        } else {
            if (mLongClickRunnable == null) {
                mLongClickRunnable = new LongClickRunnable();
            }
        }
    }

    private class LongClickRunnable implements Runnable {
        private int position;

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public void run() {
            if (mLongClickListener != null) {
                mLongClickListener.onLongClick(position);
                mLongClickTriggled = true;
            }
        }
    }

    class RecycleBin {

        @SuppressLint("NewApi")
        final LruCache<Integer, Bitmap> bitmapCache = new LruCache<Integer, Bitmap>(
                getCacheSize(getContext())) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                } else {
                    return bitmap.getByteCount();
                }
            }

            @Override
            protected void entryRemoved(boolean evicted, Integer key,
                    Bitmap oldValue, Bitmap newValue) {
                if (evicted && oldValue != null && !oldValue.isRecycled()) {
                    oldValue.recycle();
                    oldValue = null;
                }
            }
        };

        public Bitmap getCachedBitmap(int position) {
            return bitmapCache.get(position);
        }

        public void addBitmap2Cache(int position, Bitmap b) {
            bitmapCache.put(position, b);
            Runtime.getRuntime().gc();
        }

        public void removeCachedBitmap(int position) {
            if (position < 0 || position >= bitmapCache.size()) {
                return;
            }

            bitmapCache.remove(position);
        }

        public void clear() {
            bitmapCache.evictAll();
        }

        private int getCacheSize(Context context) {
            final ActivityManager am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            final int memClass = am.getMemoryClass();
            // Target ~15% of the available heap.
            int cacheSize = 1024 * 1024 * memClass / 7;

            Log.e(VIEW_LOG_TAG, "cacheSize == " + cacheSize);
            return cacheSize;
        }
    }

    public static interface TopImageLongClickListener {
        public void onLongClick(int position);
    }

    public static interface CoverFlowListener<V extends CoverFlowAdapter> {
        public void imageOnTop(final CoverFlowView<V> coverFlowView,
                int position, float left, float top, float right, float bottom);

        public void topImageClicked(final CoverFlowView<V> coverFlowView,
                int position);
    }
}
