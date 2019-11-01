package com.projet3a.rmycordeau_mirani.projet3a;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraCalibration {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private int width;
    private int height;

    public CameraCalibration(SurfaceView surfaceView){
        this.surfaceView = surfaceView;
        this.surfaceHolder = surfaceView.getHolder();
    }

    public void drawCalibrationLines(){
        this.surfaceView.setZOrderOnTop(true);
        this.surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        this.surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Canvas canvas = surfaceHolder.lockCanvas();
                if(canvas == null){
                    Log.e("Camera Calibration","Cannot draw onto the canvas as it's null");
                }else{
                    width = surfaceView.getWidth();
                    height = surfaceView.getHeight();
                    Log.e("TEST",""+width+" "+height);
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(5);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(0,0,0,height,paint);
                    canvas.drawLine(0,0,width,0,paint);
                    canvas.drawLine(0,height,width,height,paint);
                    canvas.drawLine(width,0,width,height,paint);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
    }


}
