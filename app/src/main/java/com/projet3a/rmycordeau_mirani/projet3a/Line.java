package com.projet3a.rmycordeau_mirani.projet3a;

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

    public void translateLineOnX(int translateX){
        this.xBegin += translateX;
        this.xEnd += translateX;
    }

    public void translateLineOnY(int translateY){
        this.yBegin += translateY;
        this.yEnd += translateY;
    }
}
