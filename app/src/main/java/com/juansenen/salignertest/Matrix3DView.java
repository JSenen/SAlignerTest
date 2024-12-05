package com.juansenen.salignertest;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class Matrix3DView extends GLSurfaceView {

    private Matrix3DRenderer renderer; // Referencia al renderer

    public Matrix3DView(Context context) {
        super(context);

        // Configurar OpenGL ES 2.0
        setEGLContextClientVersion(2);

        // Crear el renderer y asignarlo
        renderer = new Matrix3DRenderer();
        setRenderer(renderer);
    }

    // Método para obtener el renderer
    public Matrix3DRenderer getRenderer() {
        return renderer;
    }
}


