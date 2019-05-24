package com.super2k.gltfviewerdemo;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

import com.graphicsengine.io.GSONGraphicsEngineFactory;
import com.nucleus.CoreApp;
import com.nucleus.CoreApp.ClientApplication;
import com.nucleus.SimpleLogger;
import com.nucleus.camera.ViewFrustum;
import com.nucleus.common.FileUtils;
import com.nucleus.common.Type;
import com.nucleus.environment.Lights;
import com.nucleus.event.EventManager;
import com.nucleus.event.EventManager.EventHandler;
import com.nucleus.io.SceneSerializer;
import com.nucleus.io.SceneSerializer.NodeInflaterListener;
import com.nucleus.mmi.Key;
import com.nucleus.mmi.MMIPointer;
import com.nucleus.mmi.MMIPointerInput;
import com.nucleus.mmi.Pointer;
import com.nucleus.mmi.PointerMotion;
import com.nucleus.mmi.core.CoreInput;
import com.nucleus.mmi.core.KeyInput;
import com.nucleus.opengl.GLException;
import com.nucleus.renderer.Backend.DrawMode;
import com.nucleus.renderer.NucleusRenderer;
import com.nucleus.renderer.NucleusRenderer.RenderContextListener;
import com.nucleus.renderer.NucleusRenderer.Renderers;
import com.nucleus.renderer.RenderBackendException;
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
import com.nucleus.scene.gltf.PBRMetallicRoughness;
import com.nucleus.scene.gltf.Scene;
import com.nucleus.ui.Button;
import com.nucleus.ui.Element;
import com.nucleus.ui.Toggle;
import com.nucleus.ui.UIElementInput;
import com.nucleus.vecmath.Vec2;

