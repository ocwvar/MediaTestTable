package com.ocwvar.mediatesttable.Cores;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkLibLoader;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Project MediaTestTable
 * Created by OCWVAR
 * On 17-7-5 下午4:30
 * File Location com.ocwvar.mediatesttable.Cores
 * This file use to :
 */
public class BLVideoCore implements IVideoCore {

    private BLCallbacks blCallbacks = null;
    private DisplayCallbacks displayCallbacks = null;
    private AndroidMediaPlayer videoPlayer = null;
    private Context appContext = null;

    //当前视频资源长度
    private long videoDuration = -1L;

    //当前状态
    private Status currentStatus = Status.EMPTY;

    //播放状态是否已准备好
    private boolean isReady = false;

    //是否准备好就执行播放
    private boolean isPlayWhenReady = false;

    public BLVideoCore(Context context) {
        this.appContext = context.getApplicationContext();
        this.blCallbacks = new BLCallbacks();
        this.displayCallbacks = new DisplayCallbacks();
    }

    /**
     * 生成播放器对象
     *
     * @return 播放器对象，在播放界面无效时返回NULL
     */
    private
    @Nullable
    AndroidMediaPlayer init() {
        if (this.displayCallbacks.surfaceHolder != null) {
            //只有在播放界面存在且有效的时候才能进行对象生成
            final AndroidMediaPlayer videoPlayer = new AndroidMediaPlayer();
            videoPlayer.setOnPreparedListener(this.blCallbacks);
            videoPlayer.setOnErrorListener(this.blCallbacks);
            videoPlayer.setOnCompletionListener(this.blCallbacks);
            videoPlayer.setDisplay(this.displayCallbacks.surfaceHolder);
            return videoPlayer;
        }
        return null;
    }

