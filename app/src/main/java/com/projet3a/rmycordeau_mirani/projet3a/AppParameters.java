package com.projet3a.rmycordeau_mirani.projet3a;

/**
 * Singleton that contains all app parameters (slope, intercept, capture zone...)
 */
public final class AppParameters {

    private static volatile AppParameters instance = null;
    private int[] captureZone;
    private double slope;
    private double intercept;

    private AppParameters(){
        super();
    }

    public static final AppParameters getInstance(){
        if(AppParameters.instance == null){
           //synchronized keyword prevents any multiple instantiations by several threads
            synchronized (AppParameters.class){
                if(AppParameters.instance == null){
                    AppParameters.instance = new AppParameters();
                }
            }
        }
        return AppParameters.instance;
    }

    /* Getters and setters */

    public void setCaptureZone(int[] array){
        this.captureZone = array;
    }

    public void setSlope(double s){
        this.slope = s;
    }

    public void setIntercept(double i){
        this.intercept = i;
    }

    public int[] getCaptureZone(){
        return this.captureZone;
    }

    public double getSlope(){
        return this.slope;
    }

    public double getIntercept(){
        return this.intercept;
    }
}
