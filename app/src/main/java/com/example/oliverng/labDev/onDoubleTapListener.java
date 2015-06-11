package com.example.oliverng.labDev;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by OliverNg on 6/9/2015.
 */
public class OnDoubleTapListener implements View.OnTouchListener {

    private final GestureDetector mGestureDetector;

    public OnDoubleTapListener(Context context){
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return mGestureDetector.onTouchEvent(motionEvent);
    }

    public void onDoubleClick(){}

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener{

//        @Override
//        public boolean onDoubleTap(MotionEvent e) {
//            onDoubleClick();
//            return false;
//        }


        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            onDoubleClick();
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        //        @Override
//        public boolean onDoubleTapEvent(MotionEvent e) {
//            onDoubleClick();
//            return false;
//        }

//        @Override
//        public boolean onSingleTapUp(MotionEvent e) {
//            onDoubleClick();
//            return false;
//        }
    }
}
