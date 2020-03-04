package com.projet3a.rmycordeau_mirani.projet3a

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context.LOCATION_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.location.LocationManager
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class MainActivity : Activity() {

    private var contextWrapper: ContextWrapper? = null
    private var permissionsGranted: Boolean = false
    private val REQUEST_PERMISSIONS = 200


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableGPS()
        var startCaptureButton: Button = findViewById(R.id.startCaptureButton)
        startCaptureButton.setOnClickListener{
            view -> startCameraActivity(view)
        }

        var wavelengthCalibrationButton: Button = findViewById(R.id.wavelengthCalibrationButton)
        wavelengthCalibrationButton.setOnClickListener{
            view ->  startCalibrationActivity(view)
        }
        if (Build.VERSION.SDK_INT <= 22) {
            permissionsGranted = true
        } else {
            askPermissions()
        }
    }

    private fun startCalibrationActivity(view: View){
        if (permissionsGranted) {
            val intent = Intent(this, WavelengthCalibrationActivity::class.java)
            startActivity(intent)
        } else if (Build.VERSION.SDK_INT >= 23) {
            askPermissions()
            return
        }
    }


    private fun startCameraActivity(view: View) {
        if (permissionsGranted) {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        } else if (Build.VERSION.SDK_INT >= 23) {
            askPermissions()
            return
        }
    }

    fun quitApp(view: View) {
        finish()
    }

    /**
     * asks app permissions to user
     * */
    @RequiresApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        val version = Build.VERSION.SDK_INT
        if (version >= 23) {
            if (this.contextWrapper?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSIONS)
            } else {
                this.permissionsGranted = true
            }
        }
    }

    /**
     * Checks whether user has granted permissions for the app
     * */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    this.permissionsGranted = true
                } else {
                    Toast.makeText(this@MainActivity, "You can't use this app without granting permissions", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    /**
     * Displays GPS activation window on phone if it is not turned on
     * */
    private fun enableGPS() {
        val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            return
        }else{
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }
}
