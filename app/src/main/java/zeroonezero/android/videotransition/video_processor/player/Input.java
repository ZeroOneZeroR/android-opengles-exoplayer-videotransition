package zeroonezero.android.videotransition.video_processor.player;

import android.content.Context;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.util.Size;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

public class Input implements VideoListener {
    private Size resolution;
    private RectF rect;
    private float rotation;
    private float scaleX = 1f, scaleY = 1f;
    private boolean flipHorizontal = false;
    private boolean flipVertical = false;
    private boolean drawingEnabled = false;

    private SimpleExoPlayer player;
    private ProgressListener progressListener;

    private long duration;

    private Handler progressHandler;
    private Runnable progressRunnable;
    private int progressIntervalMs = 20;

    public Input(Context context, String applicationName, Uri sourceUri){
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, applicationName));
        MediaSource source = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(sourceUri);

        player = ExoPlayerFactory.newSimpleInstance(context);
        player.prepare(source);
        player.addVideoListener(this);
        player.setPlayWhenReady(false);

        //Getting duration
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, sourceUri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(time);
            retriever.release();
        }catch (Exception e){

        }

        progressHandler = new Handler();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if(progressListener != null){
                    progressListener.onProgress(getCurrentPosition());
                    if(getCurrentPosition() >= getDuration()){
                        progressListener.onEnd();
                    }
                }
                if( getCurrentPosition() < getDuration() ){
                    progressHandler.postDelayed(progressRunnable, progressIntervalMs);
                }
            }
        };
    }

    public void play(){
        player.setPlayWhenReady(true);

        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.postDelayed(progressRunnable, progressIntervalMs);
    }

    public void pause(){
        player.setPlayWhenReady(false);
        progressHandler.removeCallbacks(progressRunnable);
    }

    public void stopAndRelease(){
        player.stop();
        player.release();
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        resolution = new Size(width, height);
    }

    /*..............Getters.............*/
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public Size getResolution() {
        return resolution;
    }

    public RectF getRect() {
        return rect;
    }

    public float getRotation() {
        return rotation;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public boolean isFlipHorizontal() {
        return flipHorizontal;
    }

    public boolean isFlipVertical() {
        return flipVertical;
    }

    public boolean isDrawingEnabled() {
        return drawingEnabled;
    }

    public long getDuration(){
        return duration;
    }

    public long getCurrentPosition(){
        return player.getCurrentPosition();
    }


    /*..............Setters.............*/
    public void setRect(RectF rect) {
        this.rect = rect;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
    }

    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    public void setDrawingEnabled(boolean drawingEnabled) {
        this.drawingEnabled = drawingEnabled;
    }

    public void setProgressListener(ProgressListener progressListener, int progressIntervalMs) {
        this.progressListener = progressListener;
        this.progressIntervalMs = progressIntervalMs;
    }


    public interface ProgressListener{
        void onProgress(long currentPosition);
        void onEnd();
    }
}
