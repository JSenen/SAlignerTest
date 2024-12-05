package com.juansenen.salignertest;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Matrix3DRenderer implements GLSurfaceView.Renderer {

    private int[][] matrix;
    private int rows, cols;

    // Buffers para datos de los puntos
    private FloatBuffer vertexBuffer;
    private int program;

    public Matrix3DRenderer() {
        // Inicializa una matriz vacía por defecto
        this.matrix = new int[48][48];
        this.rows = matrix.length;
        this.cols = matrix[0].length;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
        this.rows = matrix.length;
        this.cols = matrix[0].length;
        updateVertexBuffer();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f); // Fondo negro
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Habilitar profundidad

        // Compilar shaders y crear el programa
        String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "}";
        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Usar el programa de shaders
        GLES20.glUseProgram(program);

        // Dibujar los puntos
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        float[] color = {1f, 1f, 1f, 1f}; // Blanco
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, rows * cols);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void updateVertexBuffer() {
        float[] vertices = new float[rows * cols * 3];
        float stepX = 2.0f / cols;
        float stepY = 2.0f / rows;

        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float x = -1.0f + j * stepX; // Coordenada X
                float y = -1.0f + i * stepY; // Coordenada Y
                float z = (float) matrix[i][j] / 4096.0f; // Coordenada Z
                vertices[index++] = x;
                vertices[index++] = y;
                vertices[index++] = z;
            }
        }

        // Crear el buffer para los vértices
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
