package com.projet3a.rmycordeau_mirani.projet3a

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startCameraActivity(view : View){
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    fun quitApp(view: View){
        finish()
    }

}
