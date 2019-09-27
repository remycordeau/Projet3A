package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private final String TAG = "Main";
    private Button startCaptureButton;
    private Button quitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        addListeners(savedInstanceState);
    }

    private void addListeners(final Bundle savedInstanceState) {

        //start capture button
        this.startCaptureButton = findViewById(R.id.start_capture);
        assert this.startCaptureButton != null;
        this.startCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        //quit button
        this.quitButton = findViewById(R.id.quit_app);
        assert this.quitButton != null;
        this.quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
