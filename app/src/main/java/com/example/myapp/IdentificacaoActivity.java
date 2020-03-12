package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Locale;

public class IdentificacaoActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private static final String TAG = "AndroidCamera";
    private ViewHolder mViewHolder = new ViewHolder();
    private TextureView cameraView;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private LocationManager locationManager;
    private String provider;
    private Location location;
    private static final int REQUEST_FINE_LOCATION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identificacao);

        this.mViewHolder.latitude = findViewById(R.id.text_latitude);
        this.mViewHolder.longitude = findViewById(R.id.text_longitude);
        this.mViewHolder.buttonContinuar = findViewById(R.id.button_continuar);

        this.mViewHolder.buttonContinuar.setOnClickListener(this);

        cameraView = findViewById(R.id.camera_view);
        assert cameraView != null;
        cameraView.setSurfaceTextureListener(textureListener);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        getLocation();
    }

    private static class ViewHolder {
        Button buttonContinuar;
        TextView latitude;
        TextView longitude;
    }

    private void getLocation () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(IdentificacaoActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            return;
        }
        location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            Log.i("LocationProvider", "Provider " + provider + " foi selecionado.");
            onLocationChanged(location);
        } else {
            this.mViewHolder.latitude.setText(R.string.localizacao_nao_disponivel);
            this.mViewHolder.longitude.setText(R.string.localizacao_nao_disponivel);
        }
    }

    private void requestLocationUpdates () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(IdentificacaoActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    public void onLocationChanged (Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        this.mViewHolder.latitude.setText(String.format(new Locale("pt", "BR"), "LAT: %.6f", lat));
        this.mViewHolder.longitude.setText(String.format(new Locale("pt", "BR"), "LNG: %.6f", lng));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread () {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread () {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview () {
        try {
            SurfaceTexture texture = cameraView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (null == cameraDevice) return;

                    cameraCaptureSessions = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(IdentificacaoActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera (){
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "camera open");

        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert configurationMap != null;
            imageDimension = configurationMap.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(IdentificacaoActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "S U C C");
    }

    protected void updatePreview () {
        if (null == cameraDevice) {
            Log.e(TAG, "errrrrou");
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(IdentificacaoActivity.this, "u just got bamboozled", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (cameraView.isAvailable()) {
            openCamera();
        } else {
            cameraView.setSurfaceTextureListener(textureListener);
        }

        requestLocationUpdates();
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();

        locationManager.removeUpdates(this);

        super.onPause();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_continuar) {
            Intent intent = new Intent(this, ListaActivity.class);
            startActivity(intent);
        }
    }
}
