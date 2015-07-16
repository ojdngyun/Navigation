package com.example.oliverng.labDev;

import android.animation.Animator;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.transition.Explode;
import android.transition.Transition;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;


public class MainActivity extends ActionBarActivity {

    Button lab1Button, lab2Button, lab3Button, lab4Button;
    private Transition.TransitionListener mEnterTransitionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mEnterTransitionListener = new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                enterReveal();
            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        };
        getWindow().getEnterTransition().addListener(mEnterTransitionListener);
        setContentView(R.layout.activity_main);
        lab1Button = (Button) findViewById(R.id.lab1Button);
        lab2Button = (Button) findViewById(R.id.lab2Button);
        lab3Button = (Button) findViewById(R.id.lab3button);
        lab4Button = (Button) findViewById(R.id.lab4Button);

        lab1Button.setOnClickListener(mOnClickListener);
        lab2Button.setOnClickListener(mOnClickListener);
        lab3Button.setOnClickListener(mOnClickListener);
        lab4Button.setOnClickListener(mOnClickListener);
    }

    private void enterReveal() {
        //previously invisible view
        final View myView = findViewById(R.id.lab3button);

        //get the center for the clipping circle
        int cx = myView.getMeasuredWidth() / 2;
        int cy = myView.getMeasuredHeight() / 2;

        //get the final radius for the clipping circle
        int finalradius = Math.max(myView.getWidth(), myView.getHeight()) / 2;
        Animator anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalradius);
        myView.setVisibility(View.VISIBLE);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                getWindow().getEnterTransition().removeListener(mEnterTransitionListener);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        anim.start();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getWindow().setExitTransition(new Explode());
            Intent intent;
            if(view.getId() == R.id.lab1Button){
                intent = new Intent(MainActivity.this, ActivityTest.class);
                startActivity(intent,
                        ActivityOptions
                                .makeSceneTransitionAnimation(MainActivity.this).toBundle());
            }else if(view.getId() == R.id.lab2Button){
                intent = new Intent(MainActivity.this, Lab2Activity.class);
                startActivity(intent,
                        ActivityOptions
                                .makeSceneTransitionAnimation(MainActivity.this).toBundle());
            }else if(view.getId() == R.id.lab3button){
                intent = new Intent(MainActivity.this, LabActivity.class);
                startActivity(intent,
                        ActivityOptions
                                .makeSceneTransitionAnimation(MainActivity.this).toBundle());
            }else if(view.getId() == R.id.lab4Button){
                intent = new Intent(MainActivity.this, Lab4Activity.class);
                startActivity(intent,
                        ActivityOptions
                                .makeSceneTransitionAnimation(MainActivity.this).toBundle());
            }
        }
    };

    private void startAct(Class cls, View view){
        Intent intent = new Intent(MainActivity.this, cls);
        Bundle b = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            b = ActivityOptions.makeScaleUpAnimation(view, 1, 1, view.getWidth(),
                    view.getHeight()).toBundle();
        }
        startActivity(intent, b);
    }
}
