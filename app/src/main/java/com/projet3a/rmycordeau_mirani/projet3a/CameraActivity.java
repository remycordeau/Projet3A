package com.projet3a.rmycordeau_mirani.projet3a;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */

public class CameraActivity extends Activity {

    private static final String TAG = "Camera Activity";
    private Button takePictureButton;
    private TextureView textureView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSession;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private HandlerThread backgroundThread;
    private android.os.Handler backgroundHandler;
    private ContextWrapper contextWrapper;
    private double[] graphData = null;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        this.contextWrapper = new ContextWrapper(getApplicationContext());
        this.textureView = findViewById(R.id.texture);
        assert this.textureView != null;
        this.textureView.setSurfaceTextureListener(this.textureListener);
        this.takePictureButton = findViewById(R.id.btn_takepicture);
        assert this.takePictureButton != null;
        this.takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayProcessingCircle();
                setImagesCapture();
            }
        });
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

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int i) {
            if(cameraDevice != null){
                cameraDevice.close();
                cameraDevice = null;
            }
        }
    };

    protected void startBackgroundThread(){
        this.backgroundThread = new HandlerThread("Camera background");
        this.backgroundThread.start();
        this.backgroundHandler = new android.os.Handler(this.backgroundThread.getLooper());
    }

    protected void stopBackgroundThread(){
        this.backgroundThread.quitSafely();
        try{
            this.backgroundThread.join();
            this.backgroundThread = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    protected void createCameraPreview(){
        try{
            SurfaceTexture texture = this.textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(this.imageDimension.getWidth(),this.imageDimension.getHeight());
            Surface surface = new Surface(texture);
            this.captureRequestBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            this.captureRequestBuilder.addTarget(surface);
            this.cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession captureSession) {
                    if(cameraDevice == null) return;
                    cameraCaptureSession = captureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession captureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            },null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    protected void updatePreview(){
        if(this.cameraDevice == null){
            Log.e(TAG, "updatePreview error, return");
        }
        this.captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            this.cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, this.backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            int version = Build.VERSION.SDK_INT;
            if(version <= 22){
                Log.e(TAG,"Cannot activate flash light, API level too low");
                Toast flashError = Toast.makeText(CameraActivity.this,"Cannot activate flash light, API level too low",Toast.LENGTH_LONG);
                flashError.show();

            }else{
                cameraManager.setTorchMode(this.cameraId,true);
            }
            this.cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(this.cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            this.imageDimension =  map.getOutputSizes(SurfaceTexture.class)[0];
            if(version > 22){
                requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
                if(this.contextWrapper.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA},200);
                }
            }
            cameraManager.openCamera(cameraId,stateCallback,null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    protected void setImagesCapture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(this.cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = this.imageDimension.getWidth();
            int height = this.imageDimension.getHeight();
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());

            //disabling automatics adjustments of camera (maybe call AE_MODE_OFF/AE_MODE_LOCKED
            //captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK,true);
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    int width = image.getWidth();
                    int height = image.getHeight();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    int[] rgb =  RGBDecoder.getRGBCode(bytes,width,height);
                    double[] intensity = RGBDecoder.getImageIntensity(rgb);
                    graphData = RGBDecoder.computeIntensityMean(intensity,width,height);
                    updateUIGraph();
                }
            };
            reader.setOnImageAvailableListener(readerListener, this.backgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_OFF);
                        captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
                        captureBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                        captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                        captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, .2f);
                        session.capture(captureBuilder.build(), captureListener,backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, this.backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updateUIGraph() {

        assert this.graphData != null;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                GraphView graphView = findViewById(R.id.intensityGraph);

                //setting up X and Y axis title
                GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
                gridLabelRenderer.setHorizontalAxisTitle("Pixel position");
                gridLabelRenderer.setVerticalAxisTitle("Pixel intensity");

                // checking if the graphic contains series
                if(!graphView.getSeries().isEmpty()){
                    graphView.removeAllSeries();
                }

                //adding series to graph
                DataPoint[] values = new DataPoint[graphData.length];
                for(int i = 0; i < graphData.length; i++){
                    values[i] = new DataPoint(i,graphData[i]);
                }
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
                graphView.addSeries(series);
                graphView.setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void displayProcessingCircle(){
     runOnUiThread(new Runnable() {
         @Override
         public void run() {
             ProgressBar progressBar = findViewById(R.id.progressBar);
             progressBar.setVisibility(View.VISIBLE);
             Toast.makeText(CameraActivity.this,"Creating graph...",Toast.LENGTH_SHORT).show();
         }
     });
    }
}
