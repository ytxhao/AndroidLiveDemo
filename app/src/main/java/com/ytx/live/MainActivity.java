package com.ytx.live;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ytx.live.jni.PushNative;
import com.ytx.live.listener.LiveStateChangeListener;
import com.ytx.live.pusher.LivePusher;

public class MainActivity extends AppCompatActivity implements LiveStateChangeListener {

    static {
        System.loadLibrary("native-lib");
    }

    static final String URL = "rtmp://47.90.99.189:1935/live/demo";
    private LivePusher live;


    private Handler handler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case PushNative.CONNECT_FAILED:
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    //Log.d("jason", "连接失败..");
                    break;
                case PushNative.INIT_FAILED:
                    Toast.makeText(MainActivity.this, "初始化失败", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface);
        //相机图像的预览
        live = new LivePusher(surfaceView.getHolder());
    }

    /**
     * 开始直播
     */
    public void mStartLive(View view) {
        Button btn = (Button)view;
        if(btn.getText().equals("开始直播")){
            live.startPush(URL,this);
            btn.setText("停止直播");
        }else{
            live.stopPush();
            btn.setText("开始直播");
        }
    }

    /**
     * 切换摄像头
     * @param btn
     */
    public void mSwitchCamera(View btn) {
        live.switchCamera();
    }

    //改方法执行仍然在子线程中，发送消息到UI主线程
    @Override
    public void onError(int code) {
        handler.sendEmptyMessage(code);
    }
}
