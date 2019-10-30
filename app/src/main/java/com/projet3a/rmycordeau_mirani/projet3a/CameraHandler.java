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
    private String cameraId;


    /**
     * activates flash light and returns image dimension for capture
     */
    public Size openCamera(Context context,CameraManager cameraManager){
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
            Size imageDimension =  map.getOutputSizes(SurfaceTexture.class)[0];
            return imageDimension;
        }catch(CameraAccessException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Closes camera device
     **/
    public void closeCamera(CameraDevice cameraDevice) {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
