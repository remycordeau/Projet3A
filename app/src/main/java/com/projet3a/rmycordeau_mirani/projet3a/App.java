package com.projet3a.rmycordeau_mirani.projet3a;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class App extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String TAG = "APP";
        int version = Build.VERSION.SDK_INT;
        Log.e(TAG, "API version : "+version);
        MainActivity activity = new MainActivity();
        activity.onCreate(savedInstanceState);
    }
}
