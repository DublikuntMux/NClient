package com.dublikunt.nclient.photoview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PhotoViewAttacher implements View.OnTouchListener,
    View.OnLayoutChangeListener {

    private static final float DEFAULT_MAX_SCALE = 3.0f;
    private static final float DEFAULT_MID_SCALE = 1.75f;
    private static final float DEFAULT_MIN_SCALE = 1.0f;
    private static final int DEFAULT_ZOOM_DURATION = 200;

    private static final int HORIZONTAL_EDGE_NONE = -1;
    private static final int HORIZONTAL_EDGE_LEFT = 0;
    private static final int HORIZONTAL_EDGE_RIGHT = 1;
    private static final int HORIZONTAL_EDGE_BOTH = 2;
    private static final int VERTICAL_EDGE_NONE = -1;
    private static final int VERTICAL_EDGE_TOP = 0;
    private static final int VERTICAL_EDGE_BOTTOM = 1;
    private static final int VERTICAL_EDGE_BOTH = 2;
    private static final int SINGLE_TOUCH = 1;
    private final ImageView mImageView;
    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mDrawMatrix = new Matrix();
    private final Matrix mSuppMatrix = new Matrix();
    private final RectF mDisplayRect = new RectF();
    private final float[] mMatrixValues = new float[9];
    private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private int mZoomDuration = DEFAULT_ZOOM_DURATION;
    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;
    private boolean mAllowParentInterceptOnEdge = true;
    private boolean mBlockParentIntercept = false;
    private GestureDetector mGestureDetector;
    private CustomGestureDetector mScaleDragDetector;
    private OnMatrixChangedListener mMatrixChangeListener;
    private OnPhotoTapListener mPhotoTapListener;
    private OnOutsidePhotoTapListener mOutsidePhotoTapListener;
    private OnViewTapListener mViewTapListener;
    private View.OnClickListener mOnClickListener;
    private OnLongClickListener mLongClickListener;
    private OnScaleChangedListener mScaleChangeListener;
    private OnSingleFlingListener mSingleFlingListener;
    private OnViewDragListener mOnViewDragListener;

    private FlingRunnable mCurrentFlingRunnable;
    private int mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH;
    private int mVerticalScrollEdge = VERTICAL_EDGE_BOTH;
    private float mBaseRotation;

    private boolean mZoomEnabled = true;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    private final OnGestureListener onGestureListener = new OnGestureListener() {
        @Override
        public void onDrag(float dx, float dy) {
            if (mScaleDragDetector.isScaling()) {
                return;
            }
            if (mOnViewDragListener != null) {
                mOnViewDragListener.onDrag(dx, dy);
            }
            mSuppMatrix.postTranslate(dx, dy);
            checkAndDisplayMatrix();

            ViewParent parent = mImageView.getParent();
            if (mAllowParentInterceptOnEdge && !mScaleDragDetector.isScaling() && !mBlockParentIntercept) {
                if (mHorizontalScrollEdge == HORIZONTAL_EDGE_BOTH
                    || (mHorizontalScrollEdge == HORIZONTAL_EDGE_LEFT && dx >= 1f)
                    || (mHorizontalScrollEdge == HORIZONTAL_EDGE_RIGHT && dx <= -1f)
                    || (mVerticalScrollEdge == VERTICAL_EDGE_TOP && dy >= 1f)
                    || (mVerticalScrollEdge == VERTICAL_EDGE_BOTTOM && dy <= -1f)) {
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
            } else {
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }
        }

        @Override
        public void onFling(float startX, float startY, float velocityX, float velocityY) {
            mCurrentFlingRunnable = new FlingRunnable(mImageView.getContext());
            mCurrentFlingRunnable.fling(getImageViewWidth(mImageView),
                getImageViewHeight(mImageView), (int) velocityX, (int) velocityY);
            mImageView.post(mCurrentFlingRunnable);
        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
            onScale(scaleFactor, focusX, focusY, 0, 0);
        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY, float dx, float dy) {
            if (getScale() < mMaxScale || scaleFactor < 1f) {
                if (mScaleChangeListener != null) {
                    mScaleChangeListener.onScaleChange(scaleFactor, focusX, focusY);
                }
                mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                mSuppMatrix.postTranslate(dx, dy);
                checkAndDisplayMatrix();
            }
        }
    };

    public PhotoViewAttacher(@NonNull ImageView imageView) {
        mImageView = imageView;
        imageView.setOnTouchListener(this);
        imageView.addOnLayoutChangeListener(this);
        if (imageView.isInEditMode()) {
            return;
        }
        mBaseRotation = 0.0f;
        mScaleDragDetector = new CustomGestureDetector(imageView.getContext(), onGestureListener);
        mGestureDetector = new GestureDetector(imageView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                if (mLongClickListener != null) {
                    mLongClickListener.onLongClick(mImageView);
                }
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                if (mSingleFlingListener != null) {
                    if (getScale() > DEFAULT_MIN_SCALE) {
                        return false;
                    }
                    if (e1.getPointerCount() > SINGLE_TOUCH
                        || e2.getPointerCount() > SINGLE_TOUCH) {
                        return false;
                    }
                    return mSingleFlingListener.onFling(e1, e2, velocityX, velocityY);
                }
                return false;
            }
        });
        mGestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(mImageView);
                }
                final RectF displayRect = getDisplayRect();
                final float x = e.getX(), y = e.getY();
                if (mViewTapListener != null) {
                    mViewTapListener.onViewTap(mImageView, x, y);
                }
                if (displayRect != null) {
                    if (displayRect.contains(x, y)) {
                        float xResult = (x - displayRect.left)
                            / displayRect.width();
                        float yResult = (y - displayRect.top)
                            / displayRect.height();
                        if (mPhotoTapListener != null) {
                            mPhotoTapListener.onPhotoTap(mImageView, xResult, yResult);
                        }
                        return true;
                    } else {
                        if (mOutsidePhotoTapListener != null) {
                            mOutsidePhotoTapListener.onOutsidePhotoTap(mImageView);
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent ev) {
                try {
                    float scale = getScale();
                    float x = ev.getX();
                    float y = ev.getY();
                    if (scale < getMediumScale()) {
                        setScale(getMediumScale(), x, y, true);
                    } else if (scale >= getMediumScale() && scale < getMaximumScale()) {
                        setScale(getMaximumScale(), x, y, true);
                    } else {
                        setScale(getMinimumScale(), x, y, true);
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
    }

    public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener newOnDoubleTapListener) {
        this.mGestureDetector.setOnDoubleTapListener(newOnDoubleTapListener);
    }

    public void setOnScaleChangeListener(OnScaleChangedListener onScaleChangeListener) {
        this.mScaleChangeListener = onScaleChangeListener;
    }

    public void setOnSingleFlingListener(OnSingleFlingListener onSingleFlingListener) {
        this.mSingleFlingListener = onSingleFlingListener;
    }

    @Deprecated
    public boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    public RectF getDisplayRect() {
        checkMatrixBounds();
        return getDisplayRect(getDrawMatrix());
    }

    public boolean setDisplayMatrix(Matrix finalMatrix) {
        if (finalMatrix == null) {
            throw new IllegalArgumentException("Matrix cannot be null");
        }
        if (mImageView.getDrawable() == null) {
            return false;
        }
        mSuppMatrix.set(finalMatrix);
        checkAndDisplayMatrix();
        return true;
    }

    public void setBaseRotation(final float degrees) {
        mBaseRotation = degrees % 360;
        update();
        setRotationBy(mBaseRotation);
        checkAndDisplayMatrix();
    }

    public void setRotationTo(float degrees) {
        mSuppMatrix.setRotate(degrees % 360);
        checkAndDisplayMatrix();
    }

    public void setRotationBy(float degrees) {
        mSuppMatrix.postRotate(degrees % 360);
        checkAndDisplayMatrix();
    }

    public float getMinimumScale() {
        return mMinScale;
    }

    public void setMinimumScale(float minimumScale) {
        Util.checkZoomLevels(minimumScale, mMidScale, mMaxScale);
        mMinScale = minimumScale;
    }

    public float getMediumScale() {
        return mMidScale;
    }

    public void setMediumScale(float mediumScale) {
        Util.checkZoomLevels(mMinScale, mediumScale, mMaxScale);
        mMidScale = mediumScale;
    }

    public float getMaximumScale() {
        return mMaxScale;
    }

    public void setMaximumScale(float maximumScale) {
        Util.checkZoomLevels(mMinScale, mMidScale, maximumScale);
        mMaxScale = maximumScale;
    }

    public float getScale() {
        return (float) Math.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow
            (getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
    }

    public void setScale(float scale) {
        setScale(scale, false);
    }

    public ScaleType getScaleType() {
        return mScaleType;
    }

    public void setScaleType(ScaleType scaleType) {
        if (Util.isSupportedScaleType(scaleType) && scaleType != mScaleType) {
            mScaleType = scaleType;
            update();
        }
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
        oldRight, int oldBottom) {
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(mImageView.getDrawable());
        }
    }


    @Override
    public boolean onTouch(@NonNull View view, MotionEvent event) {
        view.performClick();
        boolean handled = false;
        if (mZoomEnabled && Util.hasDrawable((ImageView) view)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    ViewParent parent = view.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    cancelFling();
                }
                case MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    if (getScale() < mMinScale) {
                        RectF rect = getDisplayRect();
                        if (rect != null) {
                            view.post(new AnimatedZoomRunnable(getScale(), mMinScale,
                                rect.centerX(), rect.centerY()));
                            handled = true;
                        }
                    } else if (getScale() > mMaxScale) {
                        RectF rect = getDisplayRect();
                        if (rect != null) {
                            view.post(new AnimatedZoomRunnable(getScale(), mMaxScale,
                                rect.centerX(), rect.centerY()));
                            handled = true;
                        }
                    }
                }
            }
            if (mScaleDragDetector != null) {
                boolean wasScaling = mScaleDragDetector.isScaling();
                boolean wasDragging = mScaleDragDetector.isDragging();
                handled = mScaleDragDetector.onTouchEvent(event);
                boolean didntScale = !wasScaling && !mScaleDragDetector.isScaling();
                boolean didntDrag = !wasDragging && !mScaleDragDetector.isDragging();
                mBlockParentIntercept = didntScale && didntDrag;
            }
            if (mGestureDetector != null && mGestureDetector.onTouchEvent(event)) {
                handled = true;
            }

        }
        return handled;
    }

    public void setAllowParentInterceptOnEdge(boolean allow) {
        mAllowParentInterceptOnEdge = allow;
    }

    public void setScaleLevels(float minimumScale, float mediumScale, float maximumScale) {
        Util.checkZoomLevels(minimumScale, mediumScale, maximumScale);
        mMinScale = minimumScale;
        mMidScale = mediumScale;
        mMaxScale = maximumScale;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mLongClickListener = listener;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setOnMatrixChangeListener(OnMatrixChangedListener listener) {
        mMatrixChangeListener = listener;
    }

    public void setOnPhotoTapListener(OnPhotoTapListener listener) {
        mPhotoTapListener = listener;
    }

    public void setOnOutsidePhotoTapListener(OnOutsidePhotoTapListener mOutsidePhotoTapListener) {
        this.mOutsidePhotoTapListener = mOutsidePhotoTapListener;
    }

    public void setOnViewTapListener(OnViewTapListener listener) {
        mViewTapListener = listener;
    }

    public void setOnViewDragListener(OnViewDragListener listener) {
        mOnViewDragListener = listener;
    }

    public void setScale(float scale, boolean animate) {
        setScale(scale,
            (mImageView.getRight()) / 2f,
            (mImageView.getBottom()) / 2f,
            animate);
    }

    public void setScale(float scale, float focalX, float focalY, boolean animate) {
        scale = Math.min(scale, mMaxScale);
        scale = Math.max(scale, mMinScale);
        if (animate) {
            mImageView.post(new AnimatedZoomRunnable(getScale(), scale,
                focalX, focalY));
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY);
            checkAndDisplayMatrix();
        }
    }

    public void setZoomInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public boolean isZoomable() {
        return mZoomEnabled;
    }

    public void setZoomable(boolean zoomable) {
        mZoomEnabled = zoomable;
        update();
    }

    public void update() {
        if (mZoomEnabled) {
            updateBaseMatrix(mImageView.getDrawable());
        } else {
            resetMatrix();
        }
    }

    public void getDisplayMatrix(@NonNull Matrix matrix) {
        matrix.set(getDrawMatrix());
    }

    public void getSuppMatrix(@NonNull Matrix matrix) {
        matrix.set(mSuppMatrix);
    }

    private Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    public Matrix getImageMatrix() {
        return mDrawMatrix;
    }

    public void setZoomTransitionDuration(int milliseconds) {
        this.mZoomDuration = milliseconds;
    }

    private float getValue(@NonNull Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    private void resetMatrix() {
        mSuppMatrix.reset();
        setRotationBy(mBaseRotation);
        setImageViewMatrix(getDrawMatrix());
        checkMatrixBounds();
    }

    private void setImageViewMatrix(Matrix matrix) {
        mImageView.setImageMatrix(matrix);
        if (mMatrixChangeListener != null) {
            RectF displayRect = getDisplayRect(matrix);
            if (displayRect != null) {
                mMatrixChangeListener.onMatrixChanged(displayRect);
            }
        }
    }

    private void checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(getDrawMatrix());
        }
    }

    @Nullable
    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = mImageView.getDrawable();
        if (d != null) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return null;
    }

    private void updateBaseMatrix(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        final float viewWidth = getImageViewWidth(mImageView);
        final float viewHeight = getImageViewHeight(mImageView);
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();
        mBaseMatrix.reset();
        final float widthScale = viewWidth / drawableWidth;
        final float heightScale = viewHeight / drawableHeight;
        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate((viewWidth - drawableWidth) / 2F,
                (viewHeight - drawableHeight) / 2F);

        } else if (mScaleType == ScaleType.CENTER_CROP) {
            float scale = Math.max(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                (viewHeight - drawableHeight * scale) / 2F);

        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            float scale = Math.min(1.0f, Math.min(widthScale, heightScale));
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2F,
                (viewHeight - drawableHeight * scale) / 2F);

        } else {
            RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF mTempDst = new RectF(0, 0, viewWidth, viewHeight);
            if ((int) mBaseRotation % 180 != 0) {
                mTempSrc = new RectF(0, 0, drawableHeight, drawableWidth);
            }
            switch (mScaleType) {
                case FIT_CENTER -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.CENTER);
                case FIT_START -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.START);
                case FIT_END -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.END);
                case FIT_XY -> mBaseMatrix.setRectToRect(mTempSrc, mTempDst, ScaleToFit.FILL);
                default -> {
                }
            }
        }
        resetMatrix();
    }

    private boolean checkMatrixBounds() {
        final RectF rect = getDisplayRect(getDrawMatrix());
        if (rect == null) {
            return false;
        }
        final float height = rect.height(), width = rect.width();
        float deltaX = 0, deltaY = 0;
        final int viewHeight = getImageViewHeight(mImageView);
        if (height <= viewHeight) {
            deltaY = switch (mScaleType) {
                case FIT_START -> -rect.top;
                case FIT_END -> viewHeight - height - rect.top;
                default -> (viewHeight - height) / 2 - rect.top;
            };
            mVerticalScrollEdge = VERTICAL_EDGE_BOTH;
        } else if (rect.top > 0) {
            mVerticalScrollEdge = VERTICAL_EDGE_TOP;
            deltaY = -rect.top;
        } else if (rect.bottom < viewHeight) {
            mVerticalScrollEdge = VERTICAL_EDGE_BOTTOM;
            deltaY = viewHeight - rect.bottom;
        } else {
            mVerticalScrollEdge = VERTICAL_EDGE_NONE;
        }
        final int viewWidth = getImageViewWidth(mImageView);
        if (width <= viewWidth) {
            deltaX = switch (mScaleType) {
                case FIT_START -> -rect.left;
                case FIT_END -> viewWidth - width - rect.left;
                default -> (viewWidth - width) / 2 - rect.left;
            };
            mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH;
        } else if (rect.left > 0) {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_LEFT;
            deltaX = -rect.left;
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right;
            mHorizontalScrollEdge = HORIZONTAL_EDGE_RIGHT;
        } else {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_NONE;
        }
        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY);
        return true;
    }

    private int getImageViewWidth(@NonNull ImageView imageView) {
        return imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
    }

    private int getImageViewHeight(@NonNull ImageView imageView) {
        return imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
    }

    private void cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private class AnimatedZoomRunnable implements Runnable {

        private final float mFocalX, mFocalY;
        private final long mStartTime;
        private final float mZoomStart, mZoomEnd;

        public AnimatedZoomRunnable(final float currentZoom, final float targetZoom,
                                    final float focalX, final float focalY) {
            mFocalX = focalX;
            mFocalY = focalY;
            mStartTime = System.currentTimeMillis();
            mZoomStart = currentZoom;
            mZoomEnd = targetZoom;
        }

        @Override
        public void run() {
            float t = interpolate();
            float scale = mZoomStart + t * (mZoomEnd - mZoomStart);
            float deltaScale = scale / getScale();
            onGestureListener.onScale(deltaScale, mFocalX, mFocalY);
            if (t < 1f) {
                Compat.postOnAnimation(mImageView, this);
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration;
            t = Math.min(1f, t);
            t = mInterpolator.getInterpolation(t);
            return t;
        }
    }

    private class FlingRunnable implements Runnable {

        private final OverScroller mScroller;
        private int mCurrentX, mCurrentY;

        public FlingRunnable(Context context) {
            mScroller = new OverScroller(context);
        }

        public void cancelFling() {
            mScroller.forceFinished(true);
        }

        public void fling(int viewWidth, int viewHeight, int velocityX,
                          int velocityY) {
            final RectF rect = getDisplayRect();
            if (rect == null) {
                return;
            }
            final int startX = Math.round(-rect.left);
            final int minX, maxX, minY, maxY;
            if (viewWidth < rect.width()) {
                minX = 0;
                maxX = Math.round(rect.width() - viewWidth);
            } else {
                minX = maxX = startX;
            }
            final int startY = Math.round(-rect.top);
            if (viewHeight < rect.height()) {
                minY = 0;
                maxY = Math.round(rect.height() - viewHeight);
            } else {
                minY = maxY = startY;
            }
            mCurrentX = startX;
            mCurrentY = startY;
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX,
                    maxX, minY, maxY, 0, 0);
            }
        }

        @Override
        public void run() {
            if (mScroller.isFinished()) {
                return;
            }
            if (mScroller.computeScrollOffset()) {
                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();
                mSuppMatrix.postTranslate(mCurrentX - newX, mCurrentY - newY);
                checkAndDisplayMatrix();
                mCurrentX = newX;
                mCurrentY = newY;
                Compat.postOnAnimation(mImageView, this);
            }
        }
    }
}
