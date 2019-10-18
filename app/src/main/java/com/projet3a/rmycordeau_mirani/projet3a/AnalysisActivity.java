package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.HashMap;

import static com.projet3a.rmycordeau_mirani.projet3a.CameraActivity.GRAPH_DATA_KEY;

public class AnalysisActivity extends Activity {

    private static final String TAG = "Analysis Activity";
    private Bundle cameraActivityData;
    private HashMap<String,double[]> graphData;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analysis_activity_layout);
        this.cameraActivityData = getIntent().getExtras();
        this.graphData = (HashMap<String,double[]>) this.cameraActivityData.get(GRAPH_DATA_KEY);
        if(this.graphData.containsKey("Reference") && this.graphData.containsKey("Sample")){
            drawGraph();
        }else{
            Toast.makeText(this,"Missing data to draw graph",Toast.LENGTH_SHORT).show();
        }
    }

    private void drawGraph(){

        //getting sample and reference data
        double[] referenceData = this.graphData.get("Reference");
        double[] sampleData = this.graphData.get("Sample");

        //creating graph series
        DataPoint[] values = new DataPoint[referenceData.length];
        for(int i = 0; i < referenceData.length; i++){
            values[i] = new DataPoint(i,sampleData[i]/referenceData[i]);
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
        final GraphView graphView = findViewById(R.id.resultGraph);

        //setting up X and Y axis title
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        //gridLabelRenderer.setHorizontalAxisTitle("Pixel position");
        //gridLabelRenderer.setVerticalAxisTitle("Pixel");
        graphView.addSeries(series);
    }
}
