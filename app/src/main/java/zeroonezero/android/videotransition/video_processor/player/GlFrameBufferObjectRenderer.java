package zeroonezero.android.videotransition.video_processor.player;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import zeroonezero.android.videotransition.video_processor.gl.GlFilter;
import zeroonezero.android.videotransition.video_processor.gl.GlFramebufferObject;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;



public abstract class GlFrameBufferObjectRenderer implements GLSurfaceView.Renderer {

    private GlFramebufferObject framebufferObject;
    private GlFilter normalShader;

    private final Queue<Runnable> runOnDraw;

    private boolean released = false;


    protected GlFrameBufferObjectRenderer() {
        normalShader = new GlFilter();
        runOnDraw = new LinkedList<Runnable>();
    }


    @Override
    public final void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
        if (released) return;

        framebufferObject = new GlFramebufferObject();
        normalShader.setup();
        onSurfaceCreated(config);
    }

    @Override
    public final void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        if (released) return;

        framebufferObject.setup(width, height);
        normalShader.setFrameSize(width, height);
        onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, framebufferObject.getWidth(), framebufferObject.getHeight());
    }

    @Override
    public final void onDrawFrame(final GL10 gl) {
        if (released) return;

        runAll(runOnDraw);
        framebufferObject.enable();

        onDrawFrame(framebufferObject);

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        normalShader.draw(framebufferObject.getTexName(), null);
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    protected void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public boolean isReleased() {
        return released;
    }

    public void release(){
        if(framebufferObject != null){
            framebufferObject.release();
            framebufferObject = null;
        }

        if(normalShader != null){
            normalShader.release();
            normalShader = null;
        }

        runOnDraw.clear();

        released = true;
    }

    @Override
    protected void finalize() throws Throwable { }

    public abstract void onSurfaceCreated(EGLConfig config);

    public abstract void onSurfaceChanged(int width, int height);

    public abstract void onDrawFrame(GlFramebufferObject fbo);

}
