package com.example.oliverng.labDev;

import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.oliverng.labDev.mapper.VectorUtils;


public class ActivityTest extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_test);

        TextView text = (TextView) findViewById(R.id.textView3);
        text.setText(Math.toDegrees(VectorUtils.angleBetween(new PointF(360,360),new PointF(360,0), new PointF(345,0))) +"\n" +
                Math.toDegrees(VectorUtils.angleBetween(new PointF(0.360f,0.360f),new PointF(0.360f,0), new PointF(0,0.360f))) + "\n" +
                Math.toDegrees(VectorUtils.angleBetween(new PointF(360,360),new PointF(360,0), new PointF(360,720))) + "\n" +
                Math.toDegrees(VectorUtils.angleBetween(new PointF(1,0),new PointF(1,100), new PointF(-500,0))));

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.testLayout);
        layout.setOnTouchListener(new OnDoubleTapListener(this){
            @Override
            public void onDoubleClick() {
                Toast.makeText(ActivityTest.this, "Hello", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
