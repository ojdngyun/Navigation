package com.example.oliverng.labDev;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
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

import java.util.Arrays;


public class Lab2Activity extends ActionBarActivity {

    public static final String PREFERENCES_Z_MAX = "PREFERENCES_Z_MAX";
    public static final String PREFERENCES_Z_MIN = "PREFERENCES_Z_MIN";
    public static final String PREFERENCES_LINEAR_X_ACCELERATION = "LINEAR_ACCELERATION_X";
    public static final String PREFERENCES_LINEAR_Y_ACCELERATION = "LINEAR_ACCELERATION_Y";
    public static final String PREFERENCES_STEP_RATIO = "STEP_RATIO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab2);
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
        accelerometerListener accListener;
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
            View rootView = inflater.inflate(R.layout.fragment_lab2, container, false);
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
            int lXAcceleration = preferences.getInt(PREFERENCES_LINEAR_X_ACCELERATION, 10);
            int lYAcceleration = preferences.getInt(PREFERENCES_LINEAR_Y_ACCELERATION, 10);

            //accelerometer sensor
//            TextView absAccelerometerTitle = new TextView(rootView.getContext());
//            absAccelerometerTitle.setText("---Record Acceleration(linear)(m/s^2)---");
//            absAccelerometerData = new TextView(rootView.getContext());
//            TextView accelerometerTitle = new TextView(rootView.getContext());
//            accelerometerTitle.setText("---Acceleration(linear)(m/s^2)---");
//            TextView stepCount = new TextView(rootView.getContext());
//            stepCount.setText("321546");

            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//            sensorListener = new sensorEventListener(accelerometerData, absAccelerometerData, graphView);
            accListener = new accelerometerListener(stepCount, graphView, rootView.getContext(), zMax, zMin, lXAcceleration, lYAcceleration);
            sensorManager.registerListener(accListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);

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
            sensorManager.registerListener(accListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
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
                AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
                builder.setItems(R.array.options1, mDialogListener);
                AlertDialog dialog = builder.create();
                dialog.show();
//                sensorListener.resetAbsValues();
//                graphView.purge();

            }
//            else if(item.getItemId() == R.id.stop){
////                sensorListener.stopGraph();
//            }

            return super.onOptionsItemSelected(item);
        }

        protected DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showZThreshold();
                        break;
                    case 1:
                        showLinearThreshold();
                        break;
                }
            }
        };

        public void showLinearThreshold(){
            final SharedPreferences preferences = getActivity().getSharedPreferences("CON", Context.MODE_PRIVATE);
            int lXAcceleration = preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_X_ACCELERATION, 10);
            int lYAcceleration = preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_Y_ACCELERATION, 10);
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.dilaog_linear);
            Button setButton = (Button) dialog.findViewById(R.id.setButton);
            Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
            final NumberPicker numberPickerX = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
            final NumberPicker numberPickerY = (NumberPicker) dialog.findViewById(R.id.numberPicker2);
            numberPickerX.setMaxValue(40);
            numberPickerX.setMinValue(0);
            numberPickerY.setMaxValue(40);
            numberPickerY.setMinValue(0);
            numberPickerX.setValue(lXAcceleration);
            numberPickerY.setValue(lYAcceleration);
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(Lab2Activity.PREFERENCES_LINEAR_X_ACCELERATION, numberPickerX.getValue());
                    editor.putInt(Lab2Activity.PREFERENCES_LINEAR_Y_ACCELERATION, numberPickerY.getValue());
//                    Toast.makeText(getActivity(), "Zmax: " + numberPickerX.getValue() + "Zmin: " + numberPickerY.getValue() + " Saved",
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

        public void showZThreshold(){
//            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            builder.setView(R.layout.dialog);
//            Button setButton = (Button) builder.findView
//
//            AlertDialog dialog = builder.create();
//            dialog.show();
            final SharedPreferences preferences = getActivity().getSharedPreferences("CON", Context.MODE_PRIVATE);
            int zMax = preferences.getInt(Lab2Activity.PREFERENCES_Z_MAX, 10);
            int zMin = preferences.getInt(Lab2Activity.PREFERENCES_Z_MIN, 10);
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.dialog_z);
            Button setButton = (Button) dialog.findViewById(R.id.setButton);
            Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
            final NumberPicker numberPickerZMax = (NumberPicker) dialog.findViewById(R.id.numberPicker1);
            final NumberPicker numberPickerZMin = (NumberPicker) dialog.findViewById(R.id.numberPicker2);
            numberPickerZMax.setMaxValue(40);
            numberPickerZMax.setMinValue(0);
            numberPickerZMin.setMaxValue(40);
            numberPickerZMin.setMinValue(0);
            numberPickerZMax.setValue(zMax);
            numberPickerZMin.setValue(zMin);
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(Lab2Activity.PREFERENCES_Z_MAX, numberPickerZMax.getValue());
                    editor.putInt(Lab2Activity.PREFERENCES_Z_MIN, numberPickerZMin.getValue());
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
