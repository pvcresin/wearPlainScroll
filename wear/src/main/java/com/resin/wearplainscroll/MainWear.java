package com.resin.wearplainscroll;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.CardFrame;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainWear extends WearableActivity
        implements MessageApi.MessageListener {

    String Test = "test";
    long start, end;
    double time;
    int num = 5;

    boolean finished = false;

    ArrayList<CardFrame> cards = new ArrayList<>(20);

    String TAG = "Wear";

    GoogleApiClient mGoogleApiClient;
    String MESSAGE = "/message";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
        setAmbientEnabled();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Google Api Client connected");

                        Wearable.MessageApi.addListener(mGoogleApiClient, MainWear.this);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                }).build();


        final ScrollView scrollView = (ScrollView)findViewById(R.id.scroll_view);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                num += 5;
                if (num == 20) num = 5;

                Toast.makeText(MainWear.this, "num: " + num, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start = System.currentTimeMillis();
                Toast.makeText(MainWear.this, "start !!", Toast.LENGTH_LONG).show();
                Log.d(Test, "start: " + start);
                finished = false;
            }
        });

        LinearLayout container = (LinearLayout)findViewById(R.id.container);

        for (int i = 0; i < 20; i++) {
            WearableListView.ViewHolder viewHolder = new WearableListView.ViewHolder(
                    this.getLayoutInflater().inflate(R.layout.list_view_item, new ViewGroup(this) {
                        @Override
                        protected void onLayout(boolean changed, int l, int t, int r, int b) {

                        }
                    }, false)
            );

            TextView title = (TextView)viewHolder.itemView.findViewById(R.id.text_title);
            title.setText("index: " + i);

            final CardFrame cardFrame = (CardFrame)viewHolder.itemView.findViewById(R.id.card);

            cards.add(cardFrame);



            container.addView(viewHolder.itemView);
        }

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int[] pos = new int[2];

                cards.get(num - 1).getLocationOnScreen(pos);

                if (Math.sqrt((pos[1] - 200) * (pos[1] - 200)) < 100 && !finished) {

                    Log.d(TAG, "num: " + num + ", entered!!");

                    end = System.currentTimeMillis();
                    Log.d(Test, "end: " + end);

                    time = (end - start)/(1000.0);
                    Toast.makeText(MainWear.this, "" + time, Toast.LENGTH_LONG).show();
                    Log.d(Test, "end - start: " + time + "\n");

                    finished = true;

                }

//                        Log.d(TAG, "window location : " + pos[0] + ", " + pos[1]);

                return false;
            }
        });

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {  // move,210.0,220.0
        if (MESSAGE.equals(messageEvent.getPath())) {
            String msg = new String(messageEvent.getData());

            String[] data = msg.split(",");

            Log.d(TAG, data[0] + " " + data[1] + " " + data[2]);

            float x = Float.parseFloat(data[1]), y = Float.parseFloat(data[2]);
            long time = SystemClock.uptimeMillis();
            int dur = 1; // duration

            switch (data[0]) {
                case "down":
//                    Log.d(TAG, "down");
                    dispatchTouchEvent(
                            MotionEvent.obtain(time, time + dur, MotionEvent.ACTION_DOWN, x, y, 0));
                    break;

                case  "move":
//                    Log.d(TAG, "move");
                    dispatchTouchEvent(
                            MotionEvent.obtain(time, time + dur, MotionEvent.ACTION_MOVE, x, y, 0));
                    break;

                case  "up":
//                    Log.d(TAG, "up");
                    dispatchTouchEvent(
                            MotionEvent.obtain(time, time + dur, MotionEvent.ACTION_UP, x, y, 0));
                    break;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }
}
