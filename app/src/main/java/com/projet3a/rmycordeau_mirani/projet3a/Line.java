package com.projet3a.rmycordeau_mirani.projet3a;

import java.util.ArrayList;
import java.util.List;

public class Line {

    private final String name;
    private int xBegin;
    private int yBegin;
    private int xEnd;
    private int yEnd;

    public Line(String name, int x1, int y1, int x2, int y2){
        this.name = name;
        this.xBegin = x1;
        this.yBegin = y1;
        this.xEnd = x2;
        this.yEnd = y2;
    }

    public String getName(){
        return this.name;
    }

    public int getXBegin(){
        return this.xBegin;
    }

    public int getXEnd(){
        return this.xEnd;
    }

    public int getYBegin(){
        return this.yBegin;
    }

    public int getYEnd(){
        return this.yEnd;
    }

    /**
     * Returns whether line is horizontal or vertical
     * */
    public String getLineType(){

        if(this.xBegin == this.xEnd){
            return "Vertical";
        }else if(this.yBegin == this.yEnd){
            return "Horizontal";
        }else{
            return null;
        }
    }

    /**
     * translates line on x axis
     * */
    public void translateLineOnX(int translateX){
        this.xBegin += translateX;
        this.xEnd += translateX;
    }

    /**
     * translates line on y axis
     * */
    public void translateLineOnY(int translateY){
        this.yBegin += translateY;
        this.yEnd += translateY;
    }

    /**
     * Returns intersection point between this line and an other
     * */
    public List<Integer> getIntersection(Line line){

        List<Integer> intersectionPoint = new ArrayList<>();

        if(this.getLineType().equals("Vertical") && line.getLineType().equals("Horizontal")){

            intersectionPoint.add(this.xBegin);
            intersectionPoint.add(line.getYBegin());

        }else if(this.getLineType().equals("Horizontal") && line.getLineType().equals("Vertical")){

            intersectionPoint.add(line.getXBegin());
            intersectionPoint.add(this.yBegin);
        }

        return intersectionPoint;
    }

}
