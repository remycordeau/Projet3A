package com.projet3a.rmycordeau_mirani.projet3a;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */

public class RGBDecoder {

    public static int[] getRGBCode(byte[] bytes, int width, int height) {

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        int[] rgb = new int[width * height];
        bitmap.getPixels(rgb, 0, width, 0, 0, width, height);
        int R, G, B;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width) + x;
                R = (rgb[index] >> 16) & 0xff;
                G = (rgb[index] >> 8) & 0xff;
                B = rgb[index] & 0xff;
                rgb[index] = 0xff000000 | (R << 16) | (G << 8) | B;
            }
        }
        return rgb;
    }

    public static double[] getImageIntensity(int[] rgb){
        double [] intensity = new double[rgb.length];
        for(int i = 0; i < intensity.length; i++){
            intensity[i] = (0.21* Color.red(rgb[i])+0.72*Color.green(rgb[i])+0.07*Color.blue(rgb[i]));
        }
        return intensity;
    }
}


