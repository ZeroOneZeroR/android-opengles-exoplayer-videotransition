package zeroonezero.android.videotransition;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import zeroonezero.android.videotransition.video_processor.player.PlayerView;


public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_SELECTION_REQUEST_CODE_1 = 1;
    private static final int VIDEO_SELECTION_REQUEST_CODE_2 = 2;

    private FrameLayout playerViewHolder;
    private PlayerView playerView;
    private Button playPauseButton;

    private List<Uri> uriList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        uriList.add(null);
        uriList.add(null);
        setUpViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndRelease();
    }

    private void play(){
        if(playerView == null) return;
        playerView.play();
        playPauseButton.setText(R.string.play_pause_btn_pause_text);
    }

    private void pause(){
        if(playerView == null) return;
        playerView.pause();
        playPauseButton.setText(R.string.play_pause_btn_play_text);
    }

    private void stopAndRelease(){
        if(playerView != null){
            playerView.stopAndRelease();
            playerViewHolder.removeView(playerView);
            playerView = null;
        }
    }

    private void setUpViews() {
        playerViewHolder = findViewById(R.id.player_view_holder);

        findViewById(R.id.select_video_btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFromGallery(VIDEO_SELECTION_REQUEST_CODE_1);
            }
        });

        findViewById(R.id.select_video_btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFromGallery(VIDEO_SELECTION_REQUEST_CODE_2);
            }
        });

        // play pause
        playPauseButton = findViewById(R.id.play_pause_btn);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playPauseButton.getText().toString().equals(MainActivity.this.getString(R.string.play_pause_btn_pause_text))) {
                    pause();
                } else {
                    if(uriList.get(0) == null || uriList.get(1) == null){
                        Toast.makeText(MainActivity.this, "Add two videos.", Toast.LENGTH_SHORT).show();
                    }else{
                        if(playerView == null) setupPlayerView();
                        play();
                    }
                }
            }
        });

        findViewById(R.id.restart_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(uriList.get(0) == null || uriList.get(1) == null){
                    Toast.makeText(MainActivity.this, "Add two videos.", Toast.LENGTH_SHORT).show();
                }else{
                    stopAndRelease();
                    setupPlayerView();
                    play();
                }
            }
        });
    }

    private void setupPlayerView(){
        playerView = new PlayerView(MainActivity.this);
        playerViewHolder.addView(playerView);
        playerView.addSource(uriList.get(0));
        playerView.addSource(uriList.get(1));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIDEO_SELECTION_REQUEST_CODE_1){
            if(resultCode == RESULT_OK && data != null){
                addSourceUri(0, data.getData());
                ((Button)findViewById(R.id.select_video_btn_1)).setText(R.string.change_video_text);
            }
        }else if(requestCode == VIDEO_SELECTION_REQUEST_CODE_2){
            if(resultCode == RESULT_OK && data != null){
                addSourceUri(1, data.getData());
                ((Button)findViewById(R.id.select_video_btn_2)).setText(R.string.change_video_text);
            }
        }
    }

    private void addSourceUri(int position, Uri uri){
        stopAndRelease();
        uriList.set(position, uri);
    }

    public void chooseFromGallery(int requestCode){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }
}