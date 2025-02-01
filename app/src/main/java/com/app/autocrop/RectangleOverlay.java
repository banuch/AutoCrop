package com.app.autocrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Point;

public class RectangleOverlay extends View {

    private Paint paint;
    private int rectWidth;
    private int rectHeight;
    private int topPadding; // Padding from the top

    public RectangleOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Initialize paint
        paint = new Paint();
        paint.setColor(0xFFFF0000); // Red color for the rectangle
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE); // Only draw the border, not filled

        // Get screen dimensions
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getSize(screenSize);
        }

        // Calculate rectangle dimensions
        rectWidth = (int) (screenSize.x * 0.8); // Reduce width by 80%
        rectHeight = (int) (screenSize.y * 0.20);       // Height is 1/4 of screen height

        // Set top padding (e.g., 50 pixels)
        topPadding = 50; // Adjust as needed
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Define rectangle boundaries with top padding
        int left = (getWidth() - rectWidth) / 2; // Center horizontally
        int top = topPadding;                   // Start at the top with padding
        int right = left + rectWidth;
        int bottom = top + rectHeight;

        // Draw the rectangle
        canvas.drawRect(left, top, right, bottom, paint);
    }
}


//
//
//public class RectangleOverlay extends View {
//
//    private Paint paint;
//
//    public RectangleOverlay(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }
//
//    private void init() {
//        paint = new Paint();
//        paint.setColor(0xFFFF0000); // Red color for the rectangle
//        paint.setStrokeWidth(5);
//        paint.setStyle(Paint.Style.STROKE); // Only draw the border, not filled
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        // Draw a simple rectangle on the canvas
//        canvas.drawRect(100, 100, 500, 500, paint);
//    }
//}
//
//
//public class RectangleOverlay extends View {
//    private Paint backgroundPaint;
//    private Paint rectanglePaint;
//
//    public RectangleOverlay(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }
//
//
//    private void init() {
//        // Paint for the semi-transparent background
//        backgroundPaint = new Paint();
//        backgroundPaint.setColor(Color.WHITE);
//        backgroundPaint.setAlpha(255); // Adjust transparency (0-255)
//        backgroundPaint.setStyle(Paint.Style.FILL);
//
//        // Paint for the rectangle outline
//        rectanglePaint = new Paint();
//        rectanglePaint.setColor(Color.RED);
//        rectanglePaint.setStyle(Paint.Style.STROKE);
//        rectanglePaint.setStrokeWidth(10);
//    }
//
//    @Override
//    protected void onDraw(@NonNull Canvas canvas) {
//        super.onDraw(canvas);
//
//        // Get the width and height of the view
//        int width = getWidth();
//        int height = getHeight();
//
//        // Dimensions of the rectangle
//        float left = 50;
//        float top = 50;
//        float right = width - 50;
//        float bottom = height / 5f;
//
//        // Draw the semi-transparent background
//        canvas.drawRect(0, 0, width, height, backgroundPaint);
//
//        // Use a different layer to cut out the rectangle
//        canvas.save();
//        canvas.clipRect(left, top, right, bottom);
//        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
//        canvas.restore();
//
//        // Draw the red rectangle outline
//        canvas.drawRect(left, top, right, bottom, rectanglePaint);
//    }
//
//}