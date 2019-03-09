package com.demo.ming.webview;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.umeng.analytics.MobclickAgent;

public class SplashActivity extends AppCompatActivity {

    private TimeCount timeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        timeCount=new TimeCount(2000,1000,this);
        timeCount.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        timeCount.cancel();
        super.onDestroy();
    }

    class TimeCount extends CountDownTimer {
        private Activity mActivity;
        public TimeCount(long millisInFuture, long countDownInterval, Activity activity) {
            super(millisInFuture, countDownInterval);
            mActivity=activity;
        }

        @Override
        public void onTick(long l) {
        }

        @Override
        public void onFinish() {
            mActivity.finish();
            mActivity.startActivity(new Intent(mActivity,MainActivity.class));
        }
    }
}
