package com.juansenen.salignertest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class MatrixView extends View
{
    private ArrayList<int[][]> matrices;
    private int cellSize = 15;
    private int verticalSpacing = 5;
    private ArrayList<ArrayList<Point>> cellCoordinates;
    private static final String TAG = "AlignerFragment";
    public MatrixView(Context context)
    {
        super(context);
        matrices = new ArrayList<>();
    }

    public MatrixView(Context context, AttributeSet attrs) {
        super(context, attrs);
        matrices = new ArrayList<>();
    }

    // Método para agregar una matriz a la lista
    public void addMatrix(int[][] matrix)
    {
        matrices.add(matrix);
        calculateCellCoordinates(); // Vuelve a calcular las coordenadas de las celdas
        invalidate(); // Redibujar la vista
    }
    public MatrixView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }
    // Método para calcular las coordenadas de las celdas
    private void calculateCellCoordinates()
    {
        if (matrices == null) return;

        cellCoordinates = new ArrayList<>();
        int currentX = 0;
        int currentY = 0;

        // Calcular las coordenadas de cada celda
        for (int[][] matrix : matrices)
        {
            ArrayList<Point> matrixCoordinates = new ArrayList<>();
            int rows = matrix.length;
            int cols = matrix[0].length;

            for (int i = 0; i < rows; i++)
            {
                for (int j = 0; j < cols; j++)
                {
                    int x = currentX + j * cellSize;
                    int y = currentY + i * cellSize;
                    matrixCoordinates.add(new Point(x, y));
                }
            }
            cellCoordinates.add(matrixCoordinates);

            // Mover a la siguiente posición en la misma fila o a la siguiente fila
            currentX += cols * cellSize;
            if (currentX >= getWidth() - cols * cellSize)
            { // Si no hay suficiente espacio para otra matriz en la misma fila
                currentX = 0; // Mover a la primera posición en la nueva fila
                currentY += rows * cellSize + verticalSpacing; // Mover a la siguiente fila con espacio vertical
            }
        }
    }

    public void drawMatrices(ArrayList<int[][]> matrices)
    {
        this.matrices = matrices;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int currentX = 0;
        int currentY = 0;
        int matricesDrawnInRow = 0;

        // Dibuja las matrices
        for (int[][] matrix : matrices)
        {
            for (int i = 0; i < matrix.length; i++)
            {
                for (int j = 0; j < matrix[0].length; j++)
                {
                    int color = getColorForValue(matrix[i][j]);
                    Paint paint = new Paint();
                    paint.setColor(color);
                    canvas.drawRect(currentX + j * cellSize, currentY + i * cellSize,
                            currentX + j * cellSize + cellSize, currentY + i * cellSize + cellSize, paint);
                   //Log.i(TAG,"Punto Fila " + i + "Columna " + j + "Valor color " + color);
                }
            }

            // Actualiza las coordenadas para la siguiente matriz en la misma fila
            currentX += matrix[0].length * cellSize;
        }
    }
// GELCOLOR ORIGINAL
//    private int getColorForValue(int value)
//    {
//        if (value <= 255)
//        {
//            // Escala los valores bajos a colores oscuros casi negros
//            float brightness = value / 255.0f; // Se asume que los valores van de 0 a 255
//            return Color.HSVToColor(new float[]{0f, 0f, brightness});
//        }
//        else
//        {
//            // Escala los valores restantes a tonos de azul más intenso
//            float hue = 240f - (value - 255);
//            return Color.HSVToColor(new float[]{hue, 1f, 1f});
//        }
//    }
//TODO NUEVA INTERPRETACION DE VALORES
private int getColorForValue(int value) {
    // Define el rango máximo de valores esperados (ajustarlo según sea necesario)
    int maxRange = 4096;

    // Validación del rango de valores
    if (value < 0 || value > maxRange) {
        Log.w("MatrixView", "Valor fuera de rango: " + value);
    }

    // Si el valor está entre 0 y 700, asigna negro
    if (value <= 800) {
        Log.i("MatrixView", "Valor dentro del rango negro: " + value);
        return Color.HSVToColor(new float[]{0f, 0f, 0f}); // Negro
    }

    // Discretiza el valor en pasos espaciados (por ejemplo, 10 niveles)
    int numLevels = 10; // Niveles
    int step = maxRange / numLevels; // Tamaño de cada paso

    // Encuentra el nivel más cercano para el valor
    int discretizedValue = (value / step) * step;

    // Normaliza a un rango de brillo entre 0 y 1
    //float brightness = (float) discretizedValue / maxRange;
    // Saturación fija para garantizar tonos naranjas visibles
    float saturation = 0.8f; // Puedes ajustar entre 0.7 y 1 para variar intensidad
    // Normaliza a un rango de brillo entre 0.3 y 1
    float brightness = 0.3f + (float) discretizedValue / maxRange * 0.7f;
    // Matiz fijo para naranja (30° en el espectro HSV)
    float hue = 30f;

    // Log de los valores originales y su discretización
    Log.i("MatrixView", "Valor original: " + value + ", Discretizado: " + discretizedValue + ", Brillo: " + brightness);

    // Genera un color en escala de naranja
    return Color.HSVToColor(new float[]{hue, saturation, brightness});
}


    public void clearMatrices()
    {
        matrices.clear();
        invalidate(); // Volver a dibujar la vista para borrar las matrices
    }
}