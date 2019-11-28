package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class CameraHandler {

    private static final String TAG = "Camera Handler";
    private String cameraId;


    /**
     * activates flash light of camera if possible
     */
    public void activateFlashLight(Context context, CameraManager cameraManager){
        try{
            this.cameraId = cameraManager.getCameraIdList()[0];
            int version = Build.VERSION.SDK_INT;
            if(version <= 22){
                Log.e(TAG,"Cannot activate flash light, API level too low");
                Toast flashError = Toast.makeText(context,"Cannot activate flash light, API level too low",Toast.LENGTH_LONG);
                flashError.show();

            }else{
                cameraManager.setTorchMode(this.cameraId,true);
            }
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }
}
