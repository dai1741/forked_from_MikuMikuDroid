package jp.gauzau.MikuMikuDroidmod;

import android.graphics.PointF;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

public class CameraLocrotscaleGestureListener extends SimpleOnGestureListener implements
        OnScaleGestureListener {

    private static final float MIN_START_TRANSLATE_DISTANCE_SQR = 0.07f;

    private static final float MIN_ZOOM_START_FACTOR = 1.1f;

    private static final String TAG = "CameraLocrotGestureListner";

    private boolean mIsOnTranslate;
    private boolean mIsOnZoom;

    private float mZoomRate = 1;
    private float mPreviousZoomRate = 1;
    private final float[] mLocationRate = new float[] { 0, 0, 0 };
    private final float[] mPreviousLocationRate = new float[] { 0, 0, 0 };
    private final float[] mRotationRate = new float[] { 0, 0, 0 };
    private final float[] mPreviousRotationRate = new float[] { 0, 0, 0 };

    private final PointF mPreviousFocus = new PointF();
    private final PointF mFocusDiffRate = new PointF();

    private float mSmallerScreenWidth = 1;

    private final CoreLogic mCoreLogic;
    
    private void applyZoom() {
        mCoreLogic.mCameraZoom = -35 * mZoomRate;
    }
    
    
    public CameraLocrotscaleGestureListener(CoreLogic coreLogic) {
        mCoreLogic = coreLogic;
        applyZoom();
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (!mIsOnTranslate && !mIsOnZoom) {
            float factor = detector.getScaleFactor();
            float changeRate = factor >= 1 ? factor : 1 / factor;
            if (changeRate > MIN_ZOOM_START_FACTOR) {
                Log.d(TAG, "Start zooming");
                mIsOnZoom = true;
            }
            else {
                mFocusDiffRate.set((detector.getFocusX() - mPreviousFocus.x)
                        / mSmallerScreenWidth, (detector.getFocusY() - mPreviousFocus.y)
                        / mSmallerScreenWidth);
                float movedDistanceRateSqr = mFocusDiffRate.x * mFocusDiffRate.x
                        + mFocusDiffRate.y * mFocusDiffRate.y;
                if (movedDistanceRateSqr > MIN_START_TRANSLATE_DISTANCE_SQR) {
                    Log.d(TAG, "Start translating");
                    mIsOnTranslate = true;
                }
            }
        }
        if (mIsOnTranslate) {
            // TODO: 3d
            mLocationRate[0] = mPreviousLocationRate[0]
                    + (detector.getFocusX() - mPreviousFocus.x) / mSmallerScreenWidth;
            mLocationRate[1] = mPreviousLocationRate[1]
                    + (detector.getFocusX() - mPreviousFocus.x) / mSmallerScreenWidth;
        }
        else if (mIsOnZoom) {
            float factor = detector.getScaleFactor();
            mZoomRate = mPreviousZoomRate * factor;
            applyZoom();
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mPreviousFocus.set(detector.getFocusX(), detector.getFocusY());
        mSmallerScreenWidth = Math.min(mCoreLogic.getScreenWidth(), mCoreLogic
                .getScreenHeight());

        mPreviousZoomRate = mZoomRate;
        System.arraycopy(mLocationRate, 0, mPreviousLocationRate, 0, 3);
        System.arraycopy(mRotationRate, 0, mPreviousRotationRate, 0, 3);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mIsOnTranslate = mIsOnZoom = false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        // TODO Auto-generated method stub
        return super.onScroll(e1, e2, distanceX, distanceY);
    }
    
    

}
