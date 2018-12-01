package agora.ojus.example.com.conferenceapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;


public class ConferenceActivity extends AppCompatActivity {

    public static final String CHANNEL_NAME = "hi5";

    private RtcEngine mRtcEngine;

    private IRtcEngineEventHandler mRtcEventHandler ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        initMRtcEventHandler();
        initRtcEngine();
        joinChannel();
        setupLocalVideo();
    }

    private void initMRtcEventHandler(){
        mRtcEventHandler = new IRtcEngineEventHandler() {
            @Override
            public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
                Log.i("AGORA_UID",""+uid);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupRemoteVideo(uid);
                    }
                });
            }

            @Override
            public void onUserOffline(int uid, int reason) { // Tutorial Step 7
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onRemoteUserOffline();
                    }
                });
            }

            @Override
            public void onUserJoined(int uid, int elapsed){
                super.onUserJoined(uid, elapsed);
            }

            @Override
            public void onLeaveChannel(RtcStats stats) {
                leaveChannel();
            }

            @Override
            public void onUserMuteVideo(final int uid, final boolean muted) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onRemoteUserMuteVideo(uid, muted);
                    }
                });
            }
            @Override
            public void onVideoStopped() {
                super.onVideoStopped();
                onLocalVideoStopped();
            }
        };
    }
    private void initRtcEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            joinChannel();
        } catch (Exception e) {
            Log.e("INIT_RTC_ENGINE_FAILED", Log.getStackTraceString(e));

            throw new RuntimeException("RTC engine initialization failed : " + Log.getStackTraceString(e));
        }
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout remoteVideoContainer = findViewById(R.id.remote_video);

        //allows only one remote video
        if (remoteVideoContainer.getChildCount() >= 1) {
            return;
        }

        SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getBaseContext());
        remoteVideoContainer.addView(remoteSurfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        remoteSurfaceView.setTag(uid);

    }

    private void setupLocalVideo(){
        FrameLayout localVideoContainer = findViewById(R.id.local_video);

        SurfaceView localSurfaceView = RtcEngine.CreateRendererView(getBaseContext());
        localSurfaceView.setZOrderMediaOverlay(true); // to show on top of the remote video canvas
        localVideoContainer.addView(localSurfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_FIT, 0));

    }
    private void onRemoteUserOffline() {
        FrameLayout remoteVideoContainer = findViewById(R.id.remote_video);
        remoteVideoContainer.removeAllViews();
    }

    private void onRemoteUserMuteVideo(int uid, boolean muted){
        SurfaceView remoteSurfaceView = (SurfaceView) ((FrameLayout) findViewById(R.id.remote_video)).getChildAt(0);
        Integer id = (Integer) remoteSurfaceView.getTag();
        if(uid == id)
            remoteSurfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);

    }

    private void onLocalVideoStopped(){
        FrameLayout container = findViewById(R.id.local_video);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        surfaceView.setZOrderMediaOverlay(false);
        surfaceView.setVisibility(View.GONE);
    }

    private void joinChannel() {
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.joinChannel(null, CHANNEL_NAME, "opt data", 0);
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    //when app is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
    }
}
