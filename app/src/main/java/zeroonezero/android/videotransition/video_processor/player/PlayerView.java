package zeroonezero.android.videotransition.video_processor.player;

import android.content.Context;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.google.android.exoplayer2.video.VideoListener;

import java.util.ArrayList;
import java.util.List;

import zeroonezero.android.videotransition.video_processor.gl.GlFilter;
import zeroonezero.android.videotransition.video_processor.player.chooser.EConfigChooser;
import zeroonezero.android.videotransition.video_processor.player.contextfactory.EContextFactory;

public class PlayerView extends GLSurfaceView implements VideoListener {

    private final static String TAG = PlayerView.class.getSimpleName();

    private PlayerRenderer renderer;
    private List<Input> inputList = new ArrayList<>();
    private boolean playCalled;

    private int overlappingTimeMs = 4000; // 4 seconds

    private List<Integer> currentlyPlayingInputIndices = new ArrayList<>();

    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextFactory(new EContextFactory());
        setEGLConfigChooser(new EConfigChooser());

    }

    public void addSource(Uri source){
        if(playCalled){
            throw new RuntimeException("Source can't be added after play() is called");
        }
        inputList.add(new Input(getContext(), "VideoPlayer", source));
    }

    public void setGlFilter(GlFilter glFilter) {
        renderer.setGlFilter(glFilter);
    }

    /*@Override
    public void onPause() {
        super.onPause();
        renderer.release();
    }*/

    public void play(){
        if(!playCalled){
            renderer = new PlayerRenderer(this);
            renderer.setInputList(inputList);
            setRenderer(renderer);
        }
        playCalled = true;

        for(int i = 0; i < inputList.size(); i++){
            final int position = i;
            Input input = inputList.get(position);
            if(position != 0){
                input.setScaleX(0);
                input.setScaleY(0);
            }
            input.setProgressListener(new Input.ProgressListener() {
                private boolean needOverLappingChecking = true;
                @Override
                public void onProgress(long currentPosition) {
                    if(needOverLappingChecking && input.getDuration() - input.getCurrentPosition() <= overlappingTimeMs){
                        needOverLappingChecking = false;
                        final int nextPosition = position + 1;
                        if(nextPosition < inputList.size()){
                            Input nextInput = inputList.get(nextPosition);
                            nextInput.setDrawingEnabled(true);
                            nextInput.play();
                            currentlyPlayingInputIndices.remove((Integer) nextPosition);
                            currentlyPlayingInputIndices.add(nextPosition);
                        }
                    }

                    //Animation
                    if(position != 0){
                        float scale = (float)Math.max(0f, Math.min(1f, input.getCurrentPosition() / (double) overlappingTimeMs));
                        input.setScaleX(scale);
                        input.setScaleY(scale);
                    }
                }

                @Override
                public void onEnd() {
                    //We want to keep drawing the last frame of last input
                    if(position != inputList.size() - 1){
                        input.setDrawingEnabled(false);
                    }
                }
            }, 20);
        }

        //Adding first input to play
        if(inputList.size() > 0 && currentlyPlayingInputIndices.size() < 1){
            currentlyPlayingInputIndices.add(0);
        }

        for(int i = 0; i < currentlyPlayingInputIndices.size(); i++){
            inputList.get(currentlyPlayingInputIndices.get(i)).setDrawingEnabled(true);
            inputList.get(currentlyPlayingInputIndices.get(i)).play();
        }
    }

    public void pause(){
        for(int i = 0; i < inputList.size(); i++){
            inputList.get(i).pause();
        }
    }

    public void stopAndRelease(){
        for(int i = 0; i < inputList.size(); i++){
            inputList.get(i).stopAndRelease();
        }
        inputList.clear();
    }
}