public class GLTFViewerDemo
        implements MMIPointerInput, RenderContextListener, ClientApplication, EventHandler<Node>, UIElementInput,
        KeyInput, NodeInflaterListener {

    public enum Action {
        reset(),
        camera(),
        hand(),
        loadnext(),
        loadprevious(),
        tbn_vectors(),
        ui();

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
    public static final String VERSION = "0.5";
    public static final Renderers GL_VERSION = Renderers.GLES31;

    private ArrayDeque<Message> messages = new ArrayDeque<>();

    private Navigation navigationMode = Navigation.ROTATE;
    private GLTFNode gltfNode;
    private int defaultSceneIndex = 0;
    private int modelSceneIndex = -1;
    private AlignedNodeTransform sceneRotator;
    private EventConfiguration eventConfig = new EventConfiguration(0.5f, 30f);

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
    public void onInput(MMIPointer event) {

        float[] pos = event.getPointerData().getCurrentPosition();
        switch (event.getAction()) {
            case MOVE:
                float[] move = event.getPointerData().getDelta(1);
                handleMouseMove(event.getPointerData());
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

    protected void handleMouseMove(PointerMotion drag) {
        float[] move = drag.getDelta(1);
        float[] translate = new float[] { move[0], move[1], 0 };
        CoreInput ip = CoreInput.getInstance();
        if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_X)) {
            translate[1] = 0;
        } else if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_Y)) {
            translate[0] = 0;
        } else if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_Z)) {
            // Up moves into the screen
            translate[2] = -move[1];
            translate[0] = 0;
            translate[1] = 0;
        } else if (ip.isKeyPressed(java.awt.event.KeyEvent.VK_L)) {
            changeLightIntensity(drag);
            return;
        } else if (ip.isKeyPressed(KeyEvent.VK_E)) {
            changeExposure(drag);
            return;
        }
        if (gltfNode != null && gltfNode.getGLTF() != null) {
            switch (navigationMode) {
                case ROTATE:
                    sceneRotator.rotate(translate);
                    break;
                case TRANSLATE:
                    sceneRotator.translate(translate);
                    break;
                case SCALE:
                    break;
            }
        }
    }

    private void changeLightIntensity(PointerMotion drag) {
        // Change light
        float width = viewFrustum.getWidth();
        float intensity = (drag.getCurrentPosition()[0] + width / 2) / (width / 8);
        Lights.getInstance().getLight().setIntensity(intensity);
        SimpleLogger.d(getClass(), "Intensity = " + intensity);
    }

    private void changeExposure(PointerMotion drag) {
        // Change exposure
        float width = viewFrustum.getWidth();
        float exposure = (drag.getCurrentPosition()[0] + width / 2) / (width / 3);
        PBRMetallicRoughness.setExposure(exposure);
        SimpleLogger.d(getClass(), "Exposure = " + exposure);
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
                    serializer.init(renderer, ClientClasses.values());
                }
                root = serializer.importScene("assets/", "gltfscene.json", RootNodeBuilder.NUCLEUS_SCENE, this);
                setup(width, height);
                initScene(width, height);

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
    protected void initScene(int width, int height) throws IOException, GLException {
        viewFrustum = root.getNodeById("scene", LayerNode.class).getViewFrustum();
        gltfNode = root.getNodeById("gltf", GLTFNode.class);
        path = root.getProperty(RootNodeImpl.GLTF_PATH, null);
        folders = FileUtils.getInstance().listResourceFolders(path);
        gltfFilenames = FileUtils.getInstance().listFiles(path, folders, ".gltf");
        float[] values = viewFrustum.getValues();
        // If y is going down then reverse y so that 0 is at bottom which is the same as OpenGL
        CoreInput.getInstance().setPointerTransform(viewFrustum.getWidth() / width,
                -viewFrustum.getHeight() / height, values[ViewFrustum.LEFT_INDEX],
                values[ViewFrustum.TOP_INDEX]);
        if (gltfNode.getGLTF() == null) {
            loadGLTFAsset();
        } else {
            initGLTF(gltfNode.getGLTF());
        }
    }

    private void initGLTF(GLTF gltf) {
        sceneRotator = new AlignedNodeTransform(gltf.getDefaultScene(),
                new float[] { 1 / viewFrustum.getWidth(), 1 / viewFrustum.getHeight(), 1 / viewFrustum.getDepth() });
        defaultSceneIndex = gltf.getSceneIndex();
        com.nucleus.scene.gltf.Node modelNode = gltf.getDefaultScene().getFirstNodeWithMesh();
        int nodeIndex = gltf.getNodeIndex(modelNode);
        Scene modelScene = new Scene(gltf, new int[] { nodeIndex });
        modelSceneIndex = gltf.addScene(modelScene);
    }

    private void loadGLTFAsset() throws IOException, GLException {
        gltfNode.loadGLTFAsset(renderer, gltfFilenames.get(modelIndex));
        initGLTF(gltfNode.getGLTF());
    }

    protected void setup(int width, int height) {
        coreApp.setRootNode(root);
        coreApp.addPointerInput(root);

        CoreInput.getInstance().setMaxPointers(20);
        CoreInput.getInstance().addKeyListener(this);
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
                // Do nothing

        }
    }

    private boolean deleteAsset(NucleusRenderer renderer, GLTFNode asset) {
        if (gltfNode != null) {
            try {
                gltfNode.deleteAsset(renderer);
            } catch (RenderBackendException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e);
            }
            return true;
        }
        return false;
    }

    private void load(int index) {
        if (deleteAsset(renderer, gltfNode)) {
            try {
                modelIndex = index;
                if (modelIndex < 0) {
                    modelIndex = gltfFilenames.size() - 1;
                } else if (modelIndex >= gltfFilenames.size()) {
                    modelIndex = 0;
                }
                loadGLTFAsset();
            } catch (GLException | IOException e) {
                e.printStackTrace();
            }
        } else {
            SimpleLogger.d(getClass(), "GLTF asset is null");
        }
    }

    @Override
    public boolean onInputEvent(Node obj, Pointer event) {
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
    public boolean onDrag(Node obj, PointerMotion drag) {
        if (obj.getId().contentEquals("ui")) {
            handleMouseMove(drag);
        }
        return true;
    }

    @Override
    public boolean onClick(Node obj, Pointer event) {
        SimpleLogger.d(getClass(), "onClick()");
        return true;
    }

    @Override
    public EventConfiguration getConfiguration() {
        return eventConfig;
    }

    @Override
    public void onKeyEvent(Key event) {
        switch (event.getKeyValue()) {
            case java.awt.event.KeyEvent.VK_SPACE:
                toggleSpace(event);
                break;

        }
    }

    private void toggleSpace(Key event) {
        if (event.getAction() == com.nucleus.mmi.Key.Action.PRESSED) {
            renderer.forceRenderMode(DrawMode.POINTS);
        } else {
            renderer.forceRenderMode(null);
        }
    }

    @Override
    public void onStateChange(Toggle toggle) {
        SimpleLogger.d(getClass(), "onStateChange() " + toggle.getId() + ", " + toggle.isSelected());
        Action action = Action.valueOf(toggle.getId());
        switch (action) {
            case camera:
                switchScene(toggle.isSelected() ? 0 : modelSceneIndex);
                resetSceneTransform();
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
            case tbn_vectors:
                GLTF.debugTBN = toggle.isSelected();
                break;
            default:
                // Do nothing.
        }
    }

    private void switchScene(int newScene) {
        GLTF gltf = gltfNode.getGLTF();
        gltf.setDefaultScene(newScene);
        sceneRotator.setNodeTarget(gltf.getDefaultScene());
    }

    @Override
    public void onPressed(Button button) {
        SimpleLogger.d(getClass(), "onPressed() " + button.getId());
        Action action = Action.valueOf(button.getId());
        switch (action) {
            case loadnext:
            case loadprevious:
                addMessage(new Message(action.name(), null));
                break;
            case reset:
                resetSceneTransform();
                break;
            default:
                // Do nothing
                break;
        }
    }

    @Override
    public void onInflated(Node node) {
        if (node instanceof Element) {
            Action action = Action.valueOf(node.getId());
            if (action != null) {
                switch (action) {
                    case tbn_vectors:
                        GLTF.debugTBN = ((Toggle) node).isSelected();
                        break;
                    default:
                        // Do nothing
                }
            }
        }
    }

    private void resetSceneTransform() {
        Scene scene = gltfNode.getGLTF().getDefaultScene();
        scene.clearSceneTransform();
        sceneRotator.resetRotation();
    }

}
