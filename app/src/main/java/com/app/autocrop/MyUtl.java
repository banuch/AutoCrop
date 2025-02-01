package com.app.autocrop;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;
import android.media.ExifInterface;

import java.io.IOException;
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

    static public String getImgFileName(String valType, String serviceId) {

        switch (valType) {
            case "KWH":
                return serviceId + "-kwh.jpg";
            case "KVAH":
                return serviceId + "-kvah.jpg";
            case "RMD":
                return serviceId + "-rmd.jpg";
            case "LT":
                return serviceId + "-lt.jpg";
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
//
//    static public Bitmap cropAndResizeImage(Bitmap originalBitmap) {
//        // Define the cropping parameters
//        int originalWidth = originalBitmap.getWidth();
//        int originalHeight = originalBitmap.getHeight();
//
//        // Crop the image from the top to 1/4 of its height
//        int left = 0; // Start from the left edge of the image
//        int top = 0;  // Start from the top of the image
//        int right = originalWidth; // Full width
//        int bottom = originalHeight / 4; // Crop up to 1/4 of the height
//
//        // Ensure the crop rectangle is valid
//        if (bottom <= 0) {
//            throw new IllegalArgumentException("Crop bottom is out of bounds.");
//        }
//
//        // Crop the image
//        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);
//
//        // Resize the cropped bitmap to 80% of its width (reducing by 20%)
//        int newWidth = (int) (croppedBitmap.getWidth() * 0.7);  // 80% of the original width
//        int newHeight = croppedBitmap.getHeight(); // Keep the same height
//
//        // Resize the cropped image
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, newWidth, newHeight, false);
//
//
//
//        return resizeTo640x640(resizedBitmap);
//    }

    static public Bitmap resizeTo640x640(Bitmap originalBitmap) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        // Create a new bitmap with 640x640 size and a white background
        Bitmap resizedBitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resizedBitmap);
        canvas.drawColor(Color.WHITE); // Set the background color to white

        // Calculate the scaling factors for width and height
        float scaleX = 640f / originalWidth;
        float scaleY = 640f / originalHeight;
        float scale = Math.min(scaleX, scaleY); // Choose the smaller scale to avoid stretching

        // Calculate the position to center the image on the canvas
        float offsetX = (640 - (originalWidth * scale)) / 2;
        float offsetY = (640 - (originalHeight * scale)) / 2;

        // Create a matrix for scaling and translation
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(offsetX, offsetY);

        // Draw the original image on the new canvas with the transformation applied
        canvas.drawBitmap(originalBitmap, matrix, null);

        return resizedBitmap;
    }


    static public Bitmap cropAndResizeImage(Bitmap originalBitmap, boolean mode) {
        // Define the rectangle area (left, top, right, bottom) you want to crop
        int left = 120;   // example x coordinate of the top-left corner
        int top = 50;    // example y coordinate of the top-left corner
        int right = originalBitmap.getWidth() - 50;  // example x coordinate of the bottom-right corner

        int bottom;

        if (mode) {
            bottom = originalBitmap.getHeight() / 4; // example y coordinate of the bottom-right corner
            return Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);
        } else {
            bottom = originalBitmap.getHeight() / 2; // example y coordinate of the bottom-right corner
            return Bitmap.createScaledBitmap(Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top), 640, 640, true);
        }

    }


    static public int getImageRotation(String imagePath) {
        int rotation = 0;

        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotation = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotation;
    }

    static public Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }


}
