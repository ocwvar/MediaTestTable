package com.ocwvar.mediatesttable.Cores;

import android.content.Context;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Project osu-plus
 * Created by OCWVAR
 * On 17-6-14 下午10:28
 * File Location ru.nsu.ccfit.zuev.audio.serviceAudio
 * This file use to :   EXO播放核心
 *
 * 音频核心不负责检查播放资源的可用性与正确性，检查任务由音频服务负责
 */
public class EXOCore implements IAudioCore {

    private final String TAG = "播放核心EXO";

    private final Context applicationContext;
    private final SimpleExoPlayer musicPlayer;
    private final ExoCallback exoCallback;
    private VisualizerLoader visualizerLoader = null;

    /**
     * 是否循环播放
     */
    private boolean loopFlag = false;

    /**
     * 停止状态标记：次标记为True时，setPlayWhenReady(false) 停止播放的状态将从 Pause -> Stop，此标记在每次生效后将重置为 False
     */
    private boolean isStopAction = false;

    /**
     * 当前播放器状态
     */
    private Status currentStatus = Status.EMPTY;

    private CoreCallback callback = null;

    /**
     * 当前加载的音频资源长度，无效时为-1L
     */
    private long audioDuration = -1L;

    public EXOCore(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.exoCallback = new ExoCallback();
        this.musicPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext, new DefaultTrackSelector());
        this.musicPlayer.addListener(exoCallback);
    }

    /**
     * 设置核心消息显示回调
     *
     * @param callback 回调结果
     */
    @Override
    public void setCoreCallback(CoreCallback callback) {
        this.callback = callback;
    }

    /**
     * 准备音频
     *
     * @param filePath 音频路径
     * @return 执行结果
     */
    @Override
    public boolean prepare(@NonNull String filePath) {
        final Uri uri = file2Uri(filePath);
        final DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, applicationContext.getPackageName()));
        final ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        final MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, exoCallback);
        this.musicPlayer.prepare(mediaSource);
        return true;
    }

    /**
     * 直接准备并播放音频
     *
     * @param filePath 音频路径
     * @return 执行结果
     */
    @Override
    public boolean play(@NonNull final String filePath) {
        prepare(filePath);
        return play();
    }

    /**
     * 播放已准备好的音频
     *
     * @return 执行结果
     */
    @Override
    public boolean play() {
        return resume();
    }

    /**
     * 恢复播放
     *
     * @return 执行结果
     */
    @Override
    public boolean resume() {
        if (currentStatus == Status.PAUSED) {
            //暂停状态，直接恢复播放
            musicPlayer.setPlayWhenReady(true);
            return true;
        } else if (currentStatus == Status.STOPPED) {
            //已停止状态，设置播放位置到 0，然后进行播放
            seek2(0L);
            musicPlayer.setPlayWhenReady(true);
        }
        return false;
    }

    /**
     * 应用音频效果
     */
    @Override
    public String applyPlaybackEffects(@NonNull String code){
        if (TextUtils.isEmpty(code)) return null;

        if (code.startsWith("VI_")){
            return effects_visualizer(code);
        }else if (code.startsWith("SP_")){
            return effects_speed(code);
        }else if (code.startsWith("VO_")){
            return effects_volume(code);
        }
        return null;
    }

    /**
     * 效果_音量
     * @param code  执行代码
     */
    private String effects_volume(String code){
        final String value = code.substring(2);
        setVolume(Float.parseFloat(value));
        return "音量大小："+value;
    }

    /**
     * 效果_频谱
     * @param code  执行代码
     */
    private String effects_visualizer(String code){
        switch (code){
            case "VI_ON":
                //打开频谱处理器
                enableVisualizer();
                break;
            case "VI_OFF":
                disableVisualizer();
                break;
        }
        return null;
    }

    /**
     * 效果_播放速度
     * @param code  执行代码
     */
    private String effects_speed(String code){
        //根据音频播放模式采取不同的播放速度
        switch (code){
            case "SP_N":
                //原速
                this.musicPlayer.setPlaybackParameters(PlaybackParameters.DEFAULT);
                break;
            case "SP_D":
                //双倍速度
                this.musicPlayer.setPlaybackParameters(new PlaybackParameters(2.0f,2.0f));
                break;
            case "SP_S":
                //半速
                this.musicPlayer.setPlaybackParameters(new PlaybackParameters(0.5f,0.5f));
                break;
        }
        return null;
    }

    /**
     * 停止播放
     *
     * @return 执行结果
     */
    @Override
    public boolean pause() {
        if (currentStatus == Status.PLAYING) {
            musicPlayer.setPlayWhenReady(false);
            return true;
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
        if (currentStatus == Status.PAUSED || currentStatus == Status.PLAYING) {
            this.isStopAction = true;
            musicPlayer.setPlayWhenReady(false);
            return seek2(0L);
        }
        return false;
    }

    /**
     * 定位音频位置
     *
     * @param ms 毫秒数，单位毫秒
     * @return 执行结果
     */
    @Override
    public boolean seek2(final long ms) {
        if (currentStatus != Status.EMPTY && ms >= 0 && ms <= audioDuration) {
            callback.updateResultText("转跳位置："+ms);
            musicPlayer.seekTo(ms);
            return true;
        }
        return false;
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        musicPlayer.release();
    }

    /**
     * @return 音频长度，无效长度返回 -1，单位毫秒
     */
    @Override
    public long audioDuration() {
        return audioDuration;
    }

    /**
     * @return 当前播放位置，无效长度返回 -1，单位毫秒
     */
    @Override
    public long currentPosition() {
        if (currentStatus != Status.EMPTY) {
            return musicPlayer.getCurrentPosition();
        } else {
            return -1L;
        }
    }

    /**
     * @return 当前播放器状态
     */
    @NonNull
    @Override
    public Status currentStatus() {
        return currentStatus;
    }

    /**
     * 调用此方法前需要调用 enableVisualizer() 否则无法获取到数据
     *
     * @return 当前音频的FFT，无效或无法获取时返回NULL
     */
    @Nullable
    @Override
    public float[] currentSpectrum() {
        return (visualizerLoader == null) ? null : visualizerLoader.get();
    }

    /**
     * 启动频谱解析
     * 当不需要频谱数据的时候必须执行 disableVisualizer()
     */
    private void enableVisualizer() {
        //如果SessionID小于 0 则为无效请求，直接跳出
        if (musicPlayer.getAudioSessionId() < 0) return;

        if (visualizerLoader == null || visualizerLoader.sessionID != musicPlayer.getAudioSessionId() || visualizerLoader.isReleased) {
            //以下三种状况需要重新创建频谱加载器：
            //1.频谱加载器对象为空
            //2.频谱加载器当前读取的SessionID与当前不符
            //3.频谱加载器已经释放资源
            visualizerLoader = new VisualizerLoader(musicPlayer.getAudioSessionId());
        }
        visualizerLoader.enable();
    }

    /**
     * 停止频谱解析
     */
    private void disableVisualizer() {
        if (visualizerLoader != null) {
            visualizerLoader.disable();
        }
    }

    /**
     * 设置循环播放标记
     *
     * @param isLoop 是否循环播放
     */
    @Override
    public void setLoopFlag(final boolean isLoop) {
        this.loopFlag = isLoop;
    }

    /**
     * @return 当前音量大小
     */
    @Override
    public float getVolume() {
        return musicPlayer.getVolume();
    }

    /**
     * 设置音量
     *
     * @param volume 音量大小，大小由0~1，0为静音
     */
    @Override
    public void setVolume(final float volume) {
        musicPlayer.setVolume(volume);
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
        return FileProvider.getUriForFile(applicationContext, "FileProvider", new File(filePath));
    }

    /**
     * EXO播放器状态回调处理类
     */
    private final class ExoCallback implements ExoPlayer.EventListener, ExtractorMediaSource.EventListener {

        @Override
        public void onLoadError(IOException error) {
            Log.e(TAG, "发生错误，无法读取音频：" + error);
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        /**
         * 播放器状态变化回调
         *
         * @param playWhenReady 是否当状态为READY时马上播放音频
         * @param playbackState 当前状态
         */
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState){
                case ExoPlayer.STATE_BUFFERING:
                    callback.updateResultText("缓冲中");
                    break;
                case ExoPlayer.STATE_ENDED:
                    callback.updateResultText("播放结束");
                    break;
                case ExoPlayer.STATE_IDLE:
                    callback.updateResultText("未加载数据");
                    break;
                case ExoPlayer.STATE_READY:
                    callback.updateResultText("数据已加载  Play When Ready:"+playWhenReady);
                    break;
            }
            switch (playbackState) {
                case ExoPlayer.STATE_READY:
                case ExoPlayer.STATE_BUFFERING:
                    if (isStopAction) {
                        //停止标记生效
                        currentStatus = Status.STOPPED;
                        //重置标记
                        isStopAction = false;
                    } else {
                        currentStatus = (playWhenReady) ? Status.PLAYING : Status.PAUSED;
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    currentStatus = Status.STOPPED;
                    if (loopFlag) {
                        //循环播放标记
                        seek2(0);
                        play();
                    }
                    break;
                case ExoPlayer.STATE_IDLE:
                    currentStatus = Status.EMPTY;
                    break;
            }

            //在这里才能获取到加载的媒体长度
            audioDuration = musicPlayer.getDuration();
            Log.w(TAG, "播放器状态发生变化：" + currentStatus.name() + "  EXO状态：" + playbackState + "  音频长度(ms)：" + Long.toString(audioDuration));
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.e(TAG, "发生错误，播放器出现异常：" + error.getMessage());
        }

        @Override
        public void onPositionDiscontinuity() {
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }

    }

    /**
     * 频谱加载器
     */
    private final class VisualizerLoader {

        private final Visualizer visualizer;
        private final int sessionID;
        private boolean isReleased = false;

        VisualizerLoader(final int sessionID) {
            this.sessionID = sessionID;
            visualizer = new Visualizer(sessionID);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        }

        /**
         * 启动频谱解析
         */
        void enable() {
            if (!isReleased && !visualizer.getEnabled()) {
                visualizer.setEnabled(true);
            }
        }

        /**
         * 停止频谱解析
         * 调用此方法后 get() 方法无法返回有效数据
         */
        void disable() {
            if (!isReleased && visualizer.getEnabled()) {
                visualizer.setEnabled(false);
            }
        }

        /**
         * 停止并释放频谱资源
         * 调用此方法后 get() 方法无法返回有效数据
         */
        void release() {
            if (!isReleased) {
                isReleased = true;
                if (visualizer.getEnabled()) {
                    visualizer.setEnabled(false);
                }
                visualizer.release();
            }
        }

        /**
         * 获取FFT数据
         *
         * @return FFT数据，如果无效则返回NULL
         */
        @Nullable
        float[] get() {
            try {
                final byte[] bytes = new byte[1024];
                visualizer.getFft(bytes);
                return handleIntArray2PositionArray(handleByteArray2IntArray(bytes), 0, 128);
            } catch (Exception ignore) {
                return null;
            }
        }

        /**
         * 将原生FFT数据转换为IntArray的格式
         *
         * @param byteArray 原生FFT数据
         * @return 转换得到的数据，如果转换失败则返回NULL
         */
        private
        @Nullable
        int[] handleByteArray2IntArray(@Nullable byte[] byteArray) {
            if (byteArray == null) return null;
            final ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
            final int[] numberArray = new int[byteBuffer.asIntBuffer().limit()];
            try {
                //得到FloatArray数据
                byteBuffer.asIntBuffer().get(numberArray);
            } catch (Exception e) {
                Log.e("#$", "ByteArray → IntArray 发生异常：" + e);
                return null;
            }
            return numberArray;
        }

        /**
         * 将原生FFT IntArray转换为 0 ~ 限制大小 区间内的FloatArray
         *
         * @param inArray          要用于转换的IntArray
         * @param sizeLimit        限制每个Float数值的最大值 <=0 则不限制
         * @param arrayLengthLimit 输出数组的长度 <=0 则不限制
         * @return 转换得到的数据，如果转换失败则返回NULL
         */
        private
        @Nullable
        float[] handleIntArray2PositionArray(@Nullable int[] inArray, @Nullable float sizeLimit, @Nullable int arrayLengthLimit) {
            if (inArray == null || arrayLengthLimit >= inArray.length) return null;
            //根据限制创建工作数组长度
            final float[] workArray = (arrayLengthLimit <= 0) ? new float[inArray.length] : new float[arrayLengthLimit];
            for (int i = 0; i < workArray.length; i++) {
                float number = (float) inArray[i];
                if (number == 0.0f) {
                    //原本数据就是 0f 不需要重新设置
                    continue;
                }
                if (number < 0f) {
                    //数据为负数，转为正数
                    number *= -1f;
                }
                //移动小数点
                number *= 0.0000000001f;

                if (sizeLimit > 0 && sizeLimit < number) {
                    //如果大于限制数，则将数字设为最大数值
                    number = sizeLimit;
                }
                workArray[i] = number;
            }
            return workArray;
        }

    }

}
