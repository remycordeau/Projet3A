package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


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
    private TextView lastKnownPosition;

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
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        buildGoogleApiClient();
        this.googleApiClient.connect();
    }

    private void drawGraph(){

        //getting sample and reference data
        double[] referenceData = this.graphData.get("Reference");
        double[] sampleData = this.graphData.get("Sample");

        //getting optional wavelength calibration data
        this.wavelengthCalibrationData = this.cameraActivityData.getDoubleArray(WavelengthCalibrationActivity.CALIBRATION_KEY);

        //creating graph series
        DataPoint[] values = new DataPoint[referenceData.length];
        if(this.wavelengthCalibrationData != null){
            double slope = this.wavelengthCalibrationData[0];
            double intercept = this.wavelengthCalibrationData[1];
            for(int i = 0; i < referenceData.length; i++) {
                int x = (int) (slope * i + intercept);
                if (referenceData[i] != 0) {
                    values[i] = new DataPoint(x, sampleData[i] / referenceData[i]);
                } else {
                    values[i] = new DataPoint(x, 0);
                }
            }
        }else{
            for(int i = 0; i < referenceData.length; i++){
                if (referenceData[i] != 0) {
                    values[i] = new DataPoint(i, sampleData[i] / referenceData[i]);
                } else {
                    values[i] = new DataPoint(i, 0);
                }
            }
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
        final GraphView graphView = findViewById(R.id.resultGraph);

        //setting up X and Y axis title
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setHorizontalAxisTitle("Pixel position");
        gridLabelRenderer.setVerticalAxisTitle("Transmission");
        graphView.addSeries(series);
    }

    private void getLastKnownPosition() {
        Log.e(TAG,this.latitude+" "+this.longitude);
        this.lastKnownPosition = findViewById(R.id.lastKnownPosition);
        String position = "Data measured at position ("+this.latitude+","+this.longitude+")";
        this.lastKnownPosition.setText(position);
        this.lastKnownPosition.setVisibility(View.VISIBLE);
    }

    private void stopLocationUpdates(){
        this.fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
    }

    private void startLocationUpdates(){
        this.fusedLocationProviderClient.requestLocationUpdates(this.locationRequest,this.locationCallback, Looper.getMainLooper());
    }

    private void buildGoogleApiClient() {
        this.googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
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