    /**
     * 准备视频
     *
     * @param filePath        视频路径
     * @param isPlayWhenReady 视频准备好后马上进行播放，此标记在成功播放后将重置为：False
     * @return 执行结果
     */
    @Override
    public boolean prepare(@NonNull String filePath, boolean isPlayWhenReady) {
        if (this.currentStatus != Status.EMPTY) {
            //如果当前已经有视频资源，则进行释放
            releaseVideo();
        }

        if (this.videoPlayer == null) {
            //如果当前视频播放器为空，则进行新的对象初始化
            this.videoPlayer = init();
        }

        try {
            //同时满足条件：
            // 1.有有效的播放器对象
            // 2.当前没有加载数据
            final boolean isReady2Prepare = (this.currentStatus == Status.EMPTY && this.videoPlayer != null);

            if (isReady2Prepare) {
                //重置状态和标记
                this.isReady = false;
                this.isPlayWhenReady = isPlayWhenReady;
                //生成数据源对象
                final Uri sourceUri = file2Uri(filePath);

                if (sourceUri != null && sourceUri != Uri.EMPTY) {
                    //数据源有效，则进行数据加载
                    this.videoPlayer.setDataSource(appContext, sourceUri);
                    this.videoPlayer.prepareAsync();
                    return true;
                }
            } else {
                return false;
            }

        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * 设置播放显示位置
     *
     * @param surfaceHolder SurfaceHolder对象
     */
    @Override
    public void setDisplayObject(SurfaceHolder surfaceHolder) {
        if (surfaceHolder != null) {
            surfaceHolder.addCallback(this.displayCallbacks);
        }
    }

    /**
     * 准备视频
     *
     * @param filePath 视频路径
     * @return 执行结果
     */
    @Override
    public boolean prepare(@NonNull String filePath) {
        return prepare(filePath, false);
    }

    /**
     * 直接准备并播放视频
     *
     * @param filePath 视频路径
     * @return 执行结果
     */
    @Override
    public boolean play(@NonNull String filePath) {
        return prepare(filePath, true);
    }

    /**
     * 播放已准备好的视频
     *
     * @return 执行结果
     */
    @Override
    public boolean play() {
        if (this.videoPlayer == null) {
            //播放器对象未加载
            return false;
        }

        try {
            switch (this.currentStatus) {
                case PAUSED:
                    //暂停状态则直接播放
                    this.videoPlayer.start();
                    this.currentStatus = Status.PLAYING;
                    return true;
                case PLAYING:
                    return false;
                case STOPPED:
                    //停止状态需要先重置开始位置再进行播放
                    this.videoPlayer.seekTo(0L);
                    this.videoPlayer.start();
                    this.currentStatus = Status.PLAYING;
                    return true;
                case EMPTY:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * @return 视频准备状态
     */
    @Override
    public boolean isReady() {
        return isReady;
    }

    /**
     * 恢复播放
     *
     * @return 执行结果
     */
    @Override
    public boolean resume() {
        return play();
    }

    /**
     * 停止播放
     *
     * @return 执行结果
     */
    @Override
    public boolean pause() {
        if (this.videoPlayer != null && this.currentStatus == Status.PLAYING) {
            try {
                this.videoPlayer.pause();
                this.currentStatus = Status.PAUSED;
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 停止播放并重置播放进度至开头 （00:00）
     *
     * @return 执行结果
     */
    @Override
    public boolean stop() {
        final boolean result = pause();
        if (result) {
            this.currentStatus = Status.STOPPED;
        }
        return result;
    }

    /**
     * 释放视频资源，调用此方法后播放器对象将变为NULL，需要重新调用 init() 获取对象，播放状态变为 Status.EMPTY
     *
     * @return 执行结果
     */
    @Override
    public boolean releaseVideo() {
        if (this.videoPlayer != null && this.currentStatus != Status.EMPTY) {
            //只有在有资源加载的情况下才能进行资源释放
            try {
                if (this.currentStatus != Status.STOPPED) {
                    //如果视频不是停止状态，则执行停止
                    this.videoPlayer.stop();
                }
                this.videoPlayer.release();
                this.videoPlayer = null;
                this.currentStatus = Status.EMPTY;
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @param isLoop 是否循环播放
     */
    @Override
    public void setLoop(boolean isLoop) {
        if (this.videoPlayer != null) {
            this.videoPlayer.setLooping(isLoop);
        }
    }

    /**
     * 定位音频位置
     *
     * @param ms 毫秒数
     * @return 执行结果
     */
    @Override
    public boolean seek2(long ms) {
        if (this.videoPlayer != null && (this.currentStatus == Status.PAUSED || this.currentStatus == Status.PLAYING)) {
            //视频状态OK
            if (ms >= 0 && ms <= this.videoDuration) {
                //跳转位置OK
                this.videoPlayer.seekTo(ms);
                return true;
            }
        }
        return false;
    }

    /**
     * @return 视频长度，无效长度返回 -1
     */
    @Override
    public long videoDuration() {
        return this.videoDuration;
    }

    /**
     * @return 当前播放位置，无效长度返回 -1
     */
    @Override
    public long currentPosition() {
        if (this.videoPlayer != null && (this.currentStatus == Status.PLAYING || this.currentStatus == Status.PAUSED)) {
            return this.videoPlayer.getCurrentPosition();
        } else if (this.videoPlayer == null || this.currentStatus == Status.STOPPED) {
            return 0L;
        }
        return -1;
    }

    /**
     * @return 当前播放器状态
     */
    @NonNull
    @Override
    public Status currentStatus() {
        return this.currentStatus;
    }

    /**
     * 将文件路径转换为Uri地址
     *
     * @param filePath 文件路径
     * @return 文件地址Uri，若无法转换则返回NULL
     */
    private
    @Nullable
    Uri file2Uri(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            //路径为空，返回NULL
            return null;
        }

        return FileProvider.getUriForFile(this.appContext, "FileProvider", new File(filePath));
    }

    /**
     * 播放器状态回调接口处理类
     */
    private class BLCallbacks implements IjkMediaPlayer.OnInfoListener, IjkMediaPlayer.OnErrorListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener, IjkLibLoader {

        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            return false;
        }

        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            stop();
        }

        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
            currentStatus = Status.EMPTY;
            return false;
        }

        /**
         * 播放器加载状态已准备好
         *
         * @param iMediaPlayer 播放器对象
         */
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            //设置：状态 标记 数据
            isReady = true;
            currentStatus = Status.PAUSED;
            videoDuration = iMediaPlayer.getDuration();

            if (isPlayWhenReady) {
                //是否需要马上进行播放
                isPlayWhenReady = false;
                play();
            }
        }

        @Override
        public void loadLibrary(String s) throws UnsatisfiedLinkError, SecurityException {

        }

    }

    /**
     * 显示的Holder状态回调
     */
    private class DisplayCallbacks implements SurfaceHolder.Callback {

        private SurfaceHolder surfaceHolder = null;

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            //在播放界面发生改变的时候，暂停视频播放
            pause();

            this.surfaceHolder = surfaceHolder;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            //在播放界面被销毁的时候，直接释放视频资源
            releaseVideo();

            this.surfaceHolder = null;
        }

    }

}
