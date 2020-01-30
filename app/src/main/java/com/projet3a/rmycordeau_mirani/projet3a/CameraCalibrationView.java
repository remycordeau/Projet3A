package com.projet3a.rmycordeau_mirani.projet3a;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
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
            this.paint.setStrokeWidth(1);
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

        /*Call to main activity to enable listeners on seekbars*/
        ((CameraActivity) getContext()).enableSeekBarsListeners();

        lines.add(new Line("Left line",0,0,0,height));
        lines.add(new Line("Right line",width,0,width,height));
        lines.add(new Line("Top line",0,0,width,0));
        lines.add(new Line("Bottom line",0,height,width,height));
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

    public void moveLine(String lineName, int shift){

        Line target = null;
        for(Line line : this.lines){
            if(line.getName().equals(lineName)){
                target = line;
                break;
            }
        }

        if(target == null) {
            Log.e("Translate line error", "line not found");
            return;
        }

        switch (target.getName()){
            case "Bottom line" :
                target.translateLineOnY(-shift); //bottom line can only go upwards
                this.lines.set(3,target);
                break;
            case "Top line" :
                target.translateLineOnY(shift); // top line can only go downwards
                this.lines.set(2,target);
                break;
            case "Right line" :
                target.translateLineOnX(-shift); // right line can only go left
                this.lines.set(1,target);
                break;
            case  "Left line" :
                target.translateLineOnX(shift); // left line can only go right
                this.lines.set(0,target);
                break;
        }

        eraseLines();
        drawLines();
    }

    /**
     * sets the capture zone on the following format : [x,y,width,height], called when the capture zone button is pressed
     * (x,y) is the intersection point between left line and top line
     * width is the distance between x and the x coordinate of the intersection point between right line and top line
     * height is the distance between y and the y coordinate of the intersection point between bottom line and left line
     * */
    public void setCaptureZone(){

        int[] captureZone = new int[4];

        List<Integer> topLeftIntersection = lines.get(0).getIntersection(lines.get(2));
        captureZone[0] = topLeftIntersection.get(0);
        captureZone[1] = topLeftIntersection.get(1);

        List<Integer> rightTopIntersection = lines.get(2).getIntersection(lines.get(1));
        captureZone[2] = Math.abs(rightTopIntersection.get(0) - topLeftIntersection.get(0));

        List<Integer> bottomLeftIntersection = lines.get(0).getIntersection(lines.get(3));
        captureZone[3] = Math.abs(bottomLeftIntersection.get(1) - topLeftIntersection.get(1));

        AppParameters.getInstance().setCaptureZone(captureZone);
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
