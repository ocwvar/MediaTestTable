package com.ocwvar.mediatesttable.Cores;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Project osu-plus
 * Created by OCWVAR
 * On 17-6-14 下午10:09
 * File Location ru.nsu.ccfit.zuev.audio.serviceAudio
 * This file use to :   音频功能接口
 */
public interface IAudioCore {

    /**
     * 准备音频
     *
     * @param filePath 音频路径
     * @return 执行结果
     */
    boolean prepare(@NonNull final String filePath);

    /**
     * 设置核心消息显示回调
     *
     * @param callback 回调结果
     */
    void setCoreCallback(CoreCallback callback);

    /**
     * 直接准备并播放音频
     *
     * @param filePath 音频路径
     * @return 执行结果
     */
    boolean play(@NonNull final String filePath);

    /**
     * 测试播放效果
     *
     * @param code 指令
     * @return 返回结果，没有结果返回NULL
     */
    @Nullable
    String applyPlaybackEffects(String code);

    /**
     * 播放已准备好的音频
     *
     * @return 执行结果
     */
    boolean play();

    /**
     * 恢复播放
     *
     * @return 执行结果
     */
    boolean resume();

    /**
     * 停止播放
     *
     * @return 执行结果
     */
    boolean pause();

    /**
     * 停止播放并重置播放进度至开头 （00:00）
     *
     * @return 执行结果
     */
    boolean stop();

    /**
     * 定位音频位置
     *
     * @param ms 毫秒数
     * @return 执行结果
     */
    boolean seek2(final long ms);

    /**
     * 释放资源
     */
    void release();

    /**
     * @return 音频长度，无效长度返回 -1
     */
    long audioDuration();

    /**
     * @return 当前播放位置，无效长度返回 -1
     */
    long currentPosition();

    /**
     * @return 当前播放器状态
     */
    @NonNull
    Status currentStatus();

    /**
     * 调用此方法前需要调用 enableVisualizer() 否则无法获取到数据
     *
     * @return 当前音频的FFT，无效或无法获取时返回NULL
     */
    @Nullable
    float[] currentSpectrum();

    /**
     * @return 当前音量大小
     */
    float getVolume();

    /**
     * 设置音量
     *
     * @param volume 音量大小，大小由0~1，0为静音
     */
    void setVolume(final float volume);

    /**
     * 设置循环播放标记
     *
     * @param isLoop 是否循环播放
     */
    void setLoopFlag(final boolean isLoop);

}
