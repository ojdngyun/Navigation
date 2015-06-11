package com.example.oliverng.labDev;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity {

    public static final String PREFERENCES_Z_MAX = "PREFERENCES_Z_MAX";
    public static final String PREFERENCES_Z_MIN = "PREFERENCES_Z_MIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        SensorManager sensorManager;
        Sensor accelerometerSensor;
        sensorEventListener accListener;
//        TextView absAccelerometerData, stepCount;
        LineGraphView graphView;
        int mWidth;


        public PlaceholderFragment() {
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            sensorManager = (SensorManager) rootView.getContext().getSystemService(SENSOR_SERVICE);
            LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.layout);

//            graphView = (LineGraphView) rootView.findViewById(R.id.linegraph);
            TextView stepCount = (TextView) rootView.findViewById(R.id.countData);

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mWidth = size.x;

            graphView = new LineGraphView(getActivity(), 100, Arrays.asList("X", "Y", "Z"), mWidth);

            layout.addView(graphView);
            graphView.setVisibility(View.VISIBLE);

            SharedPreferences preferences = rootView.getContext().getSharedPreferences("CON", Context.MODE_PRIVATE);
            int zMax = preferences.getInt(PREFERENCES_Z_MAX, 10);
            int zMin = preferences.getInt(PREFERENCES_Z_MIN, 10);

            //accelerometer sensor
//            TextView absAccelerometerTitle = new TextView(rootView.getContext());
//            absAccelerometerTitle.setText("---Record Acceleration(linear)(m/s^2)---");
//            absAccelerometerData = new TextView(rootView.getContext());
//            TextView accelerometerTitle = new TextView(rootView.getContext());
//            accelerometerTitle.setText("---Acceleration(linear)(m/s^2)---");
//            TextView stepCount = new TextView(rootView.getContext());
//            stepCount.setText("321546");

            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//            accListener = new sensorEventListener(accelerometerData, absAccelerometerData, graphView);
            accListener = new sensorEventListener(stepCount, graphView, rootView.getContext(), zMax, zMin);
            sensorManager.registerListener(accListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);

//            layout.addView(absAccelerometerTitle);
//            layout.addView(absAccelerometerData);
//            layout.addView(accelerometerTitle);
//            layout.addView(accelerometerData);
//            layout.addView(stepCount);

            layout.setOnTouchListener(new OnDoubleTapListener(rootView.getContext()){
                @Override
                public void onDoubleClick() {
                    reset();
                }
            });

            return rootView;
        }

        private void reset() {
            accListener.resetAbsValues();
            graphView.purge();
        }

        @Override
        public void onResume() {
            super.onResume();
            sensorManager.registerListener(accListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        @Override
        public void onPause() {
            super.onPause();
            sensorManager.unregisterListener(accListener, accelerometerSensor);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fragment_menu, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if(item.getItemId() == R.id.refresh){
                show();
//                accListener.resetAbsValues();
//                graphView.purge();

            }
//            else if(item.getItemId() == R.id.stop){
////                accListener.stopGraph();
//            }

            return super.onOptionsItemSelected(item);
        }

        public void show(){
//            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            builder.setView(R.layout.dialog);
//            Button setButton = (Button) builder.findView
//
//            AlertDialog dialog = builder.create();
//            dialog.show();
            final SharedPreferences preferences = getActivity().getSharedPreferences("CON", Context.MODE_PRIVATE);
            int zMax = preferences.getInt(MainActivity.PREFERENCES_Z_MAX, 10);
            int zMin = preferences.getInt(MainActivity.PREFERENCES_Z_MIN, 10);
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.dialog);
            Button setButton = (Button) dialog.findViewById(R.id.setButton);
            Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
            final NumberPicker numberPickerZMax = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
            final NumberPicker numberPickerZMin = (NumberPicker) dialog.findViewById(R.id.numberPicker2);
            numberPickerZMax.setMaxValue(20);
            numberPickerZMax.setMinValue(0);
            numberPickerZMin.setMaxValue(20);
            numberPickerZMin.setMinValue(0);
            numberPickerZMax.setValue(zMax);
            numberPickerZMin.setValue(zMin);
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(MainActivity.PREFERENCES_Z_MAX, numberPickerZMax.getValue());
                    editor.putInt(MainActivity.PREFERENCES_Z_MIN, numberPickerZMin.getValue());
//                    Toast.makeText(getActivity(), "Zmax: " + numberPickerZMax.getValue() + "Zmin: " + numberPickerZMin.getValue() + " Saved",
//                            Toast.LENGTH_LONG).show();
                    editor.commit();
                    reset();
                    dialog.dismiss();
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    }
}
class sensorEventListener implements SensorEventListener {
//    TextView output;
//    TextView absOutput;
//    LineGraphView mGraph;
//    Boolean addPoint = true;

