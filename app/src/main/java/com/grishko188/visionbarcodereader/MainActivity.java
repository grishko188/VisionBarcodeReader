package com.grishko188.visionbarcodereader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.grishko188.visionlibrary.BarcodeReaderView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements BarcodeReaderView.BarcodeReaderListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private BarcodeReaderView barcodeReader;
    private Button mToggleFlashButton;
    private Button mStopStartButton;
    private Button mResumePauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeReader = findViewById(R.id.barcode_view);
        barcodeReader.setPlaySoundWhenScanSuccess(false);
        barcodeReader.setBarcodeFormatPreInitState(Barcode.ALL_FORMATS);
        barcodeReader.setBarcodeReaderListener(this);
        barcodeReader.setFoundedBarcodeRectColors(Color.RED);

        mToggleFlashButton = findViewById(R.id.toggle_flash);
        mResumePauseButton = findViewById(R.id.pause_scanning);
        mStopStartButton = findViewById(R.id.stop_camera);

        mToggleFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                barcodeReader.setFlashEnable(!barcodeReader.isFlashEnabled());
                mToggleFlashButton.setText(barcodeReader.isFlashEnabled() ? "Disable flash" : "Enable flash");
            }
        });

        mResumePauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (barcodeReader.isOnPause()) {
                    barcodeReader.resumeScanning();
                } else {
                    barcodeReader.pauseScanning();
                }
                mResumePauseButton.setText(barcodeReader.isOnPause() ? "Resume" : "Pause");
            }
        });

        mStopStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (barcodeReader.isStopped()) {
                    if (barcodeReader != null)
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            barcodeReader.startCamera();
                        }
                } else {
                    barcodeReader.stopCamera();
                }
                mStopStartButton.setText(barcodeReader.isStopped() ? "Start" : "Stop");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    private void startCamera() {
        if (barcodeReader != null)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                barcodeReader.startCamera();
                return;
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeReader != null)
            barcodeReader.stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeReader != null)
            barcodeReader.releaseCamera();
    }

    @Override
    public void onScanned(final Barcode barcode) {
        Log.d(TAG, "onScanned: " + barcode.displayValue);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Barcode: " + barcode.displayValue, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onScannedMultiple(List<Barcode> barcodes) {
        Log.d(TAG, "onScannedMultiple: " + barcodes.size());

        StringBuilder codes = new StringBuilder();
        for (Barcode barcode : barcodes) {
            codes.append(barcode.displayValue).append(", ");
        }

        final String finalCodes = codes.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Barcodes: " + finalCodes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBitmapScanned(SparseArray<Barcode> sparseArray) {

    }

    @Override
    public void onScanError(final String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Barcodes: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPlayServicesNotAvailableError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Google play services not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBarcodeNotOperationalYetError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "On barcode not yet operational, please wait for a while", Toast.LENGTH_SHORT).show();
            }
        });
        new Handler().postDelayed(this::startCamera, 1000);
    }
}
