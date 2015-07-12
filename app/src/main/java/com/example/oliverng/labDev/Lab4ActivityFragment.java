package com.example.oliverng.labDev;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
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
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.oliverng.labDev.mapper.MapLoader;
import com.example.oliverng.labDev.mapper.MapView;
import com.example.oliverng.labDev.mapper.NavigationalMap;

import java.util.Arrays;
import java.util.HashMap;


/**
 * A placeholder fragment containing a simple view.
 */
public class Lab4ActivityFragment extends Fragment {

    private static HashMap<Integer, String> maps = new HashMap<>();

    MapView mapView;
    SensorManager sensorManager;
    Sensor linearAccelerationSensor, accelerometerSensor, magneticSensor;
    lab4SensorEventListener sensorListener;
    LineGraphView graphView;
    int mWidth;

    public Lab4ActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lab4, container, false);
        //initialization of hashMap
        maps.put(0, "E2-3344.svg");
        maps.put(1, "Lab-room-peninsula.svg");
        maps.put(2, "Lab-room.svg");

        //gets the screen width so that the graph adjusts on screen rotation
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;

        //mapView initialization /storage/emulated/0/Android/data/com.example.oliverng.labDev/files
        mapView = new MapView(getActivity(), 1080, 1080, 42, 42);
        registerForContextMenu(mapView);
        mapView.setMap(loadMap(maps.get(0)));

        sensorManager = (SensorManager) rootView.getContext().getSystemService(Context.SENSOR_SERVICE);
        RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.layout);

        TextView stepCount = (TextView) rootView.findViewById(R.id.countData);
        TextView displacement = (TextView) rootView.findViewById(R.id.displacement);
        TextView bearing = (TextView) rootView.findViewById(R.id.tvHeading);
        ImageView compass = (ImageView) rootView.findViewById(R.id.compassViewActual);

        graphView = new LineGraphView(getActivity(), 100, Arrays.asList("X", "Y", "Z"), mWidth);

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
        sensorListener = new lab4SensorEventListener(stepCount,
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
        RelativeLayout relativeLayout = (RelativeLayout) rootView.findViewById(R.id.relativeLayout);
        relativeLayout.setOnTouchListener(new OnDoubleTapListener(rootView.getContext()){
            @Override
            public void onDoubleClick() {
                reset();
            }
        });

        //seekBar for the zoom function
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.zoomSeekBar);
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float factor = (float) (100 + i) / 100;
                PointF point = mapView.getScale();
                point.x = 42 * factor;
                point.y = 42 * factor;
                float[] dimension = mapView.getDimension();
                dimension[0] = 1080 * factor;
                dimension[1] = 1080 * factor;
                mapView.changeScale(point, dimension);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return rootView;
    }

    private NavigationalMap loadMap(String s) {
        return MapLoader.loadMap(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), s);
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

        sensorManager.registerListener(sensorListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
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
            builder.setItems(R.array.optionsLab4, mDialogListener);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }


    //dialog application options
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
                case 3:
                    showMaps();
            }
        }
    };

    //dialog to load maps
    private void showMaps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.mapsLabel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case 0:
                        mapView.setMap(loadMap(maps.get(0)));
//                        mapView.changeScale(new PointF(80, 80));
                        break;
                    case 1:
                        mapView.setMap(loadMap(maps.get(1)));
//                        mapView.changeScale(new PointF(100, 100));
                        break;
                    case 2:
                        mapView.setMap(loadMap(maps.get(2)));
//                        mapView.changeScale(new PointF(100, 100));
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

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
