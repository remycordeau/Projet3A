package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.os.Bundle;

public class App extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraActivity activity = new CameraActivity();
        activity.onCreate(savedInstanceState);
    }
}
