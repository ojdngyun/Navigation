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
import android.widget.Toast;

import com.example.oliverng.labDev.mapper.MapLoader;
import com.example.oliverng.labDev.mapper.MapView;
import com.example.oliverng.labDev.mapper.NavigationalMap;
import com.example.oliverng.labDev.mapper.PositionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * A placeholder fragment containing a simple view.
 */
public class Lab4ActivityFragment extends Fragment {

    private static final HashMap<Integer, String> maps = new HashMap<>();

    Boolean isOriginDestinationValid = false;
    PointF nextOrigin;
    PointF nextDestination;
    PointF origin;
    PointF destination;
    NavigationalMap mNavigationalMap = new NavigationalMap();
    PointF startingNode;
    PointF endingNode;

    PointF[] route1;
    PointF[] route2;
    PointF[] route3;
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

        //initialization of hashMap of map
        maps.put(0, "E2-3344.svg");
        maps.put(1, "Lab-room-peninsula.svg");
        maps.put(2, "Lab-room.svg");

        //initialization of checkpoints
        route1 = addCheckPoints(CoordinatesConstant.route1X, CoordinatesConstant.route1Y);
        route2 = addCheckPoints(CoordinatesConstant.route2X, CoordinatesConstant.route2Y);
        route3 = addCheckPoints(CoordinatesConstant.route3X, CoordinatesConstant.route3Y);

        //gets the screen width so that the graph adjusts on screen rotation
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mWidth = size.x;

        //mapView initialization
        mapView = new MapView(getActivity(), 1080, 1080, 42, 42);
        registerForContextMenu(mapView);
        mapView.setMap(loadMap(maps.get(0)));
        mapView.addListener(new PositionListener() {
            @Override
            public void originChanged(MapView source, PointF loc) {
                nextOrigin = loc;
                isOriginDestinationValid = checkOriginDestination();
                //Toast.makeText(getActivity(), "x: " + loc.x + " y: " + loc.y, Toast.LENGTH_LONG).show();
                //Toast.makeText(getActivity(), list.size() + "", Toast.LENGTH_LONG).show();
                //findPath();
            }

            @Override
            public void destinationChanged(MapView source, PointF dest) {
                nextDestination = dest;
                isOriginDestinationValid = checkOriginDestination();
                //findPath();
            }
        });

        sensorManager = (SensorManager) rootView.getContext().getSystemService(Context.SENSOR_SERVICE);

