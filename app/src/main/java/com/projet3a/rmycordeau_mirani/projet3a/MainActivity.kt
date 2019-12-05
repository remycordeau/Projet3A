package com.projet3a.rmycordeau_mirani.projet3a

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity() {

    private var contextWrapper: ContextWrapper? = null
    private var permissionsGranted: Boolean = false
    private val REQUEST_PERMISSIONS = 200


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var startCaptureButton: Button = findViewById(R.id.startCaptureButton)
        startCaptureButton.setOnClickListener{
            view -> startCameraActivity(view)
        }
        if (Build.VERSION.SDK_INT <= 22) {
            permissionsGranted = true
        } else {
            askPermissions()
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

    @RequiresApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        val version = Build.VERSION.SDK_INT
        if (version >= 23) {
            if (this.contextWrapper?.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS)
            } else {
                this.permissionsGranted = true
            }
        }
    }

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
}
