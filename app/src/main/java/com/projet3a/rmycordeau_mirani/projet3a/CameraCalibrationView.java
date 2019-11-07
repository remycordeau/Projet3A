package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class CameraCalibrationView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder surfaceHolder = null;
    private Paint paint = null;
    private Paint clearPaint = null;
    private List<Line> lines = new ArrayList<>();
    private Canvas canvas;
    private int width;
    private int height;

    public CameraCalibrationView(Context context){
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

    private void initLines() {
        this.width = getWidth();
        this.height = getHeight();

        lines.add(new Line("Left line",0,0,0,height));
        lines.add(new Line("Top line",0,0,width,0));
        lines.add(new Line("Bottom line",0,height,width,height));
        lines.add(new Line("Right line",width,0,width,height));
    }

    public void drawLines(){
        if(lines.size() == 0){
            initLines();
        }
        this.canvas = this.surfaceHolder.lockCanvas();
        for(Line line : lines){
            canvas.drawLine(line.getXBegin(),line.getYBegin(),line.getXEnd(),line.getYEnd(),paint);
        }
        surfaceHolder.unlockCanvasAndPost(this.canvas);
    }

    public void eraseLines(){
        this.canvas = this.surfaceHolder.lockCanvas();
        this.canvas.drawRect(0,0,width,height,clearPaint);
        this.surfaceHolder.unlockCanvasAndPost(this.canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
