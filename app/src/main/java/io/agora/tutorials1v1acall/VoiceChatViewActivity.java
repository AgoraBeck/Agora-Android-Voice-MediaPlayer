package io.agora.tutorials1v1acall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;


public class VoiceChatViewActivity extends AppCompatActivity implements View.OnClickListener  {

    private static final String LOG_TAG = VoiceChatViewActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 23;


    private MediaPlayer mediaPlayer = new MediaPlayer();

    private RtcEngine mRtcEngine;// Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        @Override
        public void onUserOffline(final int uid, final int reason) { // Tutorial Step 4
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft(uid, reason);
                }
            });
        }

        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) { // Tutorial Step 6
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVoiceMuted(uid, muted);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_voice_chat_view);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            initAgoraEngineAndJoinChannel();
            initMediaPlayer();
        }

        Button btnPlay = (Button) findViewById(R.id.btnPlay);
        Button btnPause = (Button) findViewById(R.id.btnPause);
        Button btnStop = (Button) findViewById(R.id.btnStop);

        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        joinChannel();               // Tutorial Step 2
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnPlay:
                //如果没在播放中，立刻开始播放。
                if(!mediaPlayer.isPlaying()){
                    mediaPlayer.start();
                }
                break;
            case R.id.btnPause:
                //如果在播放中，立刻暂停。
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }
                break;
            case R.id.btnStop:
                //如果在播放中，立刻停止。
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.reset();
                    initMediaPlayer();//初始化播放器 MediaPlayer
                }
                break;
            default:
                break;
        }
    }

    // Tutorial Step 7
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    // Tutorial Step 5
    public void onSwitchSpeakerphoneClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.setEnableSpeakerphone(view.isSelected());
    }

    // Tutorial Step 3
    public void onEncCallClicked(View view) {
        leaveChannel();
//        finish();
    }

    public void onJoinClicked (View view) {
        joinChannel();
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void joinChannel() {
        mRtcEngine.setParameters("{\"rtc.log_filter\": 65535}");
        mRtcEngine.joinChannel(null, "voiceDemoChannel1", "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 3
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 4
    private void onRemoteUserLeft(int uid, int reason) {
        showLongToast(String.format(Locale.US, "user %d left %d", (uid & 0xFFFFFFFFL), reason));
        View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
        tipMsg.setVisibility(View.VISIBLE);
    }

    // Tutorial Step 6
    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
        showLongToast(String.format(Locale.US, "user %d muted or unmuted %b", (uid & 0xFFFFFFFFL), muted));
    }

    private void initMediaPlayer() {
        try {
            // 打开指定音乐文件,获取assets目录下指定文件的AssetFileDescriptor对象
            AssetFileDescriptor afd = getResources().getAssets().openFd("sound.mp3");
            mediaPlayer.reset();

            // 使用MediaPlayer加载指定的声音文件。
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());

            mediaPlayer.setLooping(true);//设置为循环播放
            mediaPlayer.prepare();//初始化播放器MediaPlayer

        } catch (IOException e) {
            return;
        }
    }

}
