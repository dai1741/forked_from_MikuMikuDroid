package jp.gauzau.MikuMikuDroidmod;

import android.graphics.PointF;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import java.util.Arrays;

/**
 * A gesture listener for changing camera location, rotation, and scale. 
 * This class depends on CoreLogic and Quaternion class.
 * 
 * （元ファイルと一貫性ないけど以下のコメントは日本語で書きます…。
 * 　このファイルのエンコードはutf8に変更しているのでpull時に文字化けする可能性あり）
 */
public class CameraLocrotscaleGestureListener extends SimpleOnGestureListener implements
        OnScaleGestureListener {

    /** 平行移動を始めるのに必要なフリック距離の画面上の比率の2乗 */
    private static final float MIN_START_TRANSLATE_DISTANCE_SQR = 0.005f;

    /** 拡大縮小を始めるのに必要なピンチイン/ピンチアウトの量 */
    private static final float MIN_ZOOM_START_FACTOR = 1.2f;

    private static final String TAG = "CameraLocrotGestureListner";

    /** 平行移動中ならtrue */
    private boolean mIsOnTranslate;
    /** 拡大縮小中ならtrue */
    private boolean mIsOnZoom;

    /** 現在のズーム量。1でデフォルトのズーム量 */
    private float mZoomRate = 1;
    
    /** 直前のピンチイン/ピンチアウト開始時のズーム量。 */
    private float mBeginningZoomRate = 1;

    /** 画面の短い方の幅を1とした、現在のカメラの位置(x,y,z)。 */
    private final float[] mLocationRate = new float[] { 0, 0, 0 };
    /** 平行移動開始時のカメラの位置 */
    private final float[] mBeginningLocationRate = new float[] { 0, 0, 0 };
    
    /**
     *  画面の短い方の幅を1とした、現在のカメラの向き(x,y,z)。
     *  yxzオイラー角の定数倍。現状ではzは常に0。
     */
    private final float[] mRotationRate = new float[] { 0, 0, 0 };
    /** 回転開始時のカメラの向き */
    private final float[] mBeginningRotationRate = new float[] { 0, 0, 0 };

    /** 平行移動開始時のフォーカスの位置 */
    private final PointF mBeginningFocus = new PointF();
    /** 作業用 */
    private final PointF mFocusDiffRate = new PointF();
    
    /** ズーム開始時の2つの指の距離 */
    private float mBeginningSpan;

    /** 画面の短い方の幅 */
    private float mSmallerScreenWidth = 1;

    private final CoreLogic mCoreLogic;
    
    /** デフォルトのカメラと中心点との距離 */
    public static final float INITIAL_CAMERA_DISTANCE = 35;

    /** 
     * 現在のズーム量を中央処理側に適用する。
     */
    private void applyZoom() {
        mCoreLogic.mCameraZoom = -INITIAL_CAMERA_DISTANCE / mZoomRate;
    }

    /**
     * 画面サイズとの比率をMMD側の座標サイズと合わせる定数。
     * おそらく本来はGLビューのアスペクト比に依存するが、現時点では考慮していない。
     */
    private static final float TRANSLATE_RATE = 14.75f;

    /**
     * 現在の位置を中央処理側に適用する。
     */
    private void applyLocation() {
        mCoreLogic.mCameraLocation[0] = -mLocationRate[0] * TRANSLATE_RATE;
        mCoreLogic.mCameraLocation[1] = 10 + mLocationRate[1] * TRANSLATE_RATE;
        mCoreLogic.mCameraLocation[2] = -mLocationRate[2] * TRANSLATE_RATE;
    }

    /** 画面サイズとの比率を回転量と合わせる定数 */
    private static final float ROTATE_RATE = -120;
    private static final double ROTATE_RATE_RAD = -ROTATE_RATE / 180.0 * Math.PI;

    /**
     * 現在の向きを中央処理側に適用する。
     */
    private void applyRotation() {
        // mmd camera rotation uses y-x-z euler angles
        mCoreLogic.mCameraRotation[0] = mRotationRate[1] * ROTATE_RATE;
        mCoreLogic.mCameraRotation[1] = mRotationRate[0] * ROTATE_RATE;
        mCoreLogic.mCameraRotation[2] = mRotationRate[2] * ROTATE_RATE;
    }


    public CameraLocrotscaleGestureListener(CoreLogic coreLogic) {
        mCoreLogic = coreLogic;
        reset();
    }

    /**
     * カメラの状態を初期化する。
     */
    public void reset() {
        mZoomRate = 1;
        Arrays.fill(mLocationRate, 0);
        Arrays.fill(mRotationRate, 0);
        applyZoom();
        applyLocation();
        applyRotation();

    }

    /* (non-Javadoc)
     * 2本指でタッチしているときのイベント。
     * 2本指で画面を平行移動しているならカメラを平行移動し、
     * ピンチイン/ピンチアウトしているならカメラをズームする。
     */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (!mIsOnTranslate && !mIsOnZoom) {
            float span = detector.getCurrentSpan();
            float factor = span / mBeginningSpan;
            float changeRate = factor >= 1 ? factor : 1 / factor;
            if (changeRate > MIN_ZOOM_START_FACTOR) {
                mIsOnZoom = true;
                mBeginningSpan = span;
            }
            else {
                mFocusDiffRate.set((detector.getFocusX() - mBeginningFocus.x)
                        / mSmallerScreenWidth, (detector.getFocusY() - mBeginningFocus.y)
                        / mSmallerScreenWidth);
                float movedDistanceRateSqr = mFocusDiffRate.x * mFocusDiffRate.x
                        + mFocusDiffRate.y * mFocusDiffRate.y;
                if (movedDistanceRateSqr > MIN_START_TRANSLATE_DISTANCE_SQR) {
                    mIsOnTranslate = true;
                    mBeginningFocus.set(detector.getFocusX(), detector.getFocusY());
                }
            }
        }
        if (mIsOnTranslate) {
            double[] coordAsQuat = new double[] {
                    (detector.getFocusX() - mBeginningFocus.x) / mSmallerScreenWidth
                            / mZoomRate,
                    (detector.getFocusY() - mBeginningFocus.y) / mSmallerScreenWidth
                            / mZoomRate, 0, 0 };
            Quaternion.mul(coordAsQuat, mConjugateQuat, coordAsQuat);
            Quaternion.mul(coordAsQuat, coordAsQuat, mQuat);
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

    /* (non-Javadoc)
     * 2本目の指がタッチされたときの処理。
     * カメラ回転を終了し、ズームや平行移動の準備をする。
     */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mBeginningFocus.set(detector.getFocusX(), detector.getFocusY());

        mBeginningZoomRate = mZoomRate;
        mBeginningSpan = detector.getCurrentSpan();
        System.arraycopy(mLocationRate, 0, mBeginningLocationRate, 0, 3);

        // create rotation matrix for location change
        Quaternion.createFromAngleAxis(mQuat, mRotationRate[1] * ROTATE_RATE_RAD,
                new float[] { 1, 0, 0 });
        Quaternion.createFromAngleAxis(mQuatTemp, -mRotationRate[0] * ROTATE_RATE_RAD,
                new float[] { 0, 1, 0 }); // mmd camera rotation x is reverted
        Quaternion.mul(mQuat, mQuat, mQuatTemp);
        Quaternion.createFromAngleAxis(mQuatTemp, mRotationRate[2] * ROTATE_RATE_RAD,
                new float[] { 0, 0, 1 });
        Quaternion.mul(mQuat, mQuat, mQuatTemp);
        Quaternion.normalize(mQuat, mQuat);

        System.arraycopy(mQuat, 0, mConjugateQuat, 0, 4);
        mConjugateQuat[0] *= -1;
        mConjugateQuat[1] *= -1;
        mConjugateQuat[2] *= -1;

        mIsRotateAborted = true;
        return true;
    }

    /** 作業用クォータニオン(x,y,z, w) */
    private final double[] mQuatTemp = new double[4];
    
    /** 平行移動をカメラの向きと合わせるためのクォータニオン */
    private final double[] mQuat = new double[4];
    /** {@code mQuat}の共役 */
    private final double[] mConjugateQuat = new double[4];

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mIsOnTranslate = mIsOnZoom = false;
    }

    /** 作業用 */
    private final PointF mScrollDiffRate = new PointF();
    /** タッチ開始した場所の座標 */
    private final PointF mBeginningTapPoint = new PointF();

    /** 回転中ならtrue */
    private boolean mIsOnRotate;
    /** 現在のタッチで回転中にならないならtrue。2本目の指を検知するとtrueになる */
    private boolean mIsRotateAborted;

    /* (non-Javadoc)
     * 1本指でスクロールしているときは、画面を回転させる。
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        if (!mIsRotateAborted) {
            if (!mIsOnRotate) {
                mIsOnRotate = true;
                mBeginningTapPoint.set(e2.getX(), e2.getY());
            }
            if (mIsOnRotate) {
                mScrollDiffRate.set((e2.getX() - mBeginningTapPoint.x)
                        / mSmallerScreenWidth, (e2.getY() - mBeginningTapPoint.y)
                        / mSmallerScreenWidth);
                mRotationRate[0] = mBeginningRotationRate[0] + mScrollDiffRate.x;
                mRotationRate[1] = mBeginningRotationRate[1] + mScrollDiffRate.y;
                applyRotation();
            }
        }
        return false;
    }


    @Override
    public boolean onDown(MotionEvent e) {
        mIsOnRotate = mIsRotateAborted = false;
        mSmallerScreenWidth = Math.min(mCoreLogic.getScreenWidth(), mCoreLogic
                .getScreenHeight());
        System.arraycopy(mRotationRate, 0, mBeginningRotationRate, 0, 3);
        mBeginningTapPoint.set(e.getX(), e.getY());
        return false;
    }


    /**
     * タップが終了したときの処理。
     * @return 【HACK】2つ以上の指を使ったMotionEventでこのメソッドが誤って呼ばれるので、
     *          その場合はイベント消費済みを意味するtrueを返す。
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return mIsRotateAborted;
    }


}
