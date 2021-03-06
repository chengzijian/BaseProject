package com.classic.android.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用名称: BaseProject
 * 包 名 称: com.classic.android.base
 *
 * 文件描述: 启动页
 * 创 建 人: msdx {https://github.com/msdx/androidkit}
 * 创建时间: 2015/11/4 18:34
 */
@SuppressWarnings("unused") public abstract class BaseSplashActivity extends Activity {

    /**
     * 后台任务完成的标志。
     */
    private static final byte BACKGROUND_FINISH  = 0x01;
    /**
     * 前台任务完成的标志。
     */
    private static final byte FRONTGROUND_FINISH = 0x10;
    /**
     * 表示要播放开场动画。
     */
    private static final int  SPLASH_PLAY        = 0;
    /**
     * 开场动画的资源。
     */
    private List<SplashImgResource> mResources;
    /**
     * 图片背景颜色。默认为白色。
     */
    private int mBackgroundColor = 0xFFFFFFFF;
    /**
     * UI线程。
     */
    private Handler   mUiHandler;
    /**
     * 用来显示动画。
     */
    private ImageView mSplashImage;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow()
            .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setContentView(createLayout());
        init();
        setSplashResources(mResources);
        showSplash();
        runOnMainThread();
        startOnBackground();
    }

    private void init() {
        mResources = new ArrayList<>();
        mUiHandler = new UIHandler(this);
    }

    /**
     * 设置开场动画的图片资源。
     *
     * @param resources 开场动画的图片资源。
     */
    protected abstract void setSplashResources(List<SplashImgResource> resources);

    /**
     * 返回下一个要启动的Activity。
     *
     * @return 下一个要启动的Activity。
     */
    protected abstract Class<?> nextActivity();

    /**
     * 显示开场动画。
     */
    protected void showSplash() {
        int delayTime = 0;
        for (final SplashImgResource resource : mResources) {
            Message msg = mUiHandler.obtainMessage(SPLASH_PLAY, resource);
            mUiHandler.sendMessageDelayed(msg, delayTime);
            delayTime += resource.mPlayerTime;
        }
        mUiHandler.sendEmptyMessageDelayed(FRONTGROUND_FINISH, delayTime);
    }

    /**
     * 执行耗时的操作。
     */
    private void startOnBackground() {
        new Thread(new Runnable() {
            @Override public void run() {
                runOnBackground();
                mUiHandler.sendEmptyMessage(BACKGROUND_FINISH);
            }
        }).start();
    }

    /**
     * 创建启动时的界面Layout。
     *
     * @return 返回创建的界面Layout.
     */
    private View createLayout() {
        FrameLayout layout = new FrameLayout(this);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        layout.setBackgroundColor(getBackgroundColor());
        mSplashImage = new ImageView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                                                       FrameLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        layout.addView(mSplashImage, params);

        return layout;
    }

    /**
     * 获取背景颜色。
     *
     * @return 背景颜色。默认为白色。
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * 设置背景颜色。
     *
     * @param backgroundColor 要设置的背景颜色。
     */
    public void setBackgroundColor(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
    }

    /**
     * 在前台中执行的代码。
     */
    protected void runOnMainThread() {
    }

    /**
     * 在后台中执行的代码。在此进行比较耗时的操作。
     */
    protected void runOnBackground() {
    }

    /**
     * 如果需要传送数据到下一个activity，可以重写该方法。
     */
    protected void setIntentDate(Intent intent) {
    }

    /**
     * 是否自动跳转下一个activity，默认为true。如果需要自己跳转，则重写该方法并返回false。
     */
    protected boolean isAutoStartNextActivity() {
        return true;
    }

    private static class UIHandler extends Handler {

        /**
         * 是否需要等待。
         */
        private int isWaiting = 0;
        private WeakReference<BaseSplashActivity> activity;

        UIHandler(BaseSplashActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            if (msg.what == SPLASH_PLAY) {
                SplashImgResource resource = (SplashImgResource)msg.obj;
                AlphaAnimation animation = new AlphaAnimation(resource.mStartAlpha, 1f);
                animation.setDuration(resource.mPlayerTime);
                BaseSplashActivity splash = activity.get();
                if (splash != null) {
                    if (resource.isExpand) {
                        splash.mSplashImage.setScaleType(ScaleType.FIT_XY);
                    } else {
                        splash.mSplashImage.setScaleType(ScaleType.CENTER);
                    }
                    splash.mSplashImage.setImageResource(resource.mResId);
                    splash.mSplashImage.startAnimation(animation);
                }
                return;
            }

            if (msg.what == BACKGROUND_FINISH || msg.what == FRONTGROUND_FINISH) {

                isWaiting |= msg.what;
                // 当后台或前台的任务未完成时，不执行Activity的跳转。
                BaseSplashActivity splash = activity.get();
                if (splash != null && (isWaiting == (BACKGROUND_FINISH | FRONTGROUND_FINISH))) {
                    if (splash.isAutoStartNextActivity()) {
                        Intent intent = new Intent(splash, splash.nextActivity());
                        splash.setIntentDate(intent);
                        splash.startActivity(intent);
                        splash.finish();
                    }
                }
            }
        }
    }

    /**
     * 开场动画的图片资源类。封装了图片、播放时间、开始时的透明程度。
     *
     * @author msdx
     */
    public static final class SplashImgResource implements Serializable {

        /**
         * 序列化ID。
         */
        private static final long serialVersionUID = -2257252088641281804L;
        /**
         * 资源图片ID.
         */
        private int   mResId;
        /**
         * 播放时间，单位为毫秒。
         */
        private int   mPlayerTime;
        /**
         * 开始时的透明程度。0-1之间。
         */
        private float mStartAlpha;

        /**
         * 图片是否扩展。
         */
        private boolean isExpand;

        public SplashImgResource() {
        }

        /**
         * 开场动画资源的构造方法。
         *
         * @param resId      图片资源的ID。
         * @param playerTime 图片资源的播放时间，单位为毫秒。。
         * @param startAlpha 图片资源开始时的透明程度。0-255之间。
         */
        public SplashImgResource(int resId, int playerTime, float startAlpha, boolean isExpand) {
            super();
            this.mResId = resId;
            this.mPlayerTime = playerTime;
            this.mStartAlpha = startAlpha;
            this.isExpand = isExpand;
        }

        /**
         * 获取资源图片ID。
         *
         * @return 资源图片ID。
         */
        public int getResId() {
            return mResId;
        }

        /**
         * 设置资源图片ID.
         *
         * @param mResId 要设置的资源图片ID.
         */
        public void setResId(int mResId) {
            this.mResId = mResId;
        }

        /**
         * 返回资源图片的播放时间。
         *
         * @return 资源图片的播放时间。
         */
        public int getPlayerTime() {
            return mPlayerTime;
        }

        /**
         * 设置资源图片的播放时间。
         *
         * @param playerTime 资源图片的播放时间。
         */
        public void setPlayerTime(int playerTime) {
            this.mPlayerTime = playerTime;
        }

        /**
         * 得到资源开始时的透明程度。
         */
        public float getStartAlpha() {
            return mStartAlpha;
        }

        /**
         * 设置资源开始时的透明程度。
         */
        public void setStartAlpha(float startAlpha) {
            this.mStartAlpha = startAlpha;
        }

        /**
         * 返回图片是否设置扩展。
         */
        public boolean isExpand() {
            return isExpand;
        }

        /**
         * 设置图片是否扩展。
         *
         * @param isExpand 如果为true，则图片会被拉伸至全屏幕大小进行展示，否则按原大小展示。
         */
        public void setExpand(boolean isExpand) {
            this.isExpand = isExpand;
        }
    }
}
