package com.ocwvar.mediatesttable;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ocwvar.mediatesttable.Cores.BLVideoCore;
import com.ocwvar.mediatesttable.Cores.CoreCallback;
import com.ocwvar.mediatesttable.Cores.EXOCore;
import com.ocwvar.mediatesttable.Cores.IAudioCore;
import com.ocwvar.mediatesttable.Cores.IVideoCore;

/**
 * Project MediaTestTable
 * Created by OCWVAR
 * On 17-7-4 下午5:33
 * File Location com.ocwvar.mediatesttable
 * This file use to :
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, CoreCallback {

    private IAudioCore core = null;
    private IVideoCore videoCore = null;
    private EditText sourcePath = null,inputCode = null;
    private TextView resultShower = null;
    private SurfaceView videoDisplay = null;

    private int playMode = 0;   //0:音频  1:视频

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        core = new EXOCore(MainActivity.this);
        core.setCoreCallback(MainActivity.this);

        videoCore = new BLVideoCore(MainActivity.this);

        sourcePath = (EditText) findViewById(R.id.sourcePath);
        inputCode = (EditText) findViewById(R.id.inputCode);
        resultShower = (TextView) findViewById(R.id.resultShower);
        videoDisplay = (SurfaceView) findViewById(R.id.video);

        findViewById(R.id.actionLoop).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionPause).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionPlay).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionSeek).setOnClickListener(MainActivity.this);
        findViewById(R.id.codeStart).setOnClickListener(MainActivity.this);
        findViewById(R.id.sourceLoad).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionDuration).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionStop).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionPOS).setOnClickListener(MainActivity.this);
        findViewById(R.id.actionRelease).setOnClickListener(MainActivity.this);
        ((RadioGroup)findViewById(R.id.modes)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (i == R.id.modeAudio){
                    playMode = 0;
                }else {
                    playMode = 1;
                }
            }
        });

        videoCore.setDisplayObject(videoDisplay.getHolder());
        sourcePath.setText(Environment.getExternalStorageDirectory().getPath()+"/movies/v2.flv");
    }

    @Override
    public void onClick(View view) {
        try {
            if (playMode == 0){
                exoActions(view.getId());
            }else if (playMode == 1){
                ijkActions(view.getId());
            }
        } catch (Exception e) {
            updateResult("发生异常！"+e.getMessage());
            e.printStackTrace();
        }
    }

    private void exoActions(int id){
        final String sourcePathString = sourcePath.getText().toString();
        final String inputCodeString = inputCode.getText().toString();
        switch (id){
            case R.id.actionPlay:
                core.play();
                break;
            case R.id.actionPause:
                core.pause();
                break;
            case R.id.actionSeek:
                core.seek2(Long.parseLong(inputCodeString));
                break;
            case R.id.actionLoop:
                core.setLoopFlag(true);
                break;
            case R.id.codeStart:
                updateResult(core.applyPlaybackEffects(inputCodeString));
                break;
            case R.id.sourceLoad:
                if (core.prepare(sourcePathString)){
                    updateResult("加载资源成功");
                }else {
                    updateResult("加载资源失败");
                }
                break;
            case R.id.actionRelease:
                core.release();
                break;
            case R.id.actionStop:
                core.stop();
                break;
            case R.id.actionDuration:
                updateResult("音频长度："+Long.toString(core.audioDuration()));
                break;
            case R.id.actionPOS:
                updateResult("播放位置："+Long.toString(core.currentPosition()));
                break;
        }
    }

    private void ijkActions(int id){
        final String sourcePathString = sourcePath.getText().toString();
        final String inputCodeString = inputCode.getText().toString();
        switch (id){
            case R.id.actionPlay:
                videoCore.play();
                break;
            case R.id.actionPause:
                videoCore.pause();
                break;
            case R.id.actionSeek:
                videoCore.seek2(Long.parseLong(inputCodeString));
                break;
            case R.id.actionLoop:
                videoCore.setLoop(true);
                break;
            case R.id.codeStart:
                break;
            case R.id.sourceLoad:
                if (videoCore.prepare(sourcePathString,false)){
                    updateResult("加载资源成功");
                }else {
                    updateResult("加载资源失败");
                }
                break;
            case R.id.actionRelease:
                videoCore.releaseVideo();
                break;
            case R.id.actionStop:
                videoCore.stop();
                break;
            case R.id.actionDuration:
                updateResult("视频长度："+Long.toString(videoCore.videoDuration()));
                break;
            case R.id.actionPOS:
                updateResult("播放位置："+Long.toString(videoCore.currentPosition()));
                break;
        }
    }

    private void updateResult(final String text){
        if (TextUtils.isEmpty(text)){
            resultShower.append("\n无返回结果");
        }else {
            resultShower.append("\n"+text);
        }
    }

    @Override
    public void updateResultText(String text) {
        updateResult(text);
    }

}