        final RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.layout);
        TextView stepCount = (TextView) rootView.findViewById(R.id.countData);
        TextView displacement = (TextView) rootView.findViewById(R.id.displacement);
        TextView bearing = (TextView) rootView.findViewById(R.id.tvHeading);
        ImageView compass = (ImageView) rootView.findViewById(R.id.compassViewActual);
        ImageView heading = (ImageView) rootView.findViewById(R.id.compassViewDirection);

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
                compass,
                heading,
                mapView,
                mNavigationalMap);

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
                layout.getLayoutParams().height = (int) dimension[1];
                layout.getLayoutParams().width = (int) dimension[0];
                layout.requestLayout();
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

    private boolean checkOriginDestination(){
        if(nextOrigin == null || nextDestination == null) return false;
        else{
            if(findRoute(nextOrigin, nextDestination) == 4){
                Toast.makeText(getActivity(), "Something is WRONG!", Toast.LENGTH_LONG).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle("Error!")
                        .setMessage("Choose another origin or destination point!")
                        .setPositiveButton("OK", null);
                AlertDialog dialog = builder.create();
                dialog.show();
                sensorListener.pathNotValid();
                return false;
            }else{
                origin = new PointF(nextOrigin.x, nextOrigin.y);
                destination = new PointF(nextDestination.x, nextDestination.y);
                findPath();
                return true;
            }
        }
    }

    //get the path from the origin and destination
    private boolean findPath() {
        ArrayList<PointF> path = new ArrayList<PointF>();
        if(origin == null || destination == null)return false;
        else {
            if(mNavigationalMap.calculateIntersections(origin, destination).isEmpty()){
                path.add(origin);
                path.add(destination);
                mapView.setUserPath(path);
                sensorListener.setPath(path);
                return true;
            }else{
                switch(findRoute(origin, destination)){
                    case 1:
                        path = getPath(route1);
                        mapView.setUserPath(path);
                        sensorListener.setPath(path);
                        break;
                    case 2:
                        path = getPath(route2);
                        mapView.setUserPath(path);
                        sensorListener.setPath(path);
                        break;
                    case 3:
                        path = getPath(route3);
                        mapView.setUserPath(path);
                        sensorListener.setPath(path);
                        break;
                    default:
                        Toast.makeText(getActivity(), "Choose another origin or destination", Toast.LENGTH_SHORT).show();
                }
            }

        }
        return false;
    }

    //get the path in route
    private ArrayList<PointF> getPath(PointF[] points){
        ArrayList<PointF> path = new ArrayList<PointF>();
        int startingIndex = getIndex(points, startingNode);
        int endingIndex = getIndex(points, endingNode);
        path.add(origin);
        if(startingIndex < endingIndex) {
            for (int i = startingIndex; i <= endingIndex; i++) {
                path.add(points[i]);
            }
        }else{
            for(int i = startingIndex; i >= endingIndex; i--){
                path.add(points[i]);
            }
        }
        path.add(destination);
        return path;
    }

    //gets the index of the PointF in an array of PointF
    private int getIndex(PointF[] points, PointF point){
        for(int i = 0; i < points.length; i++){
            if(point.x == points[i].x && point.y == points[i].y){
                return i;
            }
        }
        return 0;
    }

    //finds the route and the starting and ending node in the route
    private int findRoute(PointF origin, PointF destination){
        PointF originRoute1 = findClosestNode(origin, route1);
        PointF originRoute2 = findClosestNode(origin, route2);
        PointF originRoute3 = findClosestNode(origin, route3);
        PointF destinationRoute1 = findClosestNode(destination, route1);
        PointF destinationRoute2 = findClosestNode(destination, route2);
        PointF destinationRoute3 = findClosestNode(destination, route3);
        if(mNavigationalMap.calculateIntersections(origin, originRoute1).isEmpty() &&
                mNavigationalMap.calculateIntersections(destination, destinationRoute1).isEmpty()){
            startingNode = originRoute1;
            endingNode = destinationRoute1;
            return 1;
        }else if(mNavigationalMap.calculateIntersections(origin, originRoute2).isEmpty() &&
                mNavigationalMap.calculateIntersections(destination, destinationRoute2).isEmpty()){
            startingNode = originRoute2;
            endingNode = destinationRoute2;
            return 2;
        }else if(mNavigationalMap.calculateIntersections(origin, originRoute3).isEmpty() &&
                mNavigationalMap.calculateIntersections(destination, destinationRoute3).isEmpty()){
            startingNode = originRoute3;
            endingNode = destinationRoute3;
            return 3;
        }else{
            return 4;
        }
    }

    //finds the closest node to point in array of nodes
    private PointF findClosestNode(PointF point, PointF[] points){
        PointF currentPoint = points[1];
        for(int i = 0; i < points.length; i++){
            if(distanceBetweenPoints(point, currentPoint) > distanceBetweenPoints(point, points[i])){
                currentPoint = points[i];
            }
        }
        return currentPoint;
    }

    //finds the distance between 2 PointF
    private double distanceBetweenPoints(PointF one, PointF two){
        return Math.sqrt(Math.pow((one.x - two.x), 2) + Math.pow((one.y - two.y), 2));
    }



    //used for the initialization of the
    private PointF[] addCheckPoints(int[] routeX, int[] routeY) {
        int length = routeX.length;
        PointF[] points = new PointF[length];
        for(int i = 0; i < length; i++) {
            points[i] = new PointF(routeX[i], routeY[i]);
        }
        return points;
    }

    private NavigationalMap loadMap(String s) {
        mNavigationalMap = MapLoader.loadMap(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), s);
        return mNavigationalMap;
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
                        //mapView.setMap(loadMap(maps.get(1)));
//                        mapView.changeScale(new PointF(100, 100));
                        break;
                    case 2:
                        //mapView.setMap(loadMap(maps.get(2)));
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
