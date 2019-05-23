package com.super2k.gltfviewerdemo.android;

import com.nucleus.CoreApp;
import com.nucleus.android.NucleusActivity;
import com.nucleus.renderer.NucleusRenderer.Renderers;
import com.super2k.gltfviewerdemo.GLTFViewerDemo;
import com.super2k.gltfviewerdemo.GLTFViewerDemo.ClientClasses;

import android.os.Bundle;

public class GLTFViewerDemoActivity extends NucleusActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        useChoreographer = true;
        CoreApp.setClientClass(ClientClasses.clientclass);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Renderers getRenderVersion() {
        return GLTFViewerDemo.GL_VERSION;
    }

    @Override
    public int getSamples() {
        return 0;
    }

}
