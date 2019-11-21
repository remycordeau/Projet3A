package com.projet3a.rmycordeau_mirani.projet3a;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */

public class CameraActivity extends Activity {

    private static final String TAG = "Camera Activity";
    public static final String GRAPH_DATA_KEY = "Graph Data";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Button takePictureButton;
    private Button saveReferenceButton;
    private Button saveDataButton;
    private Button clearGraphButton;
    private Button calibrateButton;
    private SeekBar calibrationLeft;
    private SeekBar calibrationRight;
    private SeekBar calibrationTop;
    private SeekBar calibrationBottom;
    private int lastSeekBarValLeft = 0;
    private int lastSeekBarValRight = 0;
    private int lastSeekBarValBottom = 0;
    private int lastSeekBarValTop = 0;
    private Boolean isDefaultCalibrationDone = false;
    private Boolean isReferenceSaved = false;
    private Boolean isSampleSaved = false;
    private Boolean isCalibrating = false;
    private CameraCalibrationView cameraCalibrationView;
    private TextureView textureView;
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSession;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private HandlerThread backgroundThread;
    private android.os.Handler backgroundHandler;
    private ContextWrapper contextWrapper;
    private CameraHandler cameraHandler;
    private double[] graphData = null;
    private HashMap<String,double[]> definitiveMeasures = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        this.contextWrapper = new ContextWrapper(getApplicationContext());
        this.cameraHandler = new CameraHandler();

        //adding custom surface view above the texture view
        ConstraintLayout calibrationViewLayout = findViewById(R.id.calibrationViewLayout);
        this.cameraCalibrationView = new CameraCalibrationView(this);
        calibrationViewLayout.addView(this.cameraCalibrationView);

        enableListeners();
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
        this.cameraHandler.closeCamera(this.cameraDevice);
        stopBackgroundThread();
        super.onPause();
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

