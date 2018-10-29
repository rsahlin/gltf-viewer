package com.super2k.gltfviewerdemo.lwjgl3;

import com.nucleus.lwjgl3.LWJGL3Application;
import com.super2k.gltfviewerdemo.GLTFViewerDemo;
import com.super2k.gltfviewerdemo.GLTFViewerDemo.ClientClasses;

public class GLTFViewerDemoLWJGL3 extends LWJGL3Application {

    public GLTFViewerDemoLWJGL3(String[] args) {
        super(args, GLTFViewerDemo.GL_VERSION, ClientClasses.clientclass);

    }

    public static void main(String[] args) {
        GLTFViewerDemoLWJGL3 main = new GLTFViewerDemoLWJGL3(args);
        main.run();
    }

}
