package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

public class CameraHandler {

    private static final String TAG = "Camera Handler";
    private CameraDevice cameraDevice;
    private String cameraId;


    public CameraHandler(CameraDevice cameraDevice){
        this.cameraDevice = cameraDevice;
    }

    /**
     * opens the camera (if allowed), activates flash light and sets image dimension for capture
     */
    private void openCamera(Context context, Size imageDimension, ContextWrapper contextWrapper){
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try{
            int version = Build.VERSION.SDK_INT;
            if(version <= 22){
                Log.e(TAG,"Cannot activate flash light, API level too low");
                Toast flashError = Toast.makeText(context,"Cannot activate flash light, API level too low",Toast.LENGTH_LONG);
                flashError.show();

            }else{
                cameraManager.setTorchMode(this.cameraId,true);
            }
            this.cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(this.cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension =  map.getOutputSizes(SurfaceTexture.class)[0];
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * Closes camera device
     **/
    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
