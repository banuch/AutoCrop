package com.app.autocrop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

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


public class RectangleOverlay extends View {
    private Paint backgroundPaint;
    private Paint rectanglePaint;

    public RectangleOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        // Paint for the semi-transparent background
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setAlpha(255); // Adjust transparency (0-255)
        backgroundPaint.setStyle(Paint.Style.FILL);

        // Paint for the rectangle outline
        rectanglePaint = new Paint();
        rectanglePaint.setColor(Color.RED);
        rectanglePaint.setStyle(Paint.Style.STROKE);
        rectanglePaint.setStrokeWidth(10);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Get the width and height of the view
        int width = getWidth();
        int height = getHeight();

        // Dimensions of the rectangle
        float left = 50;
        float top = 50;
        float right = width - 50;
        float bottom = height / 5f;

        // Draw the semi-transparent background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Use a different layer to cut out the rectangle
        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        canvas.restore();

        // Draw the red rectangle outline
        canvas.drawRect(left, top, right, bottom, rectanglePaint);
    }
}