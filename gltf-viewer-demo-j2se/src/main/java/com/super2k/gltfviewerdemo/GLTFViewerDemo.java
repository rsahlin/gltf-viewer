package com.super2k.gltfviewerdemo;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

import com.graphicsengine.io.GSONGraphicsEngineFactory;
import com.nucleus.CoreApp;
import com.nucleus.CoreApp.ClientApplication;
import com.nucleus.SimpleLogger;
import com.nucleus.assets.AssetManager;
import com.nucleus.camera.ViewFrustum;
import com.nucleus.common.Type;
import com.nucleus.event.EventManager;
import com.nucleus.event.EventManager.EventHandler;
import com.nucleus.io.SceneSerializer;
import com.nucleus.mmi.KeyEvent;
import com.nucleus.mmi.KeyEvent.Action;
import com.nucleus.mmi.MMIEventListener;
import com.nucleus.mmi.MMIPointerEvent;
import com.nucleus.mmi.NodeInputListener;
import com.nucleus.mmi.PointerData;
import com.nucleus.mmi.PointerMotionData;
import com.nucleus.mmi.core.InputProcessor;
import com.nucleus.mmi.core.KeyListener;
import com.nucleus.opengl.GLESWrapper.Mode;
import com.nucleus.opengl.GLESWrapper.Renderers;
import com.nucleus.opengl.GLES20Wrapper;
import com.nucleus.opengl.GLException;
import com.nucleus.renderer.NucleusRenderer;
import com.nucleus.renderer.NucleusRenderer.RenderContextListener;
import com.nucleus.renderer.Window;
import com.nucleus.scene.GLTFNode;
import com.nucleus.scene.LayerNode;
import com.nucleus.scene.Node;
import com.nucleus.scene.NodeException;
import com.nucleus.scene.RootNode;
import com.nucleus.scene.RootNodeBuilder;
import com.nucleus.scene.RootNodeImpl;
import com.nucleus.scene.gltf.AlignedNodeTransform;
import com.nucleus.scene.gltf.GLTF;
import com.nucleus.scene.gltf.Scene;
import com.nucleus.vecmath.Vec2;

