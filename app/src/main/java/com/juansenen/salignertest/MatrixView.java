package com.juansenen.salignertest;

import android.graphics.Point;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import java.util.ArrayList;

public class MatrixView extends View {
    private ArrayList<int[][]> matrices;
    private int cellSize = 10;
    private int verticalSpacing = 5;
    private ArrayList<ArrayList<Point>> cellCoordinates;
    public MatrixView(Context context) {
        super(context);
        matrices = new ArrayList<>();
    }

    public MatrixView(Context context, AttributeSet attrs) {
        super(context, attrs);
        matrices = new ArrayList<>();
    }

    // Método para agregar una matriz a la lista
    public void addMatrix(int[][] matrix) {
        matrices.add(matrix);
        calculateCellCoordinates(); // Vuelve a calcular las coordenadas de las celdas
        invalidate(); // Redibujar la vista
    }
    public MatrixView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    // Método para calcular las coordenadas de las celdas
    private void calculateCellCoordinates() {
        if (matrices == null) return;

        cellCoordinates = new ArrayList<>();
        int currentX = 0;
        int currentY = 0;

        // Calcular las coordenadas de cada celda
        for (int[][] matrix : matrices) {
            ArrayList<Point> matrixCoordinates = new ArrayList<>();
            int rows = matrix.length;
            int cols = matrix[0].length;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int x = currentX + j * cellSize;
                    int y = currentY + i * cellSize;
                    matrixCoordinates.add(new Point(x, y));
                }
            }
            cellCoordinates.add(matrixCoordinates);

            // Mover a la siguiente posición en la misma fila o a la siguiente fila
            currentX += cols * cellSize;
            if (currentX >= getWidth() - cols * cellSize) { // Si no hay suficiente espacio para otra matriz en la misma fila
                currentX = 0; // Mover a la primera posición en la nueva fila
                currentY += rows * cellSize + verticalSpacing; // Mover a la siguiente fila con espacio vertical
            }
        }
    }



    public void drawMatrices(ArrayList<int[][]> matrices) {
        this.matrices = matrices;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int currentX = 0;
        int currentY = 0;
        int matricesDrawnInRow = 0;

        // Dibuja las matrices
        for (int[][] matrix : matrices) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    int color = getColorForValue(matrix[i][j]);
                    Paint paint = new Paint();
                    paint.setColor(color);
                    canvas.drawRect(currentX + j * cellSize, currentY + i * cellSize,
                            currentX + j * cellSize + cellSize, currentY + i * cellSize + cellSize, paint);
                }
            }

            // Actualiza las coordenadas para la siguiente matriz en la misma fila
            currentX += matrix[0].length * cellSize;

            // Verifica si se han dibujado tres matrices en la fila actual
            matricesDrawnInRow++;
            if (matricesDrawnInRow == 3) {
                currentX = 0;
                currentY += matrix.length * cellSize + verticalSpacing;
                matricesDrawnInRow = 0;
            }
        }
    }

    private int getColorForValue(int value) {
        if (value <= 50) {
            // Escala los valores bajos a colores hueso o grisáceo
            float brightness = 0.5f + (value / 20.0f); // Ajusta este factor según tus preferencias
            return Color.HSVToColor(new float[]{0f, 0f, brightness});
        } else {
            // Escala los valores restantes a colores más intensos como el rojo
            float hue = (value - 10) * 1.2f; // Ajusta este factor según tus preferencias
            return Color.HSVToColor(new float[]{hue, 1f, 1f});
        }
    }
    public void clearMatrices() {
        matrices.clear();
        invalidate(); // Volver a dibujar la vista para borrar las matrices
    }
}