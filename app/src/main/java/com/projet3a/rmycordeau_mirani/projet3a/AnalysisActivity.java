package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;


public class AnalysisActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Analysis Activity";
    private Bundle cameraActivityData;
    private HashMap<String,double[]> graphData;
    private double[] wavelengthCalibrationData = null;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private GoogleApiClient googleApiClient;
    private double latitude;
    private double longitude;
    private int dataSize;
    private TextView lastKnownPosition;
    private String position;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analysis_activity_layout);
        this.cameraActivityData = getIntent().getExtras();
        this.graphData = (HashMap<String,double[]>) this.cameraActivityData.get(CameraActivity.GRAPH_DATA_KEY);
        try{
            if(this.graphData.containsKey("Reference") && this.graphData.containsKey("Sample")){
                drawGraph();
            }else{
                Toast.makeText(this,"Missing data to draw graph",Toast.LENGTH_SHORT).show();
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        enableShareButton();
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        buildGoogleApiClient();
        this.googleApiClient.connect();
    }

    /**
     * Adds listener to the share button
     * */
    private void enableShareButton() {
        Button shareButton = findViewById(R.id.shareButton);
        if(shareButton != null){
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openEmail();
                }
            });
        }
    }

    /**
     * opens e-mail dialog box with file containing data as attachment
    */
    private void openEmail() {
        File file = saveMeasurements();
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {""});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Smart Spectro measurements");
        if(this.position != null){
            emailIntent.putExtra(Intent.EXTRA_TEXT, this.position);
        }
        if(file != null){
            Uri uri = FileProvider.getUriForFile(AnalysisActivity.this, BuildConfig.APPLICATION_ID + ".provider",file);
            emailIntent.putExtra(Intent.EXTRA_STREAM,uri);
        }
        try{
            startActivity(emailIntent);
        }catch(ActivityNotFoundException e){
            Toast.makeText(this,"No email app found on this device",Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * Saves analysis activity's results in text file and returns file
     * */
    private File saveMeasurements(){
        if(this.graphData == null){
            Toast.makeText(AnalysisActivity.this,"Error while trying to save file : no data found",Toast.LENGTH_SHORT).show();
            return null;
        }else{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());
            OutputStream outputStream;
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Transmission_" + currentDateAndTime + ".txt");
            try {
                outputStream = new FileOutputStream(file, false);

                GraphView graphView = findViewById(R.id.resultGraph);
                Series data = graphView.getSeries().get(0);
                Iterator it = data.getValues(0, this.dataSize - 1);
                while (it.hasNext()) {
                    DataPoint dataPoint = (DataPoint) it.next();
                    outputStream.write((dataPoint.getX() + ",").getBytes());
                    outputStream.write((dataPoint.getY() + "\n").getBytes());
                }

                outputStream.close();
                Toast.makeText(AnalysisActivity.this, "File successfully saved", Toast.LENGTH_SHORT).show();
                return file;

            }catch (IOException e){
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Draws transmission graph using data from camera activity
     * */
    private void drawGraph(){

        final GraphView graphView = findViewById(R.id.resultGraph);

        //getting sample and reference data
        double[] referenceData = this.graphData.get("Reference");
        double[] sampleData = this.graphData.get("Sample");
        this.dataSize = referenceData.length;

        //getting optional wavelength calibration data
        this.wavelengthCalibrationData = this.cameraActivityData.getDoubleArray(WavelengthCalibrationActivity.CALIBRATION_KEY);

        //creating graph series
        String xAxisTitle, maxTransmissionText;
        DataPoint maxTransmission = new DataPoint(0,0.0);
        DataPoint[] values = new DataPoint[referenceData.length];
        if(this.wavelengthCalibrationData != null){
            xAxisTitle = "Wavelength (nm)";
            double slope = this.wavelengthCalibrationData[0];
            double intercept = this.wavelengthCalibrationData[1];
            for(int i = 0; i < referenceData.length; i++) {
                int x = (int) (slope * i + intercept);
                if (referenceData[i] != 0) {
                    values[i] = new DataPoint(x, sampleData[i] / referenceData[i]);
                    if(values[i].getY() > maxTransmission.getY()){
                        maxTransmission = values[i];
                    }
                }else{
                    values[i] = new DataPoint(x, 0);
                }
            }
            maxTransmissionText = "Peak found at "+maxTransmission.getX()+" nm and is "+maxTransmission.getY();

            //setting manually X axis max and min bounds to see all points on graph
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMaxX((referenceData.length-1)*slope + intercept);
            graphView.getViewport().setMinX(intercept);
        }else{
            xAxisTitle = "Pixel position";
            for(int i = 0; i < referenceData.length; i++){
                if (referenceData[i] != 0) {
                    values[i] = new DataPoint(i, sampleData[i] / referenceData[i]);
                    if(values[i].getY() > maxTransmission.getY()){
                        maxTransmission = values[i];
                    }
                } else {
                    values[i] = new DataPoint(i, 0);
                }
            }
            maxTransmissionText = "Peak found at "+maxTransmission.getX()+" px and is "+maxTransmission.getY();

            //setting manually X axis bound to see all points on graph
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMaxX((double)referenceData.length-1);
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);

        //setting up X and Y axis title
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setHorizontalAxisTitle(xAxisTitle);
        gridLabelRenderer.setVerticalAxisTitle("Transmission");
        graphView.addSeries(series);

        TextView maxTransmissionView = findViewById(R.id.maxTransmission);
        maxTransmissionView.setText(maxTransmissionText);
    }

    /**
     * Displays last known position
     * */
    private void getLastKnownPosition() {
        this.lastKnownPosition = findViewById(R.id.lastKnownPosition);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        this.position = "Data measured at position ("+this.latitude+","+this.longitude+") on "+currentDateAndTime;
        this.lastKnownPosition.setText(this.position);
        this.lastKnownPosition.setVisibility(View.VISIBLE);
    }

    /**
     * Stops locations updates by google play services
     * */
    private void stopLocationUpdates(){
        this.fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
    }

    /**
     * Starts locations updates by google play services
     * */
    private void startLocationUpdates(){
        this.fusedLocationProviderClient.requestLocationUpdates(this.locationRequest,this.locationCallback, Looper.getMainLooper());
    }

    /**
     * Starts google api client to get location updates
     * */
    private void buildGoogleApiClient() {
        this.googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    /**
     * defines location request and callback to get device position and starts location updates
     * */
    public void onConnected(@Nullable Bundle bundle) {
        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(1000);
        this.locationRequest.setFastestInterval(1000);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()){
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
                stopLocationUpdates();
                getLastKnownPosition();
            }
        };

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