public class GLTFViewerDemo
        implements MMIEventListener, RenderContextListener, ClientApplication, EventHandler<Node>, NodeInputListener,
        KeyListener {

    public enum Action {
        reset(),
        camera(),
        hand(),
        loadnext(),
        loadprevious();
        
    }
    
    
    public static class Message {
        public final String key;
        public final String value;

        public Message(String key, String value) {
            this.key = key;
            this.value = value;
        }

    }

    public enum Navigation {
        ROTATE(0),
        TRANSLATE(1),
        SCALE(2);

        public final int index;

        private Navigation(int index) {
            this.index = index;
        }

    }

    public static final String GLTFVIEWER_DEMO = "gltfviewer";
    public static final String NAME = "GLTF Render demo";
    public static final String VERSION = "0.4";
    public static final Renderers GL_VERSION = Renderers.GLES31;

    private ArrayDeque<Message> messages = new ArrayDeque<>();

    private Navigation navigationMode = Navigation.ROTATE;
    private GLTFNode gltfNode;
    private AlignedNodeTransform sceneRotator;

    private String[] folders;
    /**
     * List of found .gltf filenames - including folder name
     */
    private ArrayList<String> gltfFilenames;
    private String path;
    private int modelIndex = 0;

    /**
     * The types that can be used to represent classes when importing/exporting
     * This is used as a means to decouple serialized name from implementing class.
     * 
     */
    public enum ClientClasses implements Type<Object> {
        /**
         * This is the main class implementing the ClientApplication
         */
        clientclass(GLTFViewerDemo.class);

        private final Class<?> theClass;

        private ClientClasses(Class<?> theClass) {
            this.theClass = theClass;
        }

        @Override
        public Class<Object> getTypeClass() {
            return (Class<Object>) theClass;
        }

        @Override
        public String getName() {
            return name();
        }
    }

    protected final static String TILED_SPRITE_RENDERER_TAG = "TiledSpiteRenderer";

    Window window;
    CoreApp coreApp;
    RootNode root;
    ViewFrustum viewFrustum;
    NucleusRenderer renderer;

    public GLTFViewerDemo() {
        super();
        registerEventHandler(null);
    }

    @Override
    public void onInputEvent(MMIPointerEvent event) {

        float[] pos = event.getPointerData().getCurrentPosition();
        switch (event.getAction()) {
            case MOVE:
                float[] move = event.getPointerData().getDelta(1);
                handleMouseMove(move);
                break;
            case ACTIVE:
                break;
            case ZOOM:
                handleZoom(event.getZoom());
                break;
            default:

        }
    }

    protected void handleZoom(Vec2 zoom) {
        if (gltfNode != null && gltfNode.getGLTF() != null) {
            sceneRotator.scale(zoom);
        }
    }

    protected void handleMouseMove(float[] move) {
        InputProcessor ip = InputProcessor.getInstance();
        if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_X)) {
            move[1] = 0;
        }
        if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_Y)) {
            move[0] = 0;
        }
        if (gltfNode != null && gltfNode.getGLTF() != null) {
            GLTF gltf = gltfNode.getGLTF();
            switch (navigationMode) {
                case ROTATE:
                    sceneRotator.rotate(move);
                    break;
                case TRANSLATE:
                    sceneRotator.translate(move);
                    break;
                case SCALE:
                    break;
            }
        }
    }

    @Override
    public void contextCreated(int width, int height) {
        SimpleLogger.d(getClass(), "contextCreated()");
        window = Window.getInstance();
        if (root == null) {
            try {
                SimpleLogger.d(getClass(), "Loading scene");
                SceneSerializer<RootNode> serializer = GSONGraphicsEngineFactory.getInstance();
                if (!serializer.isInitialized()) {
                    serializer.init(renderer.getGLES(), ClientClasses.values());
                }
                initScene(serializer.importScene("assets/", "gltfscene.json", RootNodeBuilder.NUCLEUS_SCENE), width,
                        height);
                setup(width, height);

            } catch (NodeException | GLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Inits all things related to the scene - the scene is loaded
     * 
     * @param root
     * @param width
     * @param height
     * @throws GLException
     * @throws IOException
     */
    protected void initScene(RootNode root, int width, int height) throws IOException, GLException {
        this.root = root;
        viewFrustum = root.getNodeById("scene", LayerNode.class).getViewFrustum();
        gltfNode = root.getNodeById("gltf", GLTFNode.class);
        path = root.getProperty(RootNodeImpl.GLTF_PATH, null);
        folders = AssetManager.getInstance().listResourceFolders(path);
        gltfFilenames = AssetManager.getInstance().listFiles(path, folders, ".gltf");
        float[] values = viewFrustum.getValues();
        // If y is going down then reverse y so that 0 is at bottom which is the same as OpenGL
        InputProcessor.getInstance().setPointerTransform(viewFrustum.getWidth() / width,
                -viewFrustum.getHeight() / height, values[ViewFrustum.LEFT_INDEX],
                values[ViewFrustum.TOP_INDEX]);
        if (gltfNode.getGLTF() == null) {
            loadGLTFAsset();
        } else {
            initGLTF();
        }
    }

    private void initGLTF() {
        sceneRotator = new AlignedNodeTransform(gltfNode.getGLTF().getDefaultScene(),
                new float[] { 1 / viewFrustum.getWidth(), 1 / viewFrustum.getHeight() });
    }

    private void loadGLTFAsset() throws IOException, GLException {
        gltfNode.loadGLTFAsset(renderer.getGLES(), gltfFilenames.get(modelIndex));
        initGLTF();
    }

    protected void setup(int width, int height) {
        coreApp.setRootNode(root);
        coreApp.addPointerInput(root);

        InputProcessor.getInstance().setMaxPointers(20);
        InputProcessor.getInstance().addKeyListener(this);
        // InputProcessor.getInstance().addMMIListener(this);
        root.setObjectInputListener(this);
    }

    @Override
    public void surfaceLost() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(CoreApp coreApp) {
        if (!coreApp.getRenderer().isInitialized()) {
            throw new IllegalArgumentException("Renderer is not initialized!");
        }
        this.coreApp = coreApp;
        renderer = coreApp.getRenderer();
        coreApp.getRenderer().addContextListener(this);
    }

    @Override
    public void beginFrame(float deltaTime) {
        // TODO Auto-generated method stub

    }

    @Override
    public void endFrame(float deltaTime) {
        handleMessages();
    }

    @Override
    public String getAppName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void handleEvent(Node object, String category, String value) {
        SimpleLogger.d(getClass(), category);
        if (gltfNode != null && gltfNode.getGLTF() != null) {
            Action action = Action.valueOf(category);
            Scene scene = gltfNode.getGLTF().getDefaultScene();
            switch (action) {
                case reset:
                    scene.clearSceneTransform();
                    sceneRotator.resetRotation();
                    break;
                case camera:
                    int selected = scene.getSelectedCameraIndex();
                    if (selected == 0) {
                        scene.selectCameraInstance(scene.getCameraInstanceCount() - 1);
                    } else {
                        scene.selectCameraInstance(0);
                    }
                    break;
                case hand:
                    switch (navigationMode) {
                        case ROTATE:
                            navigationMode = Navigation.TRANSLATE;
                            break;
                        case TRANSLATE:
                            navigationMode = Navigation.ROTATE;
                            break;
                        default:
                            throw new IllegalArgumentException("Not implemented " + navigationMode);
                    }
                    SimpleLogger.d(getClass(), "Set navigation mode to " + navigationMode);
                    break;
                case loadnext:
                case loadprevious:
                    addMessage(new Message(action.name(), null));
                    break;
            }
        }

    }

    @Override
    public String getHandlerCategory() {
        return GLTFVIEWER_DEMO;
    }

    @Override
    public void registerEventHandler(String key) {
        EventManager.getInstance().register(key, this);
    }

    protected void addMessage(Message msg) {
        synchronized (messages) {
            messages.push(msg);
        }
    }

    private void handleMessages() {
        synchronized (messages) {
            while (!messages.isEmpty()) {
                handleMessage(messages.pop());
            }
        }
    }

    private void handleMessage(Message msg) {
        Action action = Action.valueOf(msg.key);
        switch (action) {
            case loadnext:
                load(modelIndex + 1);
                break;
            case loadprevious:
                load(modelIndex - 1);
                break;
                default:
                    //Do nothing
            
        }
    }

    private boolean deleteAsset(GLTFNode asset, GLES20Wrapper wrapper) {
        if (gltfNode != null) {
            try {
                gltfNode.deleteAsset(wrapper);
            } catch (GLException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e);
            }
            return true;
        }
        return false;
    }
    
    private void load(int index) {
        if (deleteAsset(gltfNode, renderer.getGLES())) {
            try {
                if (modelIndex < 0) {
                    modelIndex = gltfFilenames.size() - 1;
                } else if (modelIndex >= gltfFilenames.size()) {
                    modelIndex = 0;
                }
                loadGLTFAsset();
            } catch (GLException | IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    @Override
    public boolean onInputEvent(Node obj, PointerData event) {
        if (obj.getId().contentEquals("ui")) {
            switch (event.action) {
                case ZOOM:
                    handleZoom(new Vec2(event.data));
                    break;
                case DOWN:
                case UP:
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented for action " + event.action);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onDrag(Node obj, PointerMotionData drag) {
        if (obj.getId().contentEquals("ui")) {
            float[] move = drag.getDelta(1);
            handleMouseMove(move);
        }
        return true;
    }

    @Override
    public boolean onClick(Node obj, PointerData event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EventConfiguration getConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onKeyEvent(KeyEvent event) {
        switch (event.getKeyValue()) {
            case java.awt.event.KeyEvent.VK_SPACE:
                toggleSpace(event);
                break;

        }
    }

    private void toggleSpace(KeyEvent event) {
        if (event.getAction() == com.nucleus.mmi.KeyEvent.Action.PRESSED) {
            gltfNode.getNodeRenderer().forceRenderMode(Mode.POINTS);
        } else {
            gltfNode.getNodeRenderer().forceRenderMode(null);
        }
    }

}
