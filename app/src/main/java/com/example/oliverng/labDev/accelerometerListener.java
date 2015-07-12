package com.example.oliverng.labDev;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

class accelerometerListener implements SensorEventListener {

    //accelerometer sensor
    Context mContext;
    TextView stepCountTextview;
    LineGraphView graph;

    //state machine
    int state = 0;
    boolean linearXAccelerationState = false;
    boolean linearYAccelerationState = false;
    int stepCount = 0;
    double zMax;
    double zMin;
    double lXAccelerationThreshold;
    double lYAccelerationThreshold;


    //lowpass filter
    float[] filteredData;
    float currentZ = 0;
    float previousZ = 0;

    //constructor for accelerometer sensor for lab 2
    public accelerometerListener(TextView stepCountTextView, LineGraphView graph, Context context, int zMax, int zMin, int lXAcceleration, int lYAcceleration){
        this.stepCountTextview = stepCountTextView;
        this.graph = graph;
        this.zMax = ((double) zMax / 10);
        this.zMin =  (-((double) zMin / 10));
        this.lXAccelerationThreshold = ((double) lXAcceleration / 10);
        this.lYAccelerationThreshold = ((double) lYAcceleration / 10);
        this.mContext = context;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
            filteredData = lowpassFilter(sensorEvent.values, filteredData);
            graph.addPoint(filteredData);
            countStep(filteredData);
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
        SharedPreferences preferences = mContext.getSharedPreferences("CON", Context.MODE_PRIVATE);
        zMax = (double) preferences.getInt(Lab2Activity.PREFERENCES_Z_MAX, 10) / 10;
        zMin = -((double) preferences.getInt(Lab2Activity.PREFERENCES_Z_MIN, 10) / 10);
        Log.d("reset", zMax + " " + zMin);
        lXAccelerationThreshold = (double) preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_X_ACCELERATION, 10) / 10;
        lYAccelerationThreshold = (double) preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_Y_ACCELERATION, 10) / 10;

        stepCountTextview.setText(0 + "");

        Toast.makeText(mContext, "Zmax: " + zMax + " Zmin: " + zMin +
                "\nLinearX: " + lXAccelerationThreshold + "LinearY: " + lYAccelerationThreshold, Toast.LENGTH_SHORT).show();
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
                if(currentZ < 1 && linearYAccelerationState && linearXAccelerationState) {
                    state = 0;
                    stepCount++;
                    upDateUi();
                    linearYAccelerationState = linearXAccelerationState = false;
                }
                Log.v("app", "case 3");
                break;
        }
    }

    public void upDateUi(){
        stepCountTextview.setText(stepCount + "");
    }
}