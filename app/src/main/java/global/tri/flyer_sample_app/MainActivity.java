package global.tri.flyer_sample_app;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private FlyerManagerThread flyerManagerThread;
    private boolean active;

    public boolean onTouch(View v, MotionEvent event) {
        FlyerManagerThread t = flyerManagerThread;

        if (t == null) return false;

        if ( (event.getAction()==MotionEvent.ACTION_DOWN) || (event.getAction()==MotionEvent.ACTION_MOVE) ) {

            t.setSpeedAndDirection((event.getY() - v.getHeight()/2)/(v.getHeight()/2), (event.getX() - v.getWidth()/2)/(v.getWidth()/2));
            //Log.e("Flyer","Move " + event.getX() + " " + event.getY());

            return true;
        }


        if (event.getAction()==MotionEvent.ACTION_UP){

            t.setSpeedAndDirection(0, 0);
            //Log.e("Flyer","Up");
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View rectangle = this.findViewById(R.id.control_rectangle);
        rectangle.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Shutdown previous instantiation of flyerManagerThread.
        synchronized (this) {
            if(flyerManagerThread != null) {
                flyerManagerThread.shutdown();
            }
            flyerManagerThread = null;
        }

        // Restart flyerManagerThread.
        try {
            flyerManagerThread = new FlyerManagerThread();
        } catch (FlyerManagerThread.FlyerManagerStartupException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Shutdown flyerManagerThread.
        synchronized (this) {
            if(flyerManagerThread != null) {
                flyerManagerThread.shutdown();
            }
            flyerManagerThread = null;
        }
    }


}
