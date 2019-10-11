package com.projet3a.rmycordeau_mirani.projet3a;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */

public class CameraActivity extends Activity {

    private static final String TAG = "Camera Activity";
    private Button takePictureButton;
    private Button saveReferenceButton;
    private Button saveDataButton;
    private Button clearGraphButton;
    private Boolean isReferenceSaved = false;
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
        enableListeners();
    }

    private void enableListeners() {

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

        this.saveReferenceButton = findViewById(R.id.save_reference_button);
        assert this.saveReferenceButton != null;
        this.saveReferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    savePicture("Reference");
                }catch (IOException e){
                    Log.e(TAG,e.toString());
                }
            }
        });

        this.saveDataButton = findViewById(R.id.save_picture_button);
        assert this.saveDataButton != null;
        this.saveDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (isReferenceSaved){
                        savePicture("Data");
                    }else{
                        Toast.makeText(CameraActivity.this,"You must first save the reference",Toast.LENGTH_SHORT).show();
                    }
                }catch (IOException e){
                    Log.e(TAG,e.toString());
                }
            }
        });

        this.clearGraphButton = findViewById(R.id.clearButton);
        assert this.clearGraphButton != null;
        this.clearGraphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearGraph();
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
                    disableAutomatics(captureRequestBuilder,captureSession,captureListener);
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
                    requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
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
                Toast.makeText(CameraActivity.this, "Sorry, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
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
                    image.close();
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
                    disableAutomatics(captureBuilder,session,captureListener);
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

        final GraphView graphView = findViewById(R.id.intensityGraph);

        //setting up X and Y axis title
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setHorizontalAxisTitle("Pixel position");
        gridLabelRenderer.setVerticalAxisTitle("Pixel intensity");

        // checking if the graphic contains series
        if(!graphView.getSeries().isEmpty() && !isReferenceSaved){
            graphView.removeAllSeries();
        }

        //adding series to graph
        DataPoint[] values = new DataPoint[graphData.length];
        for(int i = 0; i < graphData.length; i++){
            values[i] = new DataPoint(i,graphData[i]);
        }

        //if the reference is saved and not the data, we remove previous data
        if(isReferenceSaved){
            if(graphView.getSeries().size() > 1){
                graphView.getSeries().remove(1);
            }
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
        if(isReferenceSaved){
            series.setColor(Color.RED);
        }
        graphView.addSeries(series);

        assert this.graphData != null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                graphView.setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }
        });
    }

    public void clearGraph(){
        if(this.graphData == null){
            return;
        }else{
            GraphView graphView = findViewById(R.id.intensityGraph);
            if(graphView.getSeries().size() > 0)
                graphView.removeAllSeries();
            this.isReferenceSaved = false;
        }
    }

    private void disableAutomatics(CaptureRequest.Builder captureBuilder, CameraCaptureSession session, CameraCaptureSession.CaptureCallback callback) {
        try {
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
            captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_OFF);

            CaptureRequest captureRequest = captureBuilder.build();
            session.setRepeatingRequest(captureRequest, callback, this.backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

    private void savePicture(String text) throws IOException{
        if(this.graphData == null){
            Toast.makeText(CameraActivity.this,"Please press Take Picture button before saving",Toast.LENGTH_SHORT).show();
        }else{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());
            OutputStream outputStream = null;
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),text+"_"+currentDateAndTime+".txt");
            try{
                outputStream = new FileOutputStream(file,false);
                for(int i = 0; i < this.graphData.length; i++){
                    outputStream.write((i+",").getBytes());
                    outputStream.write((this.graphData[i]+"\n").getBytes());
                }
            }finally {
                if(outputStream != null){
                    outputStream.close();
                    this.isReferenceSaved = true;
                    Toast.makeText(CameraActivity.this, "File successfully saved",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
