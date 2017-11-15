package com.grishko188.visionlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.grishko188.visionlibrary.camera.CameraSource;
import com.grishko188.visionlibrary.camera.CameraSourcePreview;
import com.grishko188.visionlibrary.camera.GraphicOverlay;

import java.io.IOException;
import java.util.List;

/**
 * Created by UnrealMojo on 15.11.17.
 *
 * @author Grishko Nikita
 */

public class BarcodeReaderView extends LinearLayout implements View.OnTouchListener, BarcodeGraphicTracker.BarcodeGraphicTrackerListener {

    private static final String TAG = BarcodeReaderView.class.getSimpleName();

    private String mBeepSoundFile;

    private boolean mAutoFocus = true;
    private boolean mUseFlash = false;

    private boolean mIsPaused = false;
    private boolean mIsDrawRect = false;
    private boolean mIsDrawText = false;
    private boolean mPlaySoundWhenScanSuccess = false;
    private boolean mIsStopped;

    private int mBarcodeFormat = Barcode.ALL_FORMATS;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private BarcodeReaderListener mListener;

    public BarcodeReaderView(Context context) {
        super(context);
    }

    public BarcodeReaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttributes(attrs);
        init();
    }

    public BarcodeReaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BarcodeReaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttributes(attrs);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_barcode_reader, this);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);
        mGraphicOverlay.setDrawText(mIsDrawText);
        mGraphicOverlay.setDrawRect(mIsDrawRect);

        mGestureDetector = new GestureDetector(getContext(), new CaptureGestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        setOnTouchListener(this);
    }

    private void initAttributes(AttributeSet attributeSet) {
        if (attributeSet != null) {
            TypedArray a = getContext().obtainStyledAttributes(attributeSet, R.styleable.BarcodeReaderView);
            mAutoFocus = a.getBoolean(R.styleable.BarcodeReaderView_auto_focus, true);
            mUseFlash = a.getBoolean(R.styleable.BarcodeReaderView_use_flash, false);
            mIsDrawRect = a.getBoolean(R.styleable.BarcodeReaderView_draw_rect, false);
            mIsDrawText = a.getBoolean(R.styleable.BarcodeReaderView_draw_text, false);
            a.recycle();
        }
    }

    /**
     * Set auto focus default state. Call this method before {@link #startCamera()}
     */
    public void setAutoFocusPreInitState(boolean autoFocus) {
        this.mAutoFocus = autoFocus;
    }

    /**
     * Set use flash default state. Call this method before {@link #startCamera()}.
     * For switching flash state call {@link #setFlashEnable(boolean)} (boolean)}
     */
    public void setUseFlashPreInitState(boolean useFlash) {
        this.mUseFlash = useFlash;
    }

    /**
     * Set available barcode format. Call this method before {@link #startCamera()}.
     * Select one of constants declared in {@link Barcode} class. By default {@link Barcode#ALL_FORMATS} is selected.
     */
    public void setBarcodeFormatPreInitState(int barcodeFormat) {
        this.mBarcodeFormat = barcodeFormat;
    }

    /**
     * Set if need to play sound automatically, after success scanning
     */
    public void setPlaySoundWhenScanSuccess(boolean play) {
        this.mPlaySoundWhenScanSuccess = play;
    }

    /**
     * Change state of {@link GraphicOverlay#setDrawRect(boolean)}.
     */
    public void setDrawFoundedBarcodeRect(boolean isDrawRect) {
        this.mIsDrawRect = isDrawRect;
        if (mGraphicOverlay != null)
            mGraphicOverlay.setDrawRect(mIsDrawRect);
    }

    public void setFoundedBarcodeRectColors(@ColorInt Integer... colors) {
        if (mGraphicOverlay != null)
            mGraphicOverlay.setRectColors(colors);
    }

    /**
     * Change state of {@link GraphicOverlay#setDrawText(boolean)}
     */
    public void setDrawFoundedBarcodeResultText(boolean isDrawText) {
        this.mIsDrawText = isDrawText;
        if (mGraphicOverlay != null)
            mGraphicOverlay.setDrawText(mIsDrawText);
    }

    /**
     * Set barcode result callback
     */
    public void setBarcodeReaderListener(BarcodeReaderListener barcodeReaderListener) {
        mListener = barcodeReaderListener;
    }

    /**
     * Set sound file name. Must be inside assets folder
     */
    public void setBeepSoundFile(String fileName) {
        mBeepSoundFile = fileName;
    }

    /**
     * Change flash state
     */
    public void setFlashEnable(boolean isEnable) {
        if (mCameraSource != null) {
            mCameraSource.setFlashMode(isEnable ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        }
        mUseFlash = isEnable;
    }

    public boolean isFlashEnabled() {
        return this.mUseFlash;
    }

    /**
     * Set scan on pause. This method will not disable camera. Simply scan result would not be delivered through the callback
     */
    public void pauseScanning() {
        mIsPaused = true;
    }

    /**
     * Resume. This method will not enable camera. Simply scan result will be delivered through the given callback.
     */
    public void resumeScanning() {
        mIsPaused = false;
    }

    public boolean isOnPause() {
        return this.mIsPaused;
    }

    /**
     * Stop camera. Call this method in onPause method. Or stop camera manually if need
     */
    public void stopCamera() {
        mIsStopped = true;
        if (mPreview != null)
            mPreview.stop();
    }

    /**
     * Release camera source. Call this method inside onDestroy
     */
    public void releaseCamera() {
        if (mPreview != null)
            mPreview.release();
    }

    /**
     * This method creates and starts camera. Permission is required
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public void startCamera() {
        if (mCameraSource == null)
            createCameraSource(mAutoFocus, mUseFlash);
        startCameraSource();
        mIsStopped = false;
    }


    public boolean isStopped() {
        return mIsStopped;
    }

    /**
     * This method play the give sound file.
     */
    public void playSound() {
        playSound(mBeepSoundFile != null ? mBeepSoundFile : "beep.mp3");
    }

    public void playSound(String file) {
        MediaPlayer m = new MediaPlayer();
        try {
            if (m.isPlaying()) {
                m.stop();
                m.release();
                m = new MediaPlayer();
            }
            if (getContext() == null)
                return;
            AssetFileDescriptor descriptor = getContext().getAssets().openFd(file);
            m.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();

            m.prepare();
            m.setVolume(1f, 1f);
            m.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean b = mScaleGestureDetector.onTouchEvent(motionEvent);

        boolean c = mGestureDetector.onTouchEvent(motionEvent);

        return b || c || view.onTouchEvent(motionEvent);
    }

    @Override
    public void onScanned(Barcode barcode) {
        if (mListener != null && !mIsPaused) {
            mListener.onScanned(barcode);
            if (mPlaySoundWhenScanSuccess)
                playSound();
        }
    }

    @Override
    public void onScannedMultiple(List<Barcode> barcodeList) {
        if (mListener != null && !mIsPaused) {
            mListener.onScannedMultiple(barcodeList);
            if (mPlaySoundWhenScanSuccess)
                playSound();
        }
    }

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {
        if (mListener != null) {
            mListener.onBitmapScanned(sparseArray);
            if (mPlaySoundWhenScanSuccess)
                playSound();
        }
    }

    @Override
    public void onScanError(String errorMessage) {
        if (mListener != null) {
            mListener.onScanError(errorMessage);
        }
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }


    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(mBarcodeFormat).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);

        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        if (!barcodeDetector.isOperational()) {

            Log.e(TAG, "Detector dependencies are not yet available.");

            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = false;

            if (getContext() != null)
                hasLowStorage = getContext().registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(getContext(), R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.e(TAG, getContext().getString(R.string.low_storage_error));
            }

            if (mListener != null)
                mListener.onBarcodeNotOperationalYetError();

            return;
        }

        CameraSource.Builder builder = new CameraSource.Builder(getContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(getScreenWidth(), getScreenHeight())
                .setRequestedFps(15.0f);

        builder = builder.setFocusMode(
                autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    private void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());
        if (code != ConnectionResult.SUCCESS) {
            if (mListener != null)
                mListener.onPlayServicesNotAvailableError();
            return;
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            onScanned(best);
            return true;
        }
        return false;
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public interface BarcodeReaderListener {
        void onScanned(Barcode barcode);

        void onScannedMultiple(List<Barcode> barcodeList);

        void onBitmapScanned(SparseArray<Barcode> sparseArray);

        void onScanError(String errorMessage);

        void onPlayServicesNotAvailableError();

        void onBarcodeNotOperationalYetError();
    }
}
