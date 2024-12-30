package com.app.autocrop;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyUtl {

    private static final String TEMP_DIR_NAME = "spdcl";

    private static final String TAG = "Offline OCR";


    static boolean isWithinDateRange(String startDate, String endDate) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);
            Date today = new Date();

            // Check if today's date is within the range
            return today.equals(start) || today.equals(end) || (today.after(start) && today.before(end));
        } catch (ParseException e) {
            e.printStackTrace();
            return false; // Return false if date parsing fails
        }
    }

    static public String getImgFileName(String valType,String serviceId) {

        switch (valType) {
            case "KWH":
                return serviceId + "-kwh.jpg";
            case "KVAH":
                return serviceId + "-kvah.jpg";
            case "RMD":
                return serviceId + "-rmb.jpg";
            default:
                return "default.jpg";
        }

    }

    static String createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) {


        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File tempDir = new File(root, TEMP_DIR_NAME);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                Log.e(TAG, "Failed to create temp directory: " + tempDir.getAbsolutePath());
                return null;
            }
        }


        File file = new File(tempDir, fileName);


        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.toString();
    }

    static public Bitmap cropAndResizeImage(Bitmap originalBitmap) {
        // Define the rectangle area (left, top, right, bottom) you want to crop
        int left = 50;   // example x coordinate of the top-left corner
        int top = 50;    // example y coordinate of the top-left corner
        int right = originalBitmap.getWidth() - 50;  // example x coordinate of the bottom-right corner
        int bottom = originalBitmap.getHeight() / 2; // example y coordinate of the bottom-right corner

        return Bitmap.createScaledBitmap(Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top), 640, 640, true);


    }




}
