package com.super2k.gltfviewerdemo.jogl;

import com.nucleus.jogl.JOGLApplication;
import com.super2k.gltfviewerdemo.GLTFViewerDemo;
import com.super2k.gltfviewerdemo.GLTFViewerDemo.ClientClasses;

public class GLTFViewerDemoJOGL extends JOGLApplication {

    public GLTFViewerDemoJOGL(String[] args) {
        super(args, GLTFViewerDemo.RENDER_VERSION, ClientClasses.clientclass);
    }

    public static void main(String[] args) {
        GLTFViewerDemoJOGL spritesApp = new GLTFViewerDemoJOGL(args);
    }
}