    TextView stepCountTextview;
    LineGraphView graph;

    int state = 0;
    int stepCount = 0;
    double zMax = 1.8;
    double zMin = -1.2;

    float[] filteredData;

    float current = 0;
    float previous = 0;

    float absX = 0;
    float negAbsX = 0;
    float absY = 0;
    float absZ = 0;

    Context mContext;

//    public sensorEventListener(TextView outputView, TextView absOutputView, LineGraphView graph){
//        output = outputView;
//        absOutput = absOutputView;
//        mGraph = graph;
//    }

    public sensorEventListener(TextView stepCountTextView, LineGraphView graph, Context context, float zMax, float zMin){
        this.stepCountTextview = stepCountTextView;
        this.graph = graph;
        this.zMax = zMax / 10;
        this.zMin =  -(zMin / 10);
        this.mContext = context;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
//            if(Math.abs(absX) < Math.abs(sensorEvent.values[0])){
//                absX = sensorEvent.values[0];
//            }if(Math.abs(absY) < Math.abs(sensorEvent.values[1])){
//                absY = sensorEvent.values[1];
//            }if(Math.abs(absZ) < Math.abs(sensorEvent.values[2])){
//                absZ = sensorEvent.values[2];
//            }
//        if(absX < sensorEvent.values[0]){
//            absX = sensorEvent.values[0];
//        }if(sensorEvent.values[0] < 0){
//            if(negAbsX > sensorEvent.values[0]){
//                negAbsX = sensorEvent.values[0];
//            }
//        }
//            output.setText("\nX: " + sensorEvent.values[0] + "\nY: " + sensorEvent.values[1] + "\nZ: " + sensorEvent.values[2] +"\n\n");
//            absOutput.setText("\nX: " + absX + "\nY: " + absY + "\nZ: " + absZ +"\n\n");
//        absOutput.setText("\n X: " + absX + "\n-X: " + negAbsX );

        //if(addPoint) {
            filteredData = lowpassFilter(sensorEvent.values, filteredData);
            graph.addPoint(filteredData);
            countStep(filteredData);
            stepCountTextview.setText(stepCount + "");
        //}
//            mGraph.addPoint(sensorEvent.values);
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
        absX = 0;
        negAbsX = 0;
        absY = 0;
        absZ = 0;
        stepCount = 0;

        SharedPreferences preferences = mContext.getSharedPreferences("CON", Context.MODE_PRIVATE);
        zMax = (double) preferences.getInt(MainActivity.PREFERENCES_Z_MAX, 10) / 10;
        zMin = -((double) preferences.getInt(MainActivity.PREFERENCES_Z_MIN, 10) / 10);

        Toast.makeText(mContext, "Zmax: " + zMax + " Zmin: " + zMin + " Loaded", Toast.LENGTH_SHORT).show();
    }

//    public void stopGraph(){
//        if(addPoint == false){ addPoint = true;}
//        else{addPoint = false;}
//    }

    public void countStep(float[] data){
        previous = current;
        current = data[2];
        switch(state) {
            case 0:
                if(current < previous) state = 1;
                Log.v("app", "case 0");
                break;
            case 1:
                if(current > previous){
                    if(current < zMin) state = 2;
                    else state = 0;
                }
                Log.v("app", "case 1");
                break;
            case 2:
                if(current < previous){
                    if(current > zMax) state = 3;
                    else state = 0;
                }
                Log.v("app", "case 2");
                break;
            case 3:
                if(current < 1) {
                    state = 0;
                    stepCount++;
                }
                Log.v("app", "case 3");
                break;
        }
    }
}
