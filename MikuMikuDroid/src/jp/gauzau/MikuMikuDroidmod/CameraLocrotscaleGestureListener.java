package jp.gauzau.MikuMikuDroidmod;

import android.graphics.PointF;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import java.util.Arrays;

public class CameraLocrotscaleGestureListener extends SimpleOnGestureListener implements
        OnScaleGestureListener {

    private static final float MIN_START_TRANSLATE_DISTANCE_SQR = 0.0075f;

    private static final float MIN_ZOOM_START_FACTOR = 1.2f;

    private static final String TAG = "CameraLocrotGestureListner";

    private boolean mIsOnTranslate;
    private boolean mIsOnZoom;

    private float mZoomRate = 1;
    private float mBeginningZoomRate = 1;
    private final float[] mLocationRate = new float[] { 0, 0, 0 };
    private final float[] mBeginningLocationRate = new float[] { 0, 0, 0 };
    private final float[] mRotationRate = new float[] { 0, 0, 0 };
    private final float[] mBeginningRotationRate = new float[] { 0, 0, 0 };

    private final PointF mBeginningFocus = new PointF();
    private final PointF mFocusDiffRate = new PointF();
    private float mBeginningSpan;

    private float mSmallerScreenWidth = 1;

    private final CoreLogic mCoreLogic;

    private void applyZoom() {
        mCoreLogic.mCameraZoom = -35 / mZoomRate;
    }

    private static final float TRANSLATE_RATE = 10;

    private void applyLocation() {
        mCoreLogic.mCameraLocation[0] = -mLocationRate[0] * TRANSLATE_RATE;
        mCoreLogic.mCameraLocation[1] = 10 + mLocationRate[1] * TRANSLATE_RATE;
        mCoreLogic.mCameraLocation[2] = -mLocationRate[2] * TRANSLATE_RATE;
    }

    private static final float ROTATE_RATE = -90;
    private static final double ROTATE_RATE_RAD = -90 / 180.0 * Math.PI;

    private void applyRotation() {
        // mmd camera rotation uses y-x-z euler angles
        mCoreLogic.mCameraRotation[0] = mRotationRate[1] * ROTATE_RATE;
        mCoreLogic.mCameraRotation[1] = mRotationRate[0] * ROTATE_RATE;
        mCoreLogic.mCameraRotation[2] = mRotationRate[2] * ROTATE_RATE;
    }


    public CameraLocrotscaleGestureListener(CoreLogic coreLogic) {
        mCoreLogic = coreLogic;
        applyZoom();
        applyLocation();
        applyRotation();
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (!mIsOnTranslate && !mIsOnZoom) {
            float factor = detector.getCurrentSpan() / mBeginningSpan;
            float changeRate = factor >= 1 ? factor : 1 / factor;
            if (changeRate > MIN_ZOOM_START_FACTOR) {
                Log.d(TAG, "Start zooming");
                mIsOnZoom = true;
            }
            else {
                mFocusDiffRate.set((detector.getFocusX() - mBeginningFocus.x)
                        / mSmallerScreenWidth, (detector.getFocusY() - mBeginningFocus.y)
                        / mSmallerScreenWidth);
                float movedDistanceRateSqr = mFocusDiffRate.x * mFocusDiffRate.x
                        + mFocusDiffRate.y * mFocusDiffRate.y;
                Log.d(TAG, "dist sqr: " + movedDistanceRateSqr);
                if (movedDistanceRateSqr > MIN_START_TRANSLATE_DISTANCE_SQR) {
                    Log.d(TAG, "Start translating");
                    mIsOnTranslate = true;
                }
            }
        }
        if (mIsOnTranslate) {
            // TODO: 3d
            // mLocationRate[0] = mBeginningLocationRate[0]
            // + (detector.getFocusX() - mBeginningFocus.x) / mSmallerScreenWidth;
            // mLocationRate[1] = mBeginningLocationRate[1]
            // + (detector.getFocusY() - mBeginningFocus.y) / mSmallerScreenWidth;
            double[] coordAsQuat = new double[] {
                    (detector.getFocusX() - mBeginningFocus.x) / mSmallerScreenWidth,
                    (detector.getFocusY() - mBeginningFocus.y) / mSmallerScreenWidth, 0,
                    0 };
            Quaternion.mul(mQuatTemp, mConjugateQuat, coordAsQuat);
            Quaternion.mul(coordAsQuat, mQuatTemp, mQuat);
            mLocationRate[0] = mBeginningLocationRate[0] + (float) coordAsQuat[0];
            mLocationRate[1] = mBeginningLocationRate[1] + (float) coordAsQuat[1];
            mLocationRate[2] = mBeginningLocationRate[2] + (float) coordAsQuat[2];
            applyLocation();
        }
        else if (mIsOnZoom) {
            float factor = detector.getCurrentSpan() / mBeginningSpan;
            mZoomRate = mBeginningZoomRate * factor;
            applyZoom();
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mBeginningFocus.set(detector.getFocusX(), detector.getFocusY());

        mBeginningZoomRate = mZoomRate;
        mBeginningSpan = detector.getCurrentSpan();
        System.arraycopy(mLocationRate, 0, mBeginningLocationRate, 0, 3);

        // create rotation matrix for location change
        Quaternion.createFromAngleAxis(mQuatTemp, mRotationRate[1] * ROTATE_RATE_RAD,
                new float[] { 0, 1, 0 });
        Quaternion.createFromAngleAxis(mQuatTemp2, mRotationRate[0] * ROTATE_RATE_RAD,
                new float[] { 1, 0, 0 });
        Quaternion.mul(mQuatTemp3, mQuatTemp, mQuatTemp2);
        Quaternion.createFromAngleAxis(mQuatTemp, mRotationRate[2] * ROTATE_RATE_RAD,
                new float[] { 0, 0, 1 });
        Quaternion.mul(mQuatTemp2, mQuatTemp3, mQuatTemp);
        Quaternion.normalize(mQuat, mQuatTemp2);
        // Quaternion.toMatrix(mRotaionMatirix, new float[] {
        // (float) mQuatTemp[0], (float) mQuatTemp[1], (float) mQuatTemp[2],
        // (float) mQuatTemp[3] });
        System.arraycopy(mQuat, 0, mConjugateQuat, 0, 4);
        mConjugateQuat[0] *= -1;
        mConjugateQuat[1] *= -1;
        mConjugateQuat[2] *= -1;

        mIsOnRotate = false;
        Log.d(TAG, "Scale bigin");
        return true;
    }

    private final double[] mQuatTemp = new double[4];
    private final double[] mQuatTemp2 = new double[4];
    private final double[] mQuatTemp3 = new double[4];
    private final double[] mQuat = new double[4];
    private final double[] mConjugateQuat = new double[4];

    // private final float[] mRotaionMatirix = new float[16];

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mIsOnTranslate = mIsOnZoom = false;
    }


    private static final float MIN_START_ROTATE_DISTANCE_SQR = 0.0075f;
    private final PointF mScrollDiffRate = new PointF();
    private boolean mIsOnRotate;

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        mScrollDiffRate.set((e2.getX() - e1.getX()) / mSmallerScreenWidth,
                (e2.getY() - e1.getY()) / mSmallerScreenWidth);
        if (!mIsOnRotate) {
            float movedDistanceRateSqr = mScrollDiffRate.x * mScrollDiffRate.x
                    + mScrollDiffRate.y * mScrollDiffRate.y;
            Log.d(TAG, "dist sqr: " + movedDistanceRateSqr);
            if (movedDistanceRateSqr > MIN_START_ROTATE_DISTANCE_SQR) {
                Log.d(TAG, "Start rotating");
                mIsOnRotate = true;
            }
        }
        if (mIsOnRotate) {
            mRotationRate[0] = mBeginningRotationRate[0] + mScrollDiffRate.x;
            mRotationRate[1] = mBeginningRotationRate[1] + mScrollDiffRate.y;
            applyRotation();
        }
        return false;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        mIsOnRotate = false;
        mSmallerScreenWidth = Math.min(mCoreLogic.getScreenWidth(), mCoreLogic
                .getScreenHeight());
        System.arraycopy(mRotationRate, 0, mBeginningRotationRate, 0, 3);
        return false;
    }


}
