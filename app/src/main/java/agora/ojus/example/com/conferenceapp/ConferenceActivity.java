package agora.ojus.example.com.conferenceapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;


public class ConferenceActivity extends AppCompatActivity {

    public static final String CHANNEL_NAME = "hi5";

    private RtcEngine mRtcEngine;
    private IRtcEngineEventHandler mRtcEventHandler ;
    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    };

    public void init(){
        initRtcEngine();
        setupLocalVideo();
        joinChannel();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        initMRtcEventHandler();
        if (checkPermissions()) {
            Log.i("PERMISSIONS_EXIST", "all permissions exist");
            init();
        }
    }

    public boolean checkPermissions() {
        for(String permission : PERMISSIONS) {
            Log.i("CHECK_PERMISSION", permission);
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {
                Log.i("REQUESTING_PERMISSIONS", "App does not have permissions, requesting permissions");
                ActivityCompat.requestPermissions(this,
                        PERMISSIONS,
                        PERMISSION_REQ_ID);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.i("REQUESTED_CODE", requestCode+"");
        for(String permission : permissions){
            Log.i("PERMISSIONS", permission);
        }
        for(int grantResult : grantResults){
            Log.i("GRANT_RESULTS", grantResult+"");
        }
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
            public void onUserOffline(int uid, int reason) {
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
            Log.i("INIT_RTC_ENGINE_SUCCESS", "successfully created Rtc Engine");
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
