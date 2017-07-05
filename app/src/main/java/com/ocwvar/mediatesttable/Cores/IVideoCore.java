package com.ocwvar.mediatesttable.Cores;

import android.support.annotation.NonNull;
import android.view.SurfaceHolder;

/**
 * Project MediaTestTable
 * Created by OCWVAR
 * On 17-7-5 下午4:31
 * File Location com.ocwvar.mediatesttable.Cores
 * This file use to :
 */
public interface IVideoCore {

    /**
     * 设置播放显示位置
     *
     * @param surfaceHolder SurfaceHolder对象
     */
    void setDisplayObject(SurfaceHolder surfaceHolder);

    /**
     * 准备视频 (异步)
     *
     * @param filePath 视频路径
     * @param isPlayWhenReady 视频准备好后马上进行播放
     * @return 执行结果
     */
    boolean prepare(@NonNull final String filePath,final boolean isPlayWhenReady);

    /**
     * 准备视频 (异步)
     *
     * @param filePath 视频路径
     * @return 执行结果
     */
    boolean prepare(@NonNull final String filePath);

    /**
     * 直接准备(异步)并播放视频
     *
     * @param filePath 视频路径
     * @return 执行结果
     */
    boolean play(@NonNull final String filePath);

    /**
     * @return  视频准备状态
     */
    boolean isReady();

    /**
     * 播放已准备好的视频
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
     * 释放视频资源
     * @return  执行结果
     */
    boolean releaseVideo();

    /**
     * @return 视频长度，无效长度返回 -1
     */
    long videoDuration();

    /**
     * @param isLoop    是否循环播放
     */
    void setLoop(boolean isLoop);

    /**
     * @return 当前播放位置，无效长度返回 -1
     */
    long currentPosition();

    /**
     * @return 当前播放器状态
     */
    @NonNull
    Status currentStatus();

}
