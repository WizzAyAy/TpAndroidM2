package com.example.tp1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Class qui va afficher le graphique des temperatures
 */

public class CanvasView extends View {
    private final ArrayList<Float> temperatures = new ArrayList<>();
    private final Paint graphPaint;
    private final Paint BoxPaint;
    private final int TEXTE_SIZE = 25;
    private boolean isFull = false;

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        graphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        graphPaint.setColor(Color.BLACK);
        graphPaint.setTextSize(TEXTE_SIZE);

        BoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BoxPaint.setColor(Color.RED);
    }

    public void addTemperature(float parseFloat) {
        if (temperatures.size() > 45) temperatures.remove(0);
        temperatures.add(parseFloat);
    }

    public void setBoxContent(boolean b) {
        isFull = b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isFull) {
            graphPaint.setColor(Color.GREEN);
            canvas.drawText("IL Y A DU COURIER", 300, canvas.getHeight() - 50, graphPaint);
        } else {
            graphPaint.setColor(Color.RED);
            canvas.drawText("IL N Y A PAS DE COURIER", 300, canvas.getHeight() - 50, graphPaint);
        }

        int nbStep = 30;
        int stepDegresPixel = (canvas.getHeight() - 200) / nbStep;
        int maxStepTemp = 30;
        int stepTemp = maxStepTemp;

        // axe des degres
        for (int i = 1; i <= nbStep; i++) {

            canvas.drawText(Integer.toString(stepTemp), 25, (100 + stepDegresPixel * i) + TEXTE_SIZE / 2, graphPaint);
            if (i != nbStep)
                canvas.drawLine(75, (100 + stepDegresPixel * i), 125, (100 + stepDegresPixel * i), graphPaint);

            stepTemp--;
        }

        // les points des temps
        for (int i = 1; i <= temperatures.size(); i++) {
            if (i != 1) {
                canvas.drawLine(
                        20 * (i - 1) + 100,
                        (maxStepTemp - temperatures.get(i - 2) + 1) * stepDegresPixel + 100,
                        20 * i + 100,
                        (maxStepTemp - temperatures.get(i - 1) + 1) * stepDegresPixel + 100,
                        graphPaint);
            }
        }

        canvas.drawLine(100, 100, 100, canvas.getHeight() - 100, graphPaint);
        canvas.drawLine(100, canvas.getHeight() - 100, canvas.getWidth() - 100, canvas.getHeight() - 100, graphPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
