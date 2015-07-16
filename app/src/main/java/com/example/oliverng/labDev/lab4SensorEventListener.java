package com.example.oliverng.labDev;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
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

import com.example.oliverng.labDev.mapper.MapView;
import com.example.oliverng.labDev.mapper.NavigationalMap;
import com.example.oliverng.labDev.mapper.VectorUtils;

import java.util.List;

class lab4SensorEventListener implements SensorEventListener {

    //mapView
    MapView mapView;
    NavigationalMap mNavigationalMap;
    List<PointF> path;
    int userIndex;
    boolean isValidPath = false;
    float angleToMove;

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
    ImageView heading;

    float degree;
    float currentDegree;

    float[] gravity;
    float[] geomagnetic;
    float compassAngle;
    float[] inclination = new float[9];
    float[] rotation = new float[9];
    float[] orientation = new float[3];
    float[] angle = new float[2];
    float[] angleHeading = new float[2];

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
                                   ImageView compass,
                                   ImageView heading,
                                   MapView mapView,
                                   NavigationalMap mNavigationalMap){
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
        this.heading = heading;

        this.mapView = mapView;
        this.mNavigationalMap = mNavigationalMap;
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
                correctHeadingAngle(degree, compassAngle);

                //animation for compass
                RotateAnimation animation = new RotateAnimation(
                        angle[0],
                        angle[1],
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                animation.setDuration(16);
                animation.setFillAfter(true);
                compass.startAnimation(animation);
                //
                if(isValidPath){
                    RotateAnimation anim = new RotateAnimation(
                            (360 - angleHeading[0]) - 180,
                            (360 - angleHeading[1]) - 180,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f
                    );
                    anim.setDuration(16);
                    anim.setFillAfter(true);
                    heading.startAnimation(anim);
                }

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
                    checkStep();
                }
                Log.v("app", "case 3");
                break;
        }
    }

    private void checkStep() {
        if(isValidPath){
            if(displacement()) {
                state = 0;
                stepCount++;
                calcAngle(path.get(userIndex), path.get(userIndex + 1));
                upDateUi();
                linearYAccelerationState = linearXAccelerationState = false;
            }
        }
    }

    //converts angles to degrees from zero to 360
    private void correctAngle(float one, float two){
        if(one < 0){
            angle[0] = (float) (360 - Math.toDegrees(2*Math.PI - Math.abs(one)));
        }else{
            angle[0] = (float) (360 - Math.toDegrees(one));
        }if(two < 0){
            angle[1] = (float) (360 - Math.toDegrees(2*Math.PI - Math.abs(two)));
        }else{
            angle[1] = (float) (360 - Math.toDegrees(two));
        }
    }

    //converts angles to degrees from zero to 360
    private void correctHeadingAngle(float one, float two){
        if(one < 0){
            angleHeading[0] = (float) ((360 - Math.toDegrees(2*Math.PI - Math.abs(one))) - angleToMove);
        }else{
            angleHeading[0] = (float) ((360 - Math.toDegrees(one)) - angleToMove);
        }if(two < 0){
            angleHeading[1] = (float) ((30 - Math.toDegrees(2*Math.PI - Math.abs(two))) - angleToMove);
        }else{
            angleHeading[1] = (float) ((360 - Math.toDegrees(two)) - angleToMove);
        }
    }

    private boolean displacement(){
        correctedAngle = (float) Math.toRadians(90) - degree;
        double Ndistance = Math.round(Math.sin(correctedAngle) * 100);
        double Edistance = Math.round(Math.cos(correctedAngle) * 100);
        PointF p = new PointF(path.get(userIndex).x, path.get(userIndex).y);
        if(checkPoints(p.x += (float) (stepInMeters * (Edistance / 100)),
                p.y += -((float) ((stepInMeters) * (Ndistance / 100))),
                path.get(userIndex + 1).x, path.get(userIndex + 1).y)) {
            northDistance += stepInMeters * (Ndistance / 100);
            eastDistance += stepInMeters * (Edistance / 100);
            path.get(userIndex).x += (float) (stepInMeters * (Edistance / 100));
            path.get(userIndex).y += -((float) ((stepInMeters) * (Ndistance / 100)));
            mapView.setUserPoint(path.get(userIndex).x, path.get(userIndex).y);
            if(VectorUtils.distance(path.get(userIndex), path.get(userIndex + 1)) < 0.5){
                if((path.size() - userIndex) == 2){
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                            .setTitle("Announcement to the User!")
                            .setMessage("Destination Reached!")
                            .setPositiveButton("OK", null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                    isValidPath = false;
                }else {
                    userIndex++;
                }
            }
            return true;
        }else return false;
    }

    private boolean checkPoints(float x1, float y1, float x2, float y2){
        if(mNavigationalMap.calculateIntersections(new PointF(x1, y1), new PointF(x2, y2)).isEmpty()){
            return true;
        }else return false;
    }

    private void upDateUi(){
        displacement.setText("North: " + String.format( "%.2f", northDistance ) + "m East: " + String.format( "%.2f", eastDistance ) + "m");
        stepCountTextview.setText(stepCount + "");
    }

    public void setPath(List<PointF> path){
        calcAngle(path.get(0), path.get(1));
        Toast.makeText(mContext, angleToMove + "", Toast.LENGTH_SHORT).show();
        isValidPath = true;
        userIndex = 0;
        this.path = path;
    }

    private void calcAngle(PointF start, PointF end2){
        PointF north = new PointF(start.x, start.y);
        north.y = north.y - 2;
        angleToMove =  (float) Math.toDegrees(VectorUtils.angleBetween(start, north, end2));
    }

    public void pathNotValid(){
        isValidPath = false;
    }

}