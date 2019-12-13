package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;


public class WavelengthCalibrationView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder surfaceHolder = null;
    private Paint paint = null;
    private Paint clearPaint = null;
    private Line line;
    private Canvas canvas;
    private int width;
    private int height;

    public WavelengthCalibrationView(Context context){
        super(context);

        if(this.surfaceHolder == null){
            this.surfaceHolder = getHolder();
            this.surfaceHolder.addCallback(this);
        }

        if(this.paint == null){
            this.paint = new Paint();
            this.paint.setColor(Color.RED);
            this.paint.setStrokeWidth(5);
            this.paint.setStyle(Paint.Style.STROKE);
        }

        if(this.clearPaint == null){
            this.clearPaint = new Paint();
            this.clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        this.setZOrderOnTop(true);
        this.surfaceHolder.setFormat(PixelFormat.TRANSPARENT);

    }

    private void initLine() {
        this.height = getHeight();
        this.width = getWidth();
        Log.e("test",""+this.width);
        this.line = new Line("Calibration line",this.width/2,0,this.width/2,this.height);
        ((WavelengthCalibrationActivity)getContext()).displayLine();
    }

    public void drawLine(){
        if(this.line == null){
            initLine();
        }
        this.canvas = this.surfaceHolder.lockCanvas();
        canvas.drawLine(line.getXBegin(),line.getYBegin(),line.getXEnd(),line.getYEnd(),paint);
        this.surfaceHolder.unlockCanvasAndPost(this.canvas);
    }

    public void eraseLine(){
        this.canvas = this.surfaceHolder.lockCanvas();
        this.canvas.drawRect(0,0,width,height,clearPaint);
        this.surfaceHolder.unlockCanvasAndPost(this.canvas);
    }

    public void moveLineRigthwards(int shift){
        this.line.translateLineOnX(shift);
        eraseLine();
        drawLine();
    }

    public void moveLineLeftwards(int shift){
        this.line.translateLineOnX(-shift);
        eraseLine();
        drawLine();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initLine();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
