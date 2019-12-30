package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.Arrays;

public class WavelengthCalibrationActivity extends Activity {

    private static final String TAG = "Wavelength Calibration";
    public static final String CALIBRATION_KEY = "Slope and intercept";
    private Button validateButton;
    private Button Button436;
    private Button Button488;
    private Button Button546;
    private Button Button612;
    private Button currentButton;
    private int currentIndex;
    private WavelengthCalibrationView wavelengthCalibrationView;
    private TextureView textureView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSession;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private android.os.Handler backgroundHandler;
    private ContextWrapper contextWrapper;
    private int[] wavelengthRaysPositions = new int[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.wavelength_cal_layout);
        this.contextWrapper = new ContextWrapper(getApplicationContext());

        //adding custom surface view above the texture view
        ConstraintLayout calibrationViewLayout = findViewById(R.id.calibrationViewLayout);
        this.wavelengthCalibrationView = new WavelengthCalibrationView(this);
        calibrationViewLayout.addView(this.wavelengthCalibrationView);

        enableListeners();
    }

    /**
     * Adds listeners on various UI components
     */
    private void enableListeners() {

        /* Adding listener to texture */

        this.textureView = findViewById(R.id.texture);
        assert this.textureView != null;
        this.textureView.setSurfaceTextureListener(this.textureListener);

        /* Adding listeners to the buttons */

        this.validateButton = findViewById(R.id.validateCalButton);
        assert this.validateButton != null;
        this.validateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(Integer ent: wavelengthRaysPositions){
                    if(ent == 0){
                        Toast.makeText(getApplicationContext(),"Missing calibration for one wavelength value",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Intent intent = new Intent(WavelengthCalibrationActivity.this,CameraActivity.class);
                double[] lineData = findSlopeAndIntercept();
                intent.putExtra(CALIBRATION_KEY,lineData);
                startActivity(intent);
            }
        });

        this.Button436 = findViewById(R.id.Button436);
        assert this.Button436 != null;
        this.Button436.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentButton != null){
                    currentButton.setBackgroundColor(Color.argb(100,187,222,251));
                }
                currentButton = Button436;
                currentButton.setBackgroundColor(Color.CYAN);
                currentIndex = 0;
            }
        });

        this.Button488 = findViewById(R.id.Button488);
        assert this.Button488 != null;
        this.Button488.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentButton != null){
                    currentButton.setBackgroundColor(Color.argb(100,187,222,251));
                }
                currentButton = Button488;
                currentButton.setBackgroundColor(Color.CYAN);
                currentIndex = 1;
            }
        });

        this.Button546 = findViewById(R.id.Button546);
        assert this.Button546 != null;
        this.Button546.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentButton != null){
                    currentButton.setBackgroundColor(Color.argb(100,187,222,251));
                }
                currentButton = Button546;
                currentButton.setBackgroundColor(Color.CYAN);
                currentIndex = 2;
            }
        });

        this.Button612 = findViewById(R.id.Button612);
        assert this.Button612 != null;
        this.Button612.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentButton != null){
                    currentButton.setBackgroundColor(Color.argb(100,187,222,251));
                }
                currentButton = Button612;
                currentButton.setBackgroundColor(Color.CYAN);
                currentIndex = 3;
            }
        });
    }

    private double[] findSlopeAndIntercept() {
        double[] result = new double[2];
        double slope = (double)(436 - 612)/(wavelengthRaysPositions[0]-wavelengthRaysPositions[3]);
        double intercept = 400.0;
        result[0] = slope;
        result[1] = intercept;
        return result;
    }

    /**
     * Displays the calibration line
     * */
    public void displayLine(){
        this.wavelengthCalibrationView.drawLine();
    }

    /**
     * stores the current wavelength ray position. If the calibration line is misplaced, the value is not stored and an error message is displayed
     * */
    public void updateWaveLengthPositions() {
        if(this.currentButton != null){
            int currentLinePosition = this.wavelengthCalibrationView.getXPositionOfDrawnLine();
            for(int i = 0; i < wavelengthRaysPositions.length; i++){
                if(i < currentIndex && wavelengthRaysPositions[i] > currentLinePosition){ // check if values in the tab before the currentIndex value are <= to the currentIndex value
                    Toast.makeText(this,"The selected order is not correct",Toast.LENGTH_SHORT).show();
                    return;
                }else if(i > currentIndex && wavelengthRaysPositions[i] > 0  && wavelengthRaysPositions[i] <= currentLinePosition){ // check if values in the tab after the currentIndex value are >= to the currentIndex value
                    Toast.makeText(this,"The selected order is not correct",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            this.wavelengthRaysPositions[this.currentIndex] = currentLinePosition;
        }
    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    // "listener" of the camera device, calls various method depending on the CameraDevice state
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int i) {
            if(cameraDevice != null){
                //cameraDevice.close();
                //cameraDevice = null;
            }
        }
    };

    /**
     * opens the camera (if allowed), activates flash light and sets image dimension for capture
     */
    private void openCamera() throws SecurityException{
        try{
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            this.cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(this.cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            this.imageDimension =  map.getOutputSizes(SurfaceTexture.class)[0];
            cameraManager.openCamera(cameraId,stateCallback,null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Creates a camera preview, a CaptureSession and sets various parameters for this CaptureSession (calls disableAutmatics method)
     */
    protected void createCameraPreview(){
        try{
            SurfaceTexture texture = this.textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(this.imageDimension.getWidth(),this.imageDimension.getHeight());
            Surface surface = new Surface(texture);
            this.captureRequestBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            this.captureRequestBuilder.addTarget(surface);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            };
            this.cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession captureSession) {
                    if(cameraDevice == null) return;
                    cameraCaptureSession = captureSession;
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    disableAutomatics(captureRequestBuilder,cameraCaptureSession,captureListener);
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession captureSession) {
                    Toast.makeText(com.projet3a.rmycordeau_mirani.projet3a.WavelengthCalibrationActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            },null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * disables some camera automatics (such as auto focus, lens stabilization) for the specified CameraCaptureSession
     */
    private void disableAutomatics(CaptureRequest.Builder captureBuilder, CameraCaptureSession session, CameraCaptureSession.CaptureCallback callback) {
        try {
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
            captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_OFF);

            CaptureRequest captureRequest = captureBuilder.build();
            session.setRepeatingRequest(captureRequest, callback, this.backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e(TAG,"On Pause");
        if(this.cameraDevice != null){
            this.cameraDevice.close();
        }
    }
}
