package zeroonezero.android.videotransition.video_processor.player;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

import zeroonezero.android.videotransition.video_processor.gl.GlFilter;
import zeroonezero.android.videotransition.video_processor.gl.GlFramebufferObject;
import zeroonezero.android.videotransition.video_processor.gl.GlSurfaceTexture;
import zeroonezero.android.videotransition.video_processor.gl.GlUtil;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glViewport;

public class PlayerRenderer extends GlFrameBufferObjectRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = PlayerRenderer.class.getSimpleName();

    private float[] MMatrix = new float[16];
    private float[] VMatrix = new float[16];
    private float[] ProjMatrix = new float[16];

    private GlFramebufferObject filterFramebufferObject;
    private PreviewFilter previewFilter;

    private GlFilter glFilter;
    private boolean isNewFilter;
    private final PlayerView glPreview;

    private float aspectRatio = 1f;
    private int rendererWidth, rendererHeight;

    private Listener listener;
    private List<RendererInput> rendererInputs = new ArrayList<>();

    private boolean updateSurface = false;

    private class RendererInput{
        Input input;
        int texName;
        GlSurfaceTexture previewTexture;
    }

    public PlayerRenderer(PlayerView glPreview) {
        super();
        this.glPreview = glPreview;
        Matrix.setIdentityM(MMatrix, 0);
        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );
    }

    public void setInputList(List<Input> inputList) {
        for(int i = 0; i < inputList.size(); i++){
            RendererInput input = new RendererInput();
            input.input = inputList.get(i);
            rendererInputs.add(input);
        }
    }

    public void setGlFilter(final GlFilter filter) {
        glPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (glFilter != null) {
                    glFilter.release();
                    glFilter = null;
                }
                glFilter = filter;
                isNewFilter = true;
                glPreview.requestRender();
            }
        });
    }

    @Override
    public void onSurfaceCreated(final EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        for(int i = 0; i < rendererInputs.size(); i++){
            RendererInput input = rendererInputs.get(i);

            final int[] args = new int[1];
            GLES20.glGenTextures(args.length, args, 0);
            input.texName = args[0];

            input.previewTexture = new GlSurfaceTexture(input.texName);
            input.previewTexture.setOnFrameAvailableListener(this);

            GLES20.glBindTexture(input.previewTexture.getTextureTarget(), input.texName);
            GlUtil.setupSampler(input.previewTexture.getTextureTarget(), GL_LINEAR, GL_NEAREST);
            GLES20.glBindTexture(GL_TEXTURE_2D, 0);

            Surface surface = new Surface(input.previewTexture.getSurfaceTexture());
            input.input.getPlayer().setVideoSurface(surface);

            GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);
        }

        filterFramebufferObject = new GlFramebufferObject();
        previewFilter = new PreviewFilter(PreviewFilter.GL_TEXTURE_EXTERNAL_OES);
        previewFilter.setup();

        synchronized (this) {
            updateSurface = false;
        }

        if (glFilter != null) {
            isNewFilter = true;
        }

    }

    @Override
    public void onSurfaceChanged(final int width, final int height) {
        filterFramebufferObject.setup(width, height);
        previewFilter.setFrameSize(width, height);
        if (glFilter != null) {
            glFilter.setFrameSize(width, height);
        }

        aspectRatio = (float) width / height;
        Matrix.frustumM(ProjMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 5, 7);

        rendererWidth = width;
        rendererHeight = height;

        if(listener != null){
            listener.onRendererSizeChanged(rendererWidth, rendererHeight);
        }
    }

    @Override
    public void onDrawFrame(final GlFramebufferObject fbo) {

        synchronized (this) {
            if (updateSurface) {
                for(int i = 0; i < rendererInputs.size(); i++){
                    rendererInputs.get(i).previewTexture.updateTexImage();
                }
                updateSurface = false;
            }
        }

        if (isNewFilter) {
            if (glFilter != null) {
                glFilter.setup();
                glFilter.setFrameSize(fbo.getWidth(), fbo.getHeight());
            }
            isNewFilter = false;
        }

        if (glFilter != null) {
            filterFramebufferObject.enable();
            glViewport(0, 0, filterFramebufferObject.getWidth(), filterFramebufferObject.getHeight());
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        for(int i = 0; i < rendererInputs.size(); i++){
            Input input = rendererInputs.get(i).input;

            float[] mvpMatrix =  new float[16];
            Matrix.multiplyMM(mvpMatrix, 0, VMatrix, 0, MMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, ProjMatrix, 0, mvpMatrix, 0);

            float[] stMatrix = new float[16];
            rendererInputs.get(i).previewTexture.getTransformMatrix(stMatrix);

            if(input.isDrawingEnabled()){
                Matrix.scaleM(mvpMatrix, 0, input.getScaleX(), input.getScaleY(), 1f);
                previewFilter.draw(rendererInputs.get(i).texName, getInputVerticesData(input), mvpMatrix, stMatrix, aspectRatio);
            }
        }


        if (glFilter != null) {
            fbo.enable();
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            glFilter.draw(filterFramebufferObject.getTexName(), fbo);
        }
    }

    @Override
    public synchronized void onFrameAvailable(final SurfaceTexture previewTexture) {
        updateSurface = true;
        glPreview.requestRender();
    }

    private float[] getInputVerticesData(Input input){
        int inputWidth = rendererWidth;
        int inputHeight = rendererHeight;
        if(input.getResolution() != null){
            inputWidth = input.getResolution().getWidth();
            inputHeight = input.getResolution().getHeight();
        }

        RectF inputRect = new RectF(0, 0f, 1, 1); // In android coordinate system
        if(input.getRect() != null){
            inputRect = input.getRect();
        }

        int usedInputWidth = (int)(inputWidth * inputRect.width());
        int usedInputHeight = (int)(inputHeight * inputRect.height());

        int[] usedRendererSize = Util.getScaleAspectFit(usedInputWidth, usedInputHeight, rendererWidth, rendererHeight);
        int usedRendererWidth = usedRendererSize[0];
        int usedRendererHeight = usedRendererSize[1];

        // OpenGl Window coordinate system
        float rendererLeft = - (usedRendererWidth / (float)rendererWidth);
        float rendererTop = (usedRendererHeight / (float)rendererHeight);
        float rendererRight = - rendererLeft;
        float rendererBottom = - rendererTop;

        // Android coordinate system to OpenGL texture coordinate system
        float inputLeft = inputRect.left;
        float inputTop = 1.0f - inputRect.top;
        float inputRight = inputRect.right;
        float inputBottom = 1.0f - inputRect.bottom;

        if(input.isFlipHorizontal()){
            inputLeft = 1.0f - inputLeft;
            inputRight = 1.0f - inputRight;
        }

        if(input.isFlipVertical()){
            inputTop = 1.0f - inputTop;
            inputBottom = 1.0f - inputBottom;
        }

        float[] verticesData = new float[]{
                // X, Y, Z,  U, V
                /*(l,t)*/ rendererLeft, rendererTop,  0.0f,      inputLeft, inputTop,
                /*(r,t)*/ rendererRight, rendererTop,  0.0f,      inputRight, inputTop,
                /*(l,b)*/ rendererLeft, rendererBottom,  0.0f,      inputLeft, inputBottom,
                /*(r,b)*/ rendererRight, rendererBottom,  0.0f,      inputRight, inputBottom
        };
        return verticesData;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getRendererWidth() {
        return rendererWidth;
    }

    public int getRendererHeight() {
        return rendererHeight;
    }

    @Override
    public void release() {
        super.release();
        if (glFilter != null) {
            glFilter.release();
        }

        for(int i = 0; i < rendererInputs.size(); i++){
            rendererInputs.get(i).previewTexture.release();
        }
    }

    public interface Listener{
        void onRendererSizeChanged(int width, int height);
    }
}
