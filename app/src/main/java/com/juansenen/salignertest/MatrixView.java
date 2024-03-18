package com.juansenen.salignertest;

import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

public class MatrixView extends View {
    private int[][] matrix; // La matriz de datos
    private int cellSize = 15; // Tamaño de cada celda en píxeles

    public MatrixView(Context context) {
        super(context);
    }

    public MatrixView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MatrixView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // Método para establecer la matriz de datos
    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
        invalidate(); // Redibujar la vista
    }

    // Método para obtener un color en función de un valor en la escala de 1 a 100
//    private int getColorForValue(int value) {
//        if (value <= 10) {
//            // Escala los valores bajos a colores hueso o grisáceo
//            float brightness = 0.5f + (value / 20.0f); // Ajusta este factor según tus preferencias
//            return Color.HSVToColor(new float[]{0f, 0f, brightness});
//        } else {
//            // Escala los valores restantes a colores más intensos como el rojo
//            float hue = (value - 10) * 1.2f; // Ajusta este factor según tus preferencias
//            return Color.HSVToColor(new float[]{hue, 1f, 1f});
//        }
//    }
    // Método para obtener un color en función de un valor en la escala de 0 a 255
    private int getColorForValue(int value) {
        if (value <= 127) {
            // Escala los valores bajos a colores hueso o grisáceo
            float brightness = 0.5f + (value / 510.0f); // Escala el valor entre 0.5 y 1
            return Color.HSVToColor(new float[]{0f, 0f, brightness});
        } else {
            // Escala los valores restantes a colores más intensos como el rojo
            float hue = (value - 127) * 1.2f; // Ajusta este factor según tus preferencias
            return Color.HSVToColor(new float[]{hue, 1f, 1f});
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (matrix == null) return;

        int rows = matrix.length;
        int cols = matrix[0].length;

        // Dibujar puntos en la matriz
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int value = matrix[i][j];
                int color = getColorForValue(value);

                Paint paint = new Paint();
                paint.setColor(color);

                int x = j * cellSize;
                int y = i * cellSize;
                canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
            }
        }
    }
}