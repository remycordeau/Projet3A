package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.MotionEvent;
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
    private float startDragX, currenDragX = 0;

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

    /**
     * inits the Line object that will be drawn
     * */
    private void initLine() {
        this.height = getHeight();
        this.width = getWidth();
        int heightOfLine= (int)Math.floor(0.78*this.height);
        this.line = new Line("Calibration line",this.width/2,0,this.width/2,heightOfLine);
        ((WavelengthCalibrationActivity)getContext()).displayLine();
    }

    /**
     * draws the line in the view, and updates the position of the line in the wavelength calibration activity
     * */
    public void drawLine(){
        if(this.line == null){
            initLine();
        }
        this.canvas = this.surfaceHolder.lockCanvas();
        canvas.drawLine(line.getXBegin(),line.getYBegin(),line.getXEnd(),line.getYEnd(),paint);
        this.surfaceHolder.unlockCanvasAndPost(this.canvas);
        ((WavelengthCalibrationActivity)getContext()).updateWaveLengthPositions();
    }

    /**
     * erases the line in the view
     * */
    public void eraseLine(){
        this.canvas = this.surfaceHolder.lockCanvas();
        this.canvas.drawRect(0,0,width,height,clearPaint);
        this.surfaceHolder.unlockCanvasAndPost(this.canvas);
    }

    /**
     * translates the line in the view
     * @param shift : the shift to translate the line
     * */
    public void translateLine(int shift){
        this.line.translateLineOnX(shift);
        eraseLine();
        drawLine();
    }

    /**
     * returns the x coordinate of a point of the line
     * */
    public int getXPositionOfDrawnLine(){
        return this.line.getXBegin();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder){
        initLine();
    }

    @Override
    /**
     * Moves line on touch or on drag
     * */
    public boolean onTouchEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN) { // ACTION_DOWN -> finger is detected on screen
            startDragX = event.getX();
            int shift = (int)(startDragX - this.line.getXBegin());
            this.translateLine(shift);

        }else if (event.getAction() == MotionEvent.ACTION_UP){ // ACTION_UP -> finger is removed from screen
            if(Math.abs(startDragX-this.line.getXBegin()) < 30){ // if the finger was put 30 pixels around the drawn line, the line is translated to the right/left accordingly
                currenDragX = event.getX();
                int shift = (int)(currenDragX - startDragX);
                this.translateLine(shift);
            }
        }
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
