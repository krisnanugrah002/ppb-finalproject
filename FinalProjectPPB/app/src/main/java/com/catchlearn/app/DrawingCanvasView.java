package com.catchlearn.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawingCanvasView extends View {

    private Paint drawPaint;
    private Paint guidelinePaint;
    private Path drawPath;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    private ArrayList<Path> paths = new ArrayList<>();
    private ArrayList<Paint> paints = new ArrayList<>();

    public DrawingCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        // Paint untuk menggambar
        drawPaint = new Paint();
        drawPaint.setColor(Color.WHITE);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(12);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint untuk garis putus-putus
        guidelinePaint = new Paint();
        guidelinePaint.setColor(Color.parseColor("#30FFFFFF")); // Sangat transparan
        guidelinePaint.setAntiAlias(true);
        guidelinePaint.setStrokeWidth(2);
        guidelinePaint.setStyle(Paint.Style.STROKE);
        guidelinePaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));

        drawPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        drawGuidelines(canvas);

        canvas.drawBitmap(canvasBitmap, 0, 0, null);

        canvas.drawPath(drawPath, drawPaint);
    }

    private void drawGuidelines(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        int lineSpacing = 150;
        int startY = 100;

        for (int i = 0; i < 4; i++) {
            int y = startY + (i * lineSpacing);
            canvas.drawLine(50, y, width - 50, y, guidelinePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                paths.add(new Path(drawPath));
                paints.add(new Paint(drawPaint));
                drawPath.reset();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void clearCanvas() {
        drawPath.reset();
        paths.clear();
        paints.clear();
        if (drawCanvas != null) {
            drawCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        }
        invalidate();
    }

    public void undoLastStroke() {
        if (!paths.isEmpty()) {
            // Hapus path dan paint terakhir
            paths.remove(paths.size() - 1);
            paints.remove(paints.size() - 1);

            // Gambar ulang canvas
            if (drawCanvas != null) {
                drawCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

                for (int i = 0; i < paths.size(); i++) {
                    drawCanvas.drawPath(paths.get(i), paints.get(i));
                }
            }

            invalidate();
        }
    }

    public boolean canUndo() {
        return !paths.isEmpty();
    }

    public void setDrawColor(int color) {
        drawPaint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        drawPaint.setStrokeWidth(width);
    }
}