        this.takePictureButton = findViewById(R.id.btn_takepicture);
        assert this.takePictureButton != null;
        this.takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isCalibrating) return;
                if(isDefaultCalibrationDone){
                    displayCreationMessage();
                    setImagesCapture();
                }else{
                    Toast.makeText(CameraActivity.this,"Please calibrate before taking picture",Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.saveReferenceButton = findViewById(R.id.save_reference_button);
        assert this.saveReferenceButton != null;
        this.saveReferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(isCalibrating) return;
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
                if(isCalibrating) return;
                try {
                    GraphView graphView = findViewById(R.id.intensityGraph);
                    if (isReferenceSaved && graphView.getSeries().size() >= 2){
                        savePicture("Sample");
                        if(graphData != null && isReferenceSaved && isSampleSaved) {
                            Intent intent = new Intent(CameraActivity.this, AnalysisActivity.class);
                            assert definitiveMeasures.containsKey("Reference") && definitiveMeasures.containsKey("Sample");
                            intent.putExtra(GRAPH_DATA_KEY, definitiveMeasures);
                            startActivity(intent);
                        }
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
                if(isCalibrating) return;
                clearGraph();
            }
        });

        this.calibrateButton = findViewById(R.id.calibrationButton);
        assert this.calibrateButton != null;
        this.calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isDefaultCalibrationDone){
                    isDefaultCalibrationDone = true;
                }
                if(!isCalibrating){
                    enableCalibration();
                }else{
                    endCalibration();
                }
            }
        });
    }

    /**
     * Adds listeners on seekbars when calibrationCameraView is fully initialized
     */
    public void enableSeekBarsListeners(){

        this.calibrationBottom = findViewById(R.id.calibrationBottom);
        assert this.calibrationBottom != null;
        this.calibrationBottom.setMax(this.cameraCalibrationView.getHeight());
        this.calibrationBottom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int shift = Math.abs(progress - lastSeekBarValBottom);
                if (progress > lastSeekBarValBottom) { /*Seekbar moved to the right*/
                    cameraCalibrationView.moveLine("Bottom line",shift);
                }
                else if (progress < lastSeekBarValBottom) { /*Seekbar moved to the left*/
                    cameraCalibrationView.moveLine("Bottom line",-shift);
                }
                lastSeekBarValBottom = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.calibrationTop = findViewById(R.id.calibrationTop);
        assert this.calibrationTop != null;
        this.calibrationTop.setMax(this.cameraCalibrationView.getHeight());
        this.calibrationTop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int shift = Math.abs(progress - lastSeekBarValTop);
                if (progress > lastSeekBarValTop) { /*Seekbar moved to the right*/
                    cameraCalibrationView.moveLine("Top line",shift);
                }
                else if (progress < lastSeekBarValTop) { /*Seekbar moved to the left*/
                    cameraCalibrationView.moveLine("Top line",-shift);
                }
                lastSeekBarValTop = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.calibrationRight = findViewById(R.id.calibrationRight);
        assert this.calibrationRight != null;
        this.calibrationRight.setMax(this.cameraCalibrationView.getWidth());
        this.calibrationRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    int shift = Math.abs(progress - lastSeekBarValRight);
                    if (progress > lastSeekBarValRight) { /*Seekbar moved to the right*/
                        cameraCalibrationView.moveLine("Right line",shift);
                    }
                    else if (progress < lastSeekBarValRight) { /*Seekbar moved to the left*/
                        cameraCalibrationView.moveLine("Right line",-shift);
                    }
                    lastSeekBarValRight = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.calibrationLeft = findViewById(R.id.calibrationLeft);
        assert this.calibrationLeft != null;
        this.calibrationLeft.setMax(this.cameraCalibrationView.getWidth());
        this.calibrationLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int shift = Math.abs(progress - lastSeekBarValLeft);
                if (progress > lastSeekBarValLeft) { /*Seekbar moved to the right*/
                    cameraCalibrationView.moveLine("Left line",shift);
                }
                else if (progress < lastSeekBarValLeft) { /*Seekbar moved to the left*/
                    cameraCalibrationView.moveLine("Left line",-shift);
                }
                lastSeekBarValLeft = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void endCalibration() {

        this.isCalibrating = false;
        this.cameraCalibrationView.eraseLines();

        findViewById(R.id.intensityGraph).setVisibility(View.VISIBLE);
        findViewById(R.id.calibrationBottom).setVisibility(View.INVISIBLE);
        findViewById(R.id.calibrationTop).setVisibility(View.INVISIBLE);
        findViewById(R.id.calibrationLeft).setVisibility(View.INVISIBLE);
        findViewById(R.id.calibrationRight).setVisibility(View.INVISIBLE);
        findViewById(R.id.TextRight).setVisibility(View.INVISIBLE);
        findViewById(R.id.TextTop).setVisibility(View.INVISIBLE);
        findViewById(R.id.TextLeft).setVisibility(View.INVISIBLE);
        findViewById(R.id.TextBottom).setVisibility(View.INVISIBLE);

        this.cameraCalibrationView.getCaptureZone();
    }

    private void enableCalibration() {
        this.isCalibrating = true;

        GraphView graphView = findViewById(R.id.intensityGraph);
        if (graphView.getVisibility() == View.VISIBLE) {
            graphView.setVisibility(View.INVISIBLE);
        }
        findViewById(R.id.calibrationBottom).setVisibility(View.VISIBLE);
        findViewById(R.id.calibrationTop).setVisibility(View.VISIBLE);
        findViewById(R.id.calibrationLeft).setVisibility(View.VISIBLE);
        findViewById(R.id.calibrationRight).setVisibility(View.VISIBLE);
        findViewById(R.id.TextRight).setVisibility(View.VISIBLE);
        findViewById(R.id.TextTop).setVisibility(View.VISIBLE);
        findViewById(R.id.TextLeft).setVisibility(View.VISIBLE);
        findViewById(R.id.TextBottom).setVisibility(View.VISIBLE);

        this.cameraCalibrationView.drawLines();
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

    /**
     * Starts background thread for camera
    */
    protected void startBackgroundThread(){
        this.backgroundThread = new HandlerThread("Camera background");
        this.backgroundThread.start();
        this.backgroundHandler = new android.os.Handler(this.backgroundThread.getLooper());
    }

    /**
     * Stops background thread
     */
    protected void stopBackgroundThread(){
        this.backgroundThread.quitSafely();
        try{
            this.backgroundThread.join();
            this.backgroundThread = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * opens the camera (if allowed), activates flash light and sets image dimension for capture
     */
    private void openCamera(){
        int version = Build.VERSION.SDK_INT;
        if(version > 22){
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
            if(this.contextWrapper.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
            }
        }
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        this.imageDimension = this.cameraHandler.openCamera(this,cameraManager);
        try{
            this.cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId,stateCallback,null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Called when a access to the camera is requested. If the permission is denied, this function ends the current activity
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(CameraActivity.this, "Sorry, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
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
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            },null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    protected void setImagesCapture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            openCamera();
            return;
        }

        int width = this.textureView.getWidth();
        int height = this.textureView.getHeight();

        Bitmap bitmap = this.textureView.getBitmap(width,height);
        int [] captureZone = this.cameraCalibrationView.getCaptureZone();
        Bitmap captureZoneBitmap = Bitmap.createBitmap(bitmap,captureZone[0],captureZone[1],captureZone[2],captureZone[3]);

        int[] rgb =  RGBDecoder.getRGBCode(captureZoneBitmap,captureZone[2],captureZone[3]);
        double[] intensity = RGBDecoder.getImageIntensity(rgb);
        this.graphData = RGBDecoder.computeIntensityMean(intensity,captureZone[2],captureZone[3]);
        saveCurrentMeasure();
        updateUIGraph();
    }

    /**
     * Updates the UI graph when a new picture is taken
     * */
    private void updateUIGraph() {

        final GraphView graphView = findViewById(R.id.intensityGraph);

        Thread updateGraphThread = new Thread(new Runnable() {
            @Override
            public void run() {

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

                assert graphData != null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(graphView.getVisibility() != View.VISIBLE){
                            graphView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
        updateGraphThread.start();
    }

    /**
     * Clears graph and updates UI accordingly
     */
    public void clearGraph(){
        if(this.graphData == null){
            return;
        }else{
            GraphView graphView = findViewById(R.id.intensityGraph);
            if(graphView.getSeries().size() > 0)
                graphView.removeAllSeries();
            this.isReferenceSaved = false;
            this.isSampleSaved = false;
        }
    }

    /**
     * saves the frame's data in a map
     */
    private void saveCurrentMeasure(){
        if(!this.isReferenceSaved && !this.isSampleSaved){ // we are trying to capture the reference
            this.definitiveMeasures.put("Reference",this.graphData);
        }else if(this.isReferenceSaved && !this.isSampleSaved){
            this.definitiveMeasures.put("Sample",this.graphData);
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
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_OFF);

            CaptureRequest captureRequest = captureBuilder.build();
            session.setRepeatingRequest(captureRequest, callback, this.backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a message in the UI
     */
    private void displayCreationMessage(){
     runOnUiThread(new Runnable() {
         @Override
         public void run() {
             Toast.makeText(CameraActivity.this,"Creating graph...",Toast.LENGTH_SHORT).show();
         }
     });
    }

    /**
     * Saves picture data in a file (stored in Documents folder)
     */
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
                    if(text.equals("Reference")){
                        this.isReferenceSaved = true;
                    }else if(text.equals("Sample")){
                        this.isSampleSaved = true;
                    }
                    Toast.makeText(CameraActivity.this, "File successfully saved",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
