package com.example.oliverng.labDev;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

class lab4SensorEventListener implements SensorEventListener {

    //accelerometer sensor
    Context mContext;
    TextView stepCountTextview;
    LineGraphView graph;
    TextView displacement;

    //state machine
    int state = 0;
    boolean linearXAccelerationState = false;
    boolean linearYAccelerationState = false;
    int stepCount = 0;
    double zMax;
    double zMin;
    double lXAccelerationThreshold;
    double lYAccelerationThreshold;

    //displacement
    double northDistance = 0;
    double eastDistance = 0;
    float correctedAngle;
    double stepInMeters = 1;

    //lowpass filter
    float[] filteredData;
    float currentZ = 0;
    float previousZ = 0;

    //Compass
    TextView bearing;
    ImageView compass;

    float degree;
    float currentDegree;

    float[] gravity;
    float[] geomagnetic;
    float compassAngle;
    float[] inclination = new float[9];
    float[] rotation = new float[9];
    float[] orientation = new float[3];
    float[] angle = new float[2];

    public lab4SensorEventListener(TextView stepCountTextView,
                                   TextView displacement,
                                   LineGraphView graph,
                                   Context context,
                                   int zMax,
                                   int zMin,
                                   int lXAcceleration,
                                   int lYAcceleration,
                                   int stepInMeters,
                                   TextView bearing,
                                   ImageView compass){
        this.stepCountTextview = stepCountTextView;
        this.displacement = displacement;
        this.graph = graph;
        this.zMax = ((double) zMax / 10);
        this.zMin =  (-((double) zMin / 10));
        this.lXAccelerationThreshold = ((double) lXAcceleration / 10);
        this.lYAccelerationThreshold = ((double) lYAcceleration / 10);
        this.stepInMeters = (double) stepInMeters / 10;
        this.mContext = context;

        this.bearing = bearing;
        this.compass = compass;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            gravity = sensorEvent.values;
        }else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            geomagnetic = sensorEvent.values;
        }if( gravity != null && geomagnetic != null) {
            if (SensorManager.getRotationMatrix(rotation, inclination, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotation, orientation);
                degree = orientation[0];
                bearing.setText("Heading: " + Math.round(Math.toDegrees(degree)) + " degrees");

                correctAngle(degree, compassAngle);
                RotateAnimation animation = new RotateAnimation(
                        360-angle[0],
                        360-angle[1],
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );

                animation.setDuration(16);
                animation.setFillAfter(true);

                compass.startAnimation(animation);
                compassAngle = degree;
            }
        }
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            filteredData = lowpassFilter(sensorEvent.values, filteredData);
//            graph.addPoint(filteredData);
            countStep(filteredData);
        }
//        else if(sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION){
//            degree = sensorEvent.values[0];
//
//            bearing.setText("Heading: " + Math.round(degree) + " degrees");
//            RotateAnimation animation = new RotateAnimation(
//                    currentDegree,
//                    360-degree,
//                    Animation.RELATIVE_TO_SELF, 0.5f,
//                    Animation.RELATIVE_TO_SELF, 0.5f
//            );
//
//            animation.setDuration(16);
//            animation.setFillAfter(true);
//
//            compass.startAnimation(animation);
//            currentDegree = 360-degree;
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private float[] lowpassFilter(float[] newValues, float[] currentValues){
        if(currentValues == null){
            currentValues = new float[3];
            for(int i = 0; i < 3; i++){
                currentValues[i] = 0;
            }
        }else{
            for(int i = 0; i < 3; i++) {
                currentValues[i] += (newValues[i] - currentValues[i]) / 10;
            }
        }
        return currentValues;
    }

    public void resetAbsValues(){
        stepCount = 0;
        northDistance = 0;
        eastDistance = 0;

        SharedPreferences preferences = mContext.getSharedPreferences("CON", Context.MODE_PRIVATE);
        zMax = (double) preferences.getInt(Lab2Activity.PREFERENCES_Z_MAX, 10) / 10;
        zMin = -((double) preferences.getInt(Lab2Activity.PREFERENCES_Z_MIN, 10) / 10);
        Log.d("reset", zMax + " " + zMin);
        lXAccelerationThreshold = (double) preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_X_ACCELERATION, 10) / 10;
        lYAccelerationThreshold = (double) preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_Y_ACCELERATION, 10) / 10;
        stepInMeters = (double) preferences.getInt(Lab2Activity.PREFERENCES_STEP_RATIO, 10) / 10;

        stepCountTextview.setText(0 + "");
        displacement.setText("North: 0.0m East: 0.0m");

        Toast.makeText(mContext, "Zmax: " + zMax + " Zmin: " + zMin +
                "\nLinearX: " + lXAccelerationThreshold + " LinearY: " + lYAccelerationThreshold +
                "\nStep Ratio: " + stepInMeters, Toast.LENGTH_SHORT).show();
    }

    private void countStep(float[] data){
        previousZ = currentZ;
        currentZ = data[2];
        if(Math.abs(data[0]) > lXAccelerationThreshold){
            linearXAccelerationState = true;
        }if(Math.abs(data[1]) > lYAccelerationThreshold){
            linearYAccelerationState = true;
        }
        switch(state) {
            case 0://stable state
                if(currentZ < previousZ) state = 1;
                Log.v("app", "case 0");
                break;
            case 1://drop state
                if(currentZ > previousZ){
                    if(currentZ < zMin) state = 2;
                    else state = 0;
                }
                Log.v("app", "case 1");
                break;
            case 2://rise state
                if(currentZ < previousZ){
                    if(currentZ > zMax) state = 3;
                    else state = 0;
                }
                Log.v("app", "case 2");
                break;
            case 3://drop state
                if(currentZ < 0.5 && linearYAccelerationState && linearXAccelerationState) {
                    state = 0;
                    stepCount++;
                    displacement();
                    upDateUi();
                    linearYAccelerationState = linearXAccelerationState = false;
                }
                Log.v("app", "case 3");
                break;
        }
    }

    private void correctAngle(float one, float two){
        if(one < 0){
            angle[0] = (float) Math.toDegrees(2*Math.PI - Math.abs(one));
        }else{
            angle[0] = (float) Math.toDegrees(one);
        }if(two < 0){
            angle[1] = (float) Math.toDegrees(2*Math.PI - Math.abs(two));
        }else{
            angle[1] = (float) Math.toDegrees(two);
        }
    }

    private void displacement(){
        correctedAngle = (float) Math.toRadians(90) - degree;
        double Ndistance = Math.round(Math.sin(correctedAngle) * 100);
        double Edistance = Math.round(Math.cos(correctedAngle) * 100);
        northDistance += stepInMeters * (Ndistance / 100);
        eastDistance += stepInMeters * (Edistance / 100);
    }

    public void upDateUi(){
        displacement.setText("North: " + String.format( "%.2f", northDistance ) + "m East: " + String.format( "%.2f", eastDistance ) + "m");
        stepCountTextview.setText(stepCount + "");
    }

}