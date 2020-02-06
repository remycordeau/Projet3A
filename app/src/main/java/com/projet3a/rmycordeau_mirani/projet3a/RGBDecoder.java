package com.projet3a.rmycordeau_mirani.projet3a;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */

public class RGBDecoder {

    public static int[] getRGBCode(Bitmap bitmap, int width, int height) {
        int[] rgb = new int[width * height];
        bitmap.getPixels(rgb, 0, width, 0, 0, width, height);
        int R, G, B;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width) + x;
                R = (rgb[index] >> 16) & 0xff;
                G = (rgb[index] >> 8) & 0xff;
                B = rgb[index] & 0xff;
                rgb[index] = 0xff000000 | (R << 16) | (G << 8) | B; //color encoding
            }
        }
        return rgb;
    }

    public static double[] getImageIntensity(int[] rgb){
        double [] intensity = new double[rgb.length];
        double sRed,sGreen,sBlue;
        for(int i = 0; i < intensity.length; i++){
            //sRed = RGBDecoder.getLinearValue(Color.red(rgb[i]));
            //sBlue = RGBDecoder.getLinearValue(Color.blue(rgb[i]));
            //sGreen = RGBDecoder.getLinearValue(Color.green(rgb[i]));
            intensity[i] = (0.2126* Color.red(rgb[i])+0.7152*Color.green(rgb[i])+0.0722*Color.blue(rgb[i]));
            //intensity[i] = (0.2126*sRed+0.7152*sGreen+0.0722*sBlue);
        }
        return intensity;
    }

    public static double getLinearValue(int channel){
        if(channel <= 0.04045){
            return channel/12.92;
        }else{
            double a = 0.055;
            double d = (channel + a)/(1 + a);
            return Math.pow(d,2.4);
        }
    }

    /**
    * Calculates the intensity mean on each column for the captured frame
    * */
    public static double[] computeIntensityMean(double [] intensity, int width, int height) {
        double [] intensityMean = new double[width];
        double meanValue = 0;
        int index;
        for(int i = 0; i < intensityMean.length; i++){
            index = i;
            while(index < intensity.length){ //if the index is defined, we add it to the mean
                meanValue += intensity[index];
                index += width; //go to next line value for the considered column
            }//if it is not, it means that we have to change column in our captured picture
            intensityMean[i] = meanValue/height;
            meanValue = 0;
        }
        return intensityMean;
    }

    public static double[] getMaxIntensity(double [] intensity, int width){
        double [] maxIntensity = new double[width];
        double maxValue = 0;
        int index;
        for(int i = 0; i < maxIntensity.length; i++){
            index = i;
            while(index < intensity.length){ //if the index is defined, we add it to the mean
                if(intensity[index] > maxValue){
                    maxValue = intensity[index];
                }
                index += width; //go to next line value for the considered column
            }//if it is not, it means that we have to change column in our captured picture
            maxIntensity[i] = maxValue;
            maxValue= 0;
        }
        return maxIntensity;
    }
}