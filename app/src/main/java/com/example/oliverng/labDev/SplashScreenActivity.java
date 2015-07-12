package com.example.oliverng.labDev;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

public class SplashScreenActivity extends Activity {

    ViewGroup mContainer;
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;
    private static long ANIMATION_DURATION = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        setContentView(R.layout.activity_splash_screen);
        mContainer = (ViewGroup) findViewById(R.id.container);
        mContainer.animate().alpha(1).setDuration(SPLASH_TIME_OUT).
                withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mContainer.animate().scaleX(5).scaleY(5).alpha(0).setDuration(ANIMATION_DURATION).
                                setInterpolator(new AccelerateInterpolator()).
                                withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent i = new Intent(SplashScreenActivity.this, MainActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                });
                    }
                });
    }

}
