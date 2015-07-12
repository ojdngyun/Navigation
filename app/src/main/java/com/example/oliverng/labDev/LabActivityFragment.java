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
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.oliverng.labDev.mapper.MapLoader;
import com.example.oliverng.labDev.mapper.MapView;
import com.example.oliverng.labDev.mapper.NavigationalMap;

import java.util.Arrays;


/**
 * A placeholder fragment containing a simple view.
 */
public class LabActivityFragment extends Fragment {

    private static String E2 = "E2-3344.svg";

    MapView mapView;
    SensorManager sensorManager;
    Sensor linearAccelerationSensor, accelerometerSensor, magneticSensor;
    lab3SensorEventListener sensorListener;
    LineGraphView graphView;
    int mWidth;

    public LabActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lab, container, false);

        //mapView initialization /storage/emulated/0/Android/data/com.example.oliverng.labDev/files
        mapView = new MapView(getActivity(), 1080, 1080, 30, 30);
        registerForContextMenu(mapView);
        NavigationalMap map = MapLoader.loadMap(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), E2);
        mapView.setMap(map);

        sensorManager = (SensorManager) rootView.getContext().getSystemService(Context.SENSOR_SERVICE);
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.layout);

        TextView stepCount = (TextView) rootView.findViewById(R.id.countData);
        TextView displacement = (TextView) rootView.findViewById(R.id.displacement);
        TextView bearing = (TextView) rootView.findViewById(R.id.tvHeading);
        ImageView compass = (ImageView) rootView.findViewById(R.id.compassViewActual);

        //gets the screen width so that the graph adjusts on screen rotation
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;

        graphView = new LineGraphView(getActivity(), 100, Arrays.asList("X", "Y", "Z"), mWidth);

        layout.addView(graphView);
        graphView.setVisibility(View.VISIBLE);

        layout.addView(mapView);

        //retrieving values for the state machine thresholds
        SharedPreferences preferences = rootView.getContext().getSharedPreferences("CON", Context.MODE_PRIVATE);
        int zMax = preferences.getInt(Lab2Activity.PREFERENCES_Z_MAX, 10);
        int zMin = preferences.getInt(Lab2Activity.PREFERENCES_Z_MIN, 10);
        int lXAcceleration = preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_X_ACCELERATION, 10);
        int lYAcceleration = preferences.getInt(Lab2Activity.PREFERENCES_LINEAR_Y_ACCELERATION, 10);
        int stepInMeters = preferences.getInt(Lab2Activity.PREFERENCES_STEP_RATIO, 10);

        //rotation sensor
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //magnetic sensor
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        //linearAcceleration sensor
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        //registering sensors
        sensorListener = new lab3SensorEventListener(stepCount,
                displacement,
                graphView,
                rootView.getContext(),
                zMax,
                zMin,
                lXAcceleration,
                lYAcceleration,
                stepInMeters,
                bearing,
                compass);

        sensorManager.registerListener(sensorListener,accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_GAME);


        //resets the step counter and retrieve threshold values from memory
        ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);
        scrollView.setOnTouchListener(new OnDoubleTapListener(rootView.getContext()){
            @Override
            public void onDoubleClick() {
                reset();
            }
        });



        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mapView.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item) || mapView.onContextItemSelected(item);
    }

    private void reset() {
        sensorListener.resetAbsValues();
        graphView.purge();
    }

    @Override
    public void onResume() {
        super.onResume();

        sensorManager.registerListener(sensorListener,accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener, accelerometerSensor);
        sensorManager.unregisterListener(sensorListener, magneticSensor);
        sensorManager.unregisterListener(sensorListener, linearAccelerationSensor);
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
            builder.setItems(R.array.options, mDialogListener);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
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
                case 2:
                    showDisplacementOptions();
                    break;
            }
        }
    };

    //dialog interface for the displacement measurement
    public void showDisplacementOptions(){
        final SharedPreferences preferences = getActivity().getSharedPreferences("CON", Context.MODE_PRIVATE);
        int stepRatio = preferences.getInt(Lab2Activity.PREFERENCES_STEP_RATIO, 10);
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_displacement);
        Button setButton = (Button) dialog.findViewById(R.id.setButton);
        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        final NumberPicker numberPickerStep = (NumberPicker) dialog.findViewById(R.id.numberPicker2);
        numberPickerStep.setMaxValue(40);
        numberPickerStep.setMinValue(0);
        numberPickerStep.setValue(stepRatio);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(Lab2Activity.PREFERENCES_STEP_RATIO, numberPickerStep.getValue());
                editor.apply();
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

    //dialog interface for the state machine's linear motion x and y
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
                editor.apply();
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

    //dialog interface for the state machine's z threshold
    public void showZThreshold(){
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
                editor.apply();
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